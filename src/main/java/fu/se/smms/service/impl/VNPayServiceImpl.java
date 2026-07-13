package fu.se.smms.service.impl;

import fu.se.smms.config.VNPayConfig;
import fu.se.smms.dto.VNPayRequestDTO;
import fu.se.smms.dto.VNPayResponseDTO;
import fu.se.smms.entity.SalesOrder;
import fu.se.smms.entity.SalesOrderDetail;
import fu.se.smms.enums.PaymentStatus;
import fu.se.smms.repository.ProductRepository;
import fu.se.smms.repository.SalesOrderRepository;
import fu.se.smms.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Transactional
public class VNPayServiceImpl implements VNPayService {

    private static final Logger log = LoggerFactory.getLogger(VNPayServiceImpl.class);

    private final VNPayConfig vnPayConfig;
    private final SalesOrderRepository salesOrderRepository;
    private final ProductRepository productRepository;

    public VNPayServiceImpl(VNPayConfig vnPayConfig, SalesOrderRepository salesOrderRepository,
                            ProductRepository productRepository) {
        this.vnPayConfig = vnPayConfig;
        this.salesOrderRepository = salesOrderRepository;
        this.productRepository = productRepository;
    }

    @Override
    public VNPayResponseDTO createPaymentUrl(VNPayRequestDTO request, HttpServletRequest httpRequest) {
        try {
            // Append timestamp to ensure uniqueness — VNPay rejects duplicate vnp_TxnRef
            String vnp_TxnRef = request.getOrderId() + "_" + System.currentTimeMillis();
            String vnp_IpAddr = vnPayConfig.getIpAddress(httpRequest);
            
            // Amount in VND (multiply by 100 as per VNPay docs)
            long amount = request.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
            
            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", vnPayConfig.getVersion());
            vnp_Params.put("vnp_Command", vnPayConfig.getCommand());
            vnp_Params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
            vnp_Params.put("vnp_Amount", String.valueOf(amount));
            vnp_Params.put("vnp_CurrCode", "VND");
            
            if (request.getBankCode() != null && !request.getBankCode().isEmpty()) {
                vnp_Params.put("vnp_BankCode", request.getBankCode());
            }
            
            vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
            // Avoid special characters (e.g. #) in vnp_OrderInfo
            String orderInfo = request.getOrderInfo() != null && !request.getOrderInfo().isBlank()
                    ? request.getOrderInfo()
                    : "Thanh toan don hang " + request.getOrderId();
            vnp_Params.put("vnp_OrderInfo", orderInfo);
            vnp_Params.put("vnp_OrderType", vnPayConfig.getOrderType());
            
            String locale = request.getLanguage();
            vnp_Params.put("vnp_Locale", (locale != null && !locale.isEmpty()) ? locale : "vn");
            
            vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
            vnp_Params.put("vnp_IpAddr", vnp_IpAddr);
            
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            String vnp_CreateDate = formatter.format(calendar.getTime());
            vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
            
            calendar.add(Calendar.MINUTE, 15);
            String vnp_ExpireDate = formatter.format(calendar.getTime());
            vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);
            
            // Build query string and hash data (sorted, no trailing &)
            List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();
            
            for (String fieldName : fieldNames) {
                String fieldValue = vnp_Params.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    if (hashData.length() > 0) {
                        hashData.append('&');
                        query.append('&');
                    }
                    hashData.append(fieldName).append('=')
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8))
                            .append('=')
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                }
            }
            
            String queryUrl = query.toString();
            String vnp_SecureHash = vnPayConfig.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
            queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
            
            String paymentUrl = vnPayConfig.getPayUrl() + "?" + queryUrl;
            
            log.info("VNPay payment URL created for order: {}", vnp_TxnRef);
            
            return VNPayResponseDTO.success(paymentUrl);
            
        } catch (Exception e) {
            log.error("Error creating VNPay payment URL: {}", e.getMessage(), e);
            return VNPayResponseDTO.error("99", "Lỗi tạo URL thanh toán: " + e.getMessage());
        }
    }

    @Override
    public VNPayResponseDTO processPaymentReturn(Map<String, String> params) {
        try {
            String vnp_SecureHash = params.get("vnp_SecureHash");
            
            // Remove hash params for verification
            Map<String, String> fields = new HashMap<>(params);
            fields.remove("vnp_SecureHashType");
            fields.remove("vnp_SecureHash");
            
            String signValue = vnPayConfig.hashAllFields(fields);
            
            VNPayResponseDTO response = VNPayResponseDTO.builder()
                    .txnRef(params.get("vnp_TxnRef"))
                    .transactionNo(params.get("vnp_TransactionNo"))
                    .responseCode(params.get("vnp_ResponseCode"))
                    .transactionStatus(params.get("vnp_TransactionStatus"))
                    .bankCode(params.get("vnp_BankCode"))
                    .bankTranNo(params.get("vnp_BankTranNo"))
                    .payDate(params.get("vnp_PayDate"))
                    .orderInfo(params.get("vnp_OrderInfo"))
                    .build();
            
            String amountStr = params.get("vnp_Amount");
            if (amountStr != null) {
                response.setAmount(new BigDecimal(amountStr).divide(BigDecimal.valueOf(100)));
            }
            
            if (signValue.equals(vnp_SecureHash)) {
                String responseCode = params.get("vnp_ResponseCode");
                if ("00".equals(responseCode)) {
                    response.setCode("00");
                    response.setMessage("Giao dịch thành công");
                    
                    // Update order payment status
                    updateOrderPaymentStatus(params.get("vnp_TxnRef"), true, params.get("vnp_TransactionNo"));
                } else {
                    response.setCode(responseCode);
                    response.setMessage(getResponseMessage(responseCode));
                }
            } else {
                response.setCode("97");
                response.setMessage("Chữ ký không hợp lệ");
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("Error processing VNPay return: {}", e.getMessage(), e);
            return VNPayResponseDTO.error("99", "Lỗi xử lý kết quả thanh toán");
        }
    }

    @Override
    public Map<String, String> processIPN(Map<String, String> params) {
        Map<String, String> result = new HashMap<>();
        
        try {
            String vnp_SecureHash = params.get("vnp_SecureHash");
            
            Map<String, String> fields = new HashMap<>(params);
            fields.remove("vnp_SecureHashType");
            fields.remove("vnp_SecureHash");
            
            String signValue = vnPayConfig.hashAllFields(fields);
            
            if (!signValue.equals(vnp_SecureHash)) {
                result.put("RspCode", "97");
                result.put("Message", "Invalid Checksum");
                return result;
            }
            
            String txnRef = params.get("vnp_TxnRef");
            Integer orderId = extractOrderId(txnRef);
            String amountStr = params.get("vnp_Amount");
            BigDecimal vnpAmount = new BigDecimal(amountStr).divide(BigDecimal.valueOf(100));
            
            Optional<SalesOrder> orderOpt = salesOrderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                result.put("RspCode", "01");
                result.put("Message", "Order not found");
                return result;
            }
            
            SalesOrder order = orderOpt.get();
            
            if (order.getTotalAmount().compareTo(vnpAmount) != 0) {
                result.put("RspCode", "04");
                result.put("Message", "Invalid amount");
                return result;
            }
            
            // Already processed
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                result.put("RspCode", "02");
                result.put("Message", "Order already confirmed");
                return result;
            }
            
            String responseCode = params.get("vnp_ResponseCode");
            if ("00".equals(responseCode)) {
                confirmVnPayOrder(order, params.get("vnp_TransactionNo"));
            } else {
                order.setPaymentStatus(PaymentStatus.FAILED);
                salesOrderRepository.save(order);
                log.info("Order {} VNPay payment failed, responseCode={}", orderId, responseCode);
            }
            result.put("RspCode", "00");
            result.put("Message", "Confirm Success");
            
        } catch (Exception e) {
            log.error("Error processing VNPay IPN: {}", e.getMessage(), e);
            result.put("RspCode", "99");
            result.put("Message", "Unknown error");
        }
        
        return result;
    }

    /**
     * Extracts the order ID from vnp_TxnRef which has format "orderId_timestamp".
     */
    private Integer extractOrderId(String txnRef) {
        if (txnRef == null) throw new IllegalArgumentException("vnp_TxnRef is null");
        String idPart = txnRef.contains("_") ? txnRef.split("_")[0] : txnRef;
        return Integer.parseInt(idPart);
    }

    /**
     * Confirms a VNPay order: deducts stock and marks it as PAID.
     */
    private void confirmVnPayOrder(SalesOrder order, String transactionNo) {
        order.setVnpayTransactionNo(transactionNo);
        order.setPaymentStatus(PaymentStatus.PAID);
        salesOrderRepository.save(order);

        for (SalesOrderDetail detail : order.getSalesOrderDetails()) {
            int updated = productRepository.decreaseStockLevel(
                    detail.getProduct().getProductId(), detail.getQuantity());
            if (updated == 0) {
                log.warn("Stock deduction failed for product {} (qty {}), restoring order to PENDING",
                        detail.getProduct().getProductId(), detail.getQuantity());
            }
        }
        log.info("Order {} payment confirmed via VNPay, transaction: {}", order.getSalesOrderId(), transactionNo);
    }

    private void updateOrderPaymentStatus(String txnRef, boolean success, String transactionNo) {
        try {
            Integer orderId = extractOrderId(txnRef);
            Optional<SalesOrder> orderOpt = salesOrderRepository.findById(orderId);
            if (orderOpt.isPresent()) {
                SalesOrder order = orderOpt.get();
                if (success) {
                    confirmVnPayOrder(order, transactionNo);
                } else {
                    order.setPaymentStatus(PaymentStatus.FAILED);
                    salesOrderRepository.save(order);
                }
            }
        } catch (Exception e) {
            log.error("Error updating order payment status: {}", e.getMessage(), e);
        }
    }

    private String getResponseMessage(String responseCode) {
        return switch (responseCode) {
            case "00" -> "Giao dịch thành công";
            case "07" -> "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường)";
            case "09" -> "Thẻ/Tài khoản chưa đăng ký dịch vụ InternetBanking";
            case "10" -> "Xác thực thông tin thẻ/tài khoản không đúng quá 3 lần";
            case "11" -> "Đã hết hạn chờ thanh toán";
            case "12" -> "Thẻ/Tài khoản bị khóa";
            case "13" -> "Nhập sai mật khẩu xác thực giao dịch (OTP)";
            case "24" -> "Khách hàng hủy giao dịch";
            case "51" -> "Tài khoản không đủ số dư";
            case "65" -> "Tài khoản đã vượt quá hạn mức giao dịch trong ngày";
            case "75" -> "Ngân hàng thanh toán đang bảo trì";
            case "79" -> "Nhập sai mật khẩu thanh toán quá số lần quy định";
            default -> "Lỗi không xác định";
        };
    }
}
