package fu.se.smms.service.impl;

import fu.se.smms.dto.BarcodePrintRequest;
import fu.se.smms.entity.Product;
import fu.se.smms.exception.BadRequestException;
import fu.se.smms.exception.ResourceNotFoundException;
import fu.se.smms.repository.ProductRepository;
import fu.se.smms.service.BarcodeService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.krysalis.barcode4j.impl.code128.Code128Bean;
import org.krysalis.barcode4j.output.bitmap.BitmapCanvasProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BarcodeServiceImpl implements BarcodeService {
    private static final Logger log = LoggerFactory.getLogger(BarcodeServiceImpl.class);

    private static final float LABEL_WIDTH = 180f;   // ~63mm
    private static final float LABEL_HEIGHT = 90f;   // ~32mm
    private static final float MARGIN = 10f;
    private static final float PAGE_MARGIN = 20f;
    private static final int MAX_LABELS = 500;

    private final ProductRepository productRepository;

    public BarcodeServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public BufferedImage generateBarcodeImage(String barcodeText, int dpi) {
        try {
            Code128Bean bean = new Code128Bean();
            bean.setModuleWidth(0.4);
            bean.setHeight(12.0);
            bean.doQuietZone(true);
            bean.setFontSize(3.0);

            BitmapCanvasProvider provider = new BitmapCanvasProvider(
                    dpi, BufferedImage.TYPE_BYTE_GRAY, true, 0);
            bean.generateBarcode(provider, barcodeText);
            provider.finish();

            return provider.getBufferedImage();
        } catch (Exception e) {
            log.error("Failed to generate barcode for: {}", barcodeText, e);
            throw new BadRequestException("Không thể tạo mã vạch: " + barcodeText);
        }
    }

    @Override
    public byte[] generateLabelsPdf(BarcodePrintRequest request) {
        int totalLabels = request.getItems().stream()
                .mapToInt(BarcodePrintRequest.PrintItem::getQuantity)
                .sum();
        if (totalLabels > MAX_LABELS) {
            throw new BadRequestException("Tối đa " + MAX_LABELS + " nhãn mỗi lần in. Yêu cầu: " + totalLabels);
        }

        List<Integer> productIds = request.getItems().stream()
                .map(BarcodePrintRequest.PrintItem::getProductId)
                .toList();
        List<Product> products = productRepository.findAllById(productIds);

        if (products.size() != productIds.size()) {
            throw new ResourceNotFoundException("Một số sản phẩm không tồn tại");
        }

        List<String> invalidSkuProducts = products.stream()
                .filter(p -> p.getBarcode() == null || p.getBarcode().isBlank())
                .map(p -> p.getName() + " (ID: " + p.getProductId() + ")")
                .toList();
        if (!invalidSkuProducts.isEmpty()) {
            throw new BadRequestException(
                    "Sản phẩm chưa có mã SKU hợp lệ: " + String.join(", ", invalidSkuProducts));
        }

        Map<Integer, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getProductId, Function.identity()));

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            int labelsPerRow = (int) ((PDRectangle.A4.getWidth() - 2 * PAGE_MARGIN) / LABEL_WIDTH);
            int labelsPerCol = (int) ((PDRectangle.A4.getHeight() - 2 * PAGE_MARGIN) / LABEL_HEIGHT);
            int labelsPerPage = labelsPerRow * labelsPerCol;
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            Map<Integer, PDImageXObject> barcodeImageMap = new HashMap<>();
            for (Product product : products) {
                BufferedImage barcodeImg = generateBarcodeImage(product.getBarcode(), 150);
                barcodeImageMap.put(product.getProductId(), LosslessFactory.createFromImage(doc, barcodeImg));
            }

            int labelIndex = 0;
            PDPageContentStream cs = null;

            for (BarcodePrintRequest.PrintItem item : request.getItems()) {
                Product product = productMap.get(item.getProductId());
                PDImageXObject pdImage = barcodeImageMap.get(item.getProductId());

                for (int q = 0; q < item.getQuantity(); q++) {
                    if (labelIndex % labelsPerPage == 0) {
                        if (cs != null) cs.close();
                        PDPage page = new PDPage(PDRectangle.A4);
                        doc.addPage(page);
                        cs = new PDPageContentStream(doc, page);
                    }

                    int posOnPage = labelIndex % labelsPerPage;
                    int col = posOnPage % labelsPerRow;
                    int row = posOnPage / labelsPerRow;

                    float x = PAGE_MARGIN + col * LABEL_WIDTH;
                    float y = PDRectangle.A4.getHeight() - PAGE_MARGIN - (row + 1) * LABEL_HEIGHT;

                    drawLabel(cs, fontBold, pdImage, product, x, y);
                    labelIndex++;
                }
            }

            if (cs != null) cs.close();

            doc.save(out);
            log.info("Generated PDF with {} barcode labels", labelIndex);
            return out.toByteArray();
        } catch (BadRequestException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate barcode PDF", e);
            throw new BadRequestException("Không thể tạo file PDF barcode");
        }
    }

    private void drawLabel(PDPageContentStream cs, PDType1Font fontBold,
                           PDImageXObject pdImage, Product product,
                           float x, float y) throws IOException {
        float innerX = x + MARGIN;
        float innerWidth = LABEL_WIDTH - 2 * MARGIN;
        float currentY = y + LABEL_HEIGHT - MARGIN;

        String productName = truncate(product.getName());
        cs.beginText();
        cs.setFont(fontBold, 7);
        cs.newLineAtOffset(innerX, currentY - 7);
        cs.showText(productName);
        cs.endText();
        currentY -= 12;

        float barcodeDisplayWidth = innerWidth;
        float barcodeDisplayHeight = barcodeDisplayWidth * pdImage.getHeight() / pdImage.getWidth();
        if (barcodeDisplayHeight > 35) {
            barcodeDisplayHeight = 35;
            barcodeDisplayWidth = barcodeDisplayHeight * pdImage.getWidth() / pdImage.getHeight();
        }
        float barcodeX = innerX + (innerWidth - barcodeDisplayWidth) / 2;
        cs.drawImage(pdImage, barcodeX, currentY - barcodeDisplayHeight, barcodeDisplayWidth, barcodeDisplayHeight);
        currentY -= barcodeDisplayHeight + 3;

        BigDecimal price = product.getSellingPrice();
        String priceText = price != null ? formatPrice(price) : "";
        cs.beginText();
        cs.setFont(fontBold, 8);
        float priceWidth = fontBold.getStringWidth(priceText) / 1000 * 8;
        cs.newLineAtOffset(innerX + (innerWidth - priceWidth) / 2, currentY - 8);
        cs.showText(priceText);
        cs.endText();

        cs.setLineWidth(0.3f);
        cs.addRect(x + 1, y + 1, LABEL_WIDTH - 2, LABEL_HEIGHT - 2);
        cs.stroke();
    }

    private String truncate(String text) {
        if (text == null) return "";
        return text.length() > 25 ? text.substring(0, 25 - 2) + ".." : text;
    }

    private String formatPrice(BigDecimal price) {
        long value = price.longValue();
        StringBuilder sb = new StringBuilder(Long.toString(value));
        for (int i = sb.length() - 3; i > 0; i -= 3) {
            sb.insert(i, '.');
        }
        return sb + " VND";
    }
}
