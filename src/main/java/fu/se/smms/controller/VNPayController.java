package fu.se.smms.controller;

import fu.se.smms.dto.VNPayRequestDTO;
import fu.se.smms.dto.VNPayResponseDTO;
import fu.se.smms.service.VNPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment/vnpay")
@Tag(name = "VNPay Payment", description = "VNPay payment integration APIs")
public class VNPayController {

    private final VNPayService vnPayService;

    public VNPayController(VNPayService vnPayService) {
        this.vnPayService = vnPayService;
    }

    @PostMapping("/create-payment")
    @Operation(summary = "Create VNPay payment URL", description = "Generate payment URL to redirect user to VNPay")
    public ResponseEntity<VNPayResponseDTO> createPayment(
            @RequestBody VNPayRequestDTO request,
            HttpServletRequest httpRequest) {
        VNPayResponseDTO response = vnPayService.createPaymentUrl(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/payment-return")
    @Operation(summary = "Handle VNPay return", description = "Process payment result from VNPay redirect")
    public ResponseEntity<VNPayResponseDTO> paymentReturn(@RequestParam Map<String, String> params) {
        VNPayResponseDTO response = vnPayService.processPaymentReturn(params);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ipn")
    @Operation(summary = "VNPay IPN handler", description = "Instant Payment Notification from VNPay")
    public ResponseEntity<Map<String, String>> ipnHandler(@RequestParam Map<String, String> params) {
        Map<String, String> result = vnPayService.processIPN(params);
        return ResponseEntity.ok(result);
    }
}
