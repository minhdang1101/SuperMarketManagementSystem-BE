package fu.se.smms.service;

import fu.se.smms.dto.BarcodePrintRequest;

import java.awt.image.BufferedImage;

public interface BarcodeService {

    BufferedImage generateBarcodeImage(String barcodeText, int dpi);

    byte[] generateLabelsPdf(BarcodePrintRequest request);
}
