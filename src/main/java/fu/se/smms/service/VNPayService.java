package fu.se.smms.service;

import fu.se.smms.dto.VNPayRequestDTO;
import fu.se.smms.dto.VNPayResponseDTO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface VNPayService {
    VNPayResponseDTO createPaymentUrl(VNPayRequestDTO request, HttpServletRequest httpRequest);
    VNPayResponseDTO processPaymentReturn(Map<String, String> params);
    Map<String, String> processIPN(Map<String, String> params);
}
