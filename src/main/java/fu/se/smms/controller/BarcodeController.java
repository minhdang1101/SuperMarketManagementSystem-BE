package fu.se.smms.controller;

import fu.se.smms.dto.BarcodePrintRequest;
import fu.se.smms.service.BarcodeService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@RestController
@Validated
@RequestMapping("/api/v1/barcodes")
public class BarcodeController {
    private static final Logger log = LoggerFactory.getLogger(BarcodeController.class);
    private final BarcodeService barcodeService;

    public BarcodeController(BarcodeService barcodeService) {
        this.barcodeService = barcodeService;
    }

    @GetMapping(value = "/image/{barcode}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getBarcodeImage(@PathVariable String barcode) throws IOException {
        log.debug("Generate barcode image for: {}", barcode);

        BufferedImage image = barcodeService.generateBarcodeImage(barcode, 200);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(out.toByteArray());
    }

    @PostMapping(value = "/labels/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generateLabelsPdf(@Valid @RequestBody BarcodePrintRequest request) {
        int totalLabels = request.getItems().stream()
                .mapToInt(BarcodePrintRequest.PrintItem::getQuantity)
                .sum();
        log.info("Generate barcode labels PDF: {} products, {} labels total",
                request.getItems().size(), totalLabels);

        byte[] pdfBytes = barcodeService.generateLabelsPdf(request);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=barcode-labels.pdf")
                .body(pdfBytes);
    }
}
