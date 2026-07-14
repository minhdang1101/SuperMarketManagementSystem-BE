package fu.se.smms.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.se.smms.dto.AiChatMessageDTO;
import fu.se.smms.dto.AiChatRequestDTO;
import fu.se.smms.dto.AiChatResponseDTO;
import fu.se.smms.dto.ShiftDTO;
import fu.se.smms.dto.StaffDTO;
import fu.se.smms.dto.UserDetailDTO;
import fu.se.smms.entity.Product;
import fu.se.smms.entity.Supplier;
import fu.se.smms.enums.OrderStatus;
import fu.se.smms.enums.ShiftType;
import fu.se.smms.repository.ProductRepository;
import fu.se.smms.repository.PurchaseOrderRepository;
import fu.se.smms.repository.SupplierRepository;
import fu.se.smms.service.AiChatService;
import fu.se.smms.service.ShiftService;
import fu.se.smms.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AiChatServiceImpl implements AiChatService {
    private static final Logger log = LoggerFactory.getLogger(AiChatServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final UserService userService;
    private final ShiftService shiftService;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.provider:openai}")
    private String provider;

    @Value("${ai.api-url:}")
    private String apiUrl;

    @Value("${ai.model:gpt-4o-mini}")
    private String model;

    @Value("${ai.timeout-seconds:30}")
    private int timeoutSeconds;

    public AiChatServiceImpl(UserService userService,
                             ShiftService shiftService,
                             ProductRepository productRepository,
                             SupplierRepository supplierRepository,
                             PurchaseOrderRepository purchaseOrderRepository) {
        this.userService = userService;
        this.shiftService = shiftService;
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public AiChatResponseDTO chat(AiChatRequestDTO request, UserDetailDTO principal) {
        String role = principal != null && principal.getUser() != null ? principal.getUser().getRole() : "UNKNOWN";
        String username = principal != null ? principal.getUsername() : "unknown";

        if (isShiftManagementIntent(request.getMessage())) {
            return AiChatResponseDTO.builder()
                    .answer(buildShiftSuggestionAnswer(request.getMessage(), role))
                    .suggestions(buildSuggestions(role))
                    .fallback(false)
                    .model("shift-suggestion")
                    .build();
        }

        String context = buildBusinessContext(role, username, request.getPagePath());

        if (isAiProviderConfigured()) {
            try {
                String answer = callAiProvider(request, context, role);
                if (answer != null && !answer.isBlank()) {
                    return AiChatResponseDTO.builder()
                            .answer(answer.trim())
                            .suggestions(buildSuggestions(role))
                            .fallback(false)
                            .model(model)
                            .build();
                }
            } catch (Exception ex) {
                log.warn("AI provider call failed, using local fallback: {}", ex.getMessage());
            }
        }

        return AiChatResponseDTO.builder()
                .answer(buildLocalFallbackAnswer(request.getMessage(), context, role))
                .suggestions(buildSuggestions(role))
                .fallback(true)
                .model("local-fallback")
                .build();
    }

    private boolean isShiftManagementIntent(String message) {
        String normalized = normalize(message);
        boolean mentionsShift = normalized.contains(" ca")
                || normalized.contains("ca lam")
                || normalized.contains("lich ca")
                || normalized.contains("sap lich")
                || normalized.contains("xep lich")
                || normalized.contains("phan ca");
        boolean mentionsStaff = normalized.contains("nhan vien")
                || normalized.contains("thanh vien")
                || normalized.contains("moi nguoi")
                || normalized.contains("tat ca")
                || normalized.contains("ai ");
        return mentionsShift && mentionsStaff;
    }

    private String buildShiftSuggestionAnswer(String message, String role) {
        if (!"ADMIN".equals(role) && !"MANAGER".equals(role)) {
            return "Bạn có thể xem lịch ca của mình, nhưng việc sắp lịch/gợi ý quản lý ca chỉ dành cho ADMIN hoặc MANAGER.";
        }

        String normalized = normalize(message);
        List<StaffDTO> activeStaff = safeList(userService.getAllStaff()).stream()
                .filter(staff -> staff.getStatus() == null || !"Inactive".equalsIgnoreCase(staff.getStatus()))
                .sorted(Comparator.comparing(StaffDTO::getId, Comparator.nullsLast(String::compareTo)))
                .toList();

        if (activeStaff.isEmpty()) {
            return "Chưa có nhân viên đang hoạt động để gợi ý lịch ca.";
        }

        LocalDate[] bounds = resolveWeekBounds(message);
        LocalDate start = bounds[0];
        LocalDate end = bounds[1];
        List<ShiftDTO> weekShifts = safeList(shiftService.getShiftsBetweenDates(start, end));
        Map<String, Long> shiftCountByStaff = buildShiftCountByStaff(activeStaff, weekShifts);
        List<Map.Entry<String, Long>> sortedCounts = shiftCountByStaff.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().thenComparing(Map.Entry::getKey))
                .toList();
        List<String> unassignedStaff = sortedCounts.stream()
                .filter(entry -> entry.getValue() == 0)
                .map(Map.Entry::getKey)
                .toList();

        StringBuilder answer = new StringBuilder();
        answer.append("Mình chỉ tạo gợi ý lịch ca, chưa ghi vào database.\n");
        answer.append("Khoảng thời gian phân tích: ")
                .append(start.format(DATE_FORMAT))
                .append(" - ")
                .append(end.format(DATE_FORMAT))
                .append(".\n\n");

        if (normalized.contains("it ca") || normalized.contains("it nhat") || normalized.contains("ai dang co it")) {
            long min = sortedCounts.isEmpty() ? 0 : sortedCounts.get(0).getValue();
            answer.append("Nhân viên đang có ít ca nhất:\n");
            sortedCounts.stream()
                    .filter(entry -> entry.getValue() == min)
                    .forEach(entry -> answer.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append(" ca\n"));
            answer.append("\n");
        }

        if (normalized.contains("chua duoc phan") || normalized.contains("chua phan") || normalized.contains("chua co ca")) {
            if (unassignedStaff.isEmpty()) {
                answer.append("Tất cả nhân viên đang hoạt động đều đã có ít nhất 1 ca trong khoảng này.\n\n");
            } else {
                answer.append("Nhân viên chưa được phân ca:\n");
                unassignedStaff.forEach(staff -> answer.append("- ").append(staff).append("\n"));
                answer.append("\n");
            }
        }

        if (normalized.contains("nhieu ca") || normalized.contains("nhieu nhat") || normalized.contains("lam nhieu")) {
            long max = sortedCounts.isEmpty() ? 0 : sortedCounts.get(sortedCounts.size() - 1).getValue();
            answer.append("Nhân viên đang có nhiều ca nhất:\n");
            sortedCounts.stream()
                    .filter(entry -> entry.getValue() == max)
                    .forEach(entry -> answer.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append(" ca\n"));
            answer.append("\n");
        }

        boolean wantsBalanceReview = normalized.contains("chua deu")
                || normalized.contains("khong deu")
                || normalized.contains("lech")
                || normalized.contains("can bang")
                || normalized.contains("kiem tra")
                || normalized.contains("phan tich");

        if (wantsBalanceReview) {
            answer.append(buildBalanceReview(sortedCounts)).append("\n\n");
        }

        boolean wantsProposal = normalized.contains("sap lich")
                || normalized.contains("xep lich")
                || normalized.contains("chia lich")
                || normalized.contains("tao goi y")
                || normalized.contains("goi y lich")
                || normalized.contains("them ca")
                || normalized.contains("nen phan")
                || normalized.contains("nen cho");

        if (wantsProposal) {
            int requestedStaffCount = resolveRequestedStaffCount(normalized, activeStaff.size());
            List<StaffDTO> selectedStaff = activeStaff.stream()
                    .sorted(Comparator.comparingLong(staff -> shiftCountByStaff.getOrDefault(staffLabel(staff), 0L)))
                    .limit(requestedStaffCount)
                    .toList();
            answer.append("Gợi ý lịch ca để cân bằng tải:\n");
            answer.append(buildScheduleProposal(start, end, selectedStaff));
            answer.append("\n");
        }

        answer.append("Tổng quan hiện tại:\n");
        sortedCounts.forEach(entry -> answer.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append(" ca\n"));
        answer.append("\nĐể áp dụng vào DB, bước sau nên làm nút 'Áp dụng lịch' để người quản lý xác nhận trước khi tạo ca.");
        return answer.toString().trim();
    }

    private Map<String, Long> buildShiftCountByStaff(List<StaffDTO> staffList, List<ShiftDTO> shifts) {
        Map<String, Long> counts = new LinkedHashMap<>();
        staffList.forEach(staff -> counts.put(staffLabel(staff), 0L));
        for (ShiftDTO shift : shifts) {
            String key = shift.getStaffId() + " - " + shift.getStaffName();
            if (counts.containsKey(key)) {
                counts.put(key, counts.get(key) + 1);
            }
        }
        return counts;
    }

    private String buildBalanceReview(List<Map.Entry<String, Long>> sortedCounts) {
        if (sortedCounts.isEmpty()) {
            return "Chưa có dữ liệu nhân viên để đánh giá độ đều của lịch ca.";
        }

        long min = sortedCounts.get(0).getValue();
        long max = sortedCounts.get(sortedCounts.size() - 1).getValue();
        long gap = max - min;
        String leastAssigned = sortedCounts.stream()
                .filter(entry -> entry.getValue() == min)
                .map(Map.Entry::getKey)
                .collect(Collectors.joining(", "));
        String mostAssigned = sortedCounts.stream()
                .filter(entry -> entry.getValue() == max)
                .map(Map.Entry::getKey)
                .collect(Collectors.joining(", "));

        StringBuilder review = new StringBuilder();
        review.append("Đánh giá độ đều lịch ca:\n");
        review.append("- Ít ca nhất: ").append(leastAssigned).append(" (").append(min).append(" ca)\n");
        review.append("- Nhiều ca nhất: ").append(mostAssigned).append(" (").append(max).append(" ca)\n");
        review.append("- Mức chênh lệch: ").append(gap).append(" ca\n");
        if (gap <= 1) {
            review.append("=> Lịch ca hiện tại khá đều. Nếu cần thêm ca mới, nên ưu tiên người đang có ít ca hơn.");
        } else {
            review.append("=> Lịch ca hiện tại chưa đều. Nên ưu tiên phân thêm ca cho nhóm ít ca và hạn chế thêm ca cho nhóm nhiều ca.");
        }
        return review.toString();
    }

    private String buildScheduleProposal(LocalDate start, LocalDate end, List<StaffDTO> staffList) {
        if (staffList.isEmpty()) {
            return "- Không có nhân viên phù hợp để gợi ý.";
        }

        ShiftType[] shifts = {ShiftType.MORNING, ShiftType.AFTERNOON, ShiftType.NIGHT};
        StringBuilder proposal = new StringBuilder();
        int dayIndex = 0;
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            proposal.append(date.format(DATE_FORMAT)).append(":\n");
            for (int slot = 0; slot < Math.min(3, staffList.size()); slot++) {
                StaffDTO staff = staffList.get((dayIndex + slot) % staffList.size());
                ShiftType shiftType = shifts[slot % shifts.length];
                proposal.append("- ")
                        .append(displayShiftType(shiftType))
                        .append(": ")
                        .append(staffLabel(staff))
                        .append("\n");
            }
            dayIndex++;
        }
        return proposal.toString().trim();
    }

    private int resolveRequestedStaffCount(String normalizedMessage, int staffCount) {
        if (normalizedMessage.contains("tat ca") || normalizedMessage.contains("tung") || normalizedMessage.contains("moi nguoi")) {
            return staffCount;
        }

        Matcher matcher = Pattern.compile("(\\d+)\\s*(nhan vien|thanh vien|nguoi)").matcher(normalizedMessage);
        if (matcher.find()) {
            return Math.max(1, Math.min(Integer.parseInt(matcher.group(1)), staffCount));
        }
        return Math.min(3, staffCount);
    }

    private LocalDate[] resolveWeekBounds(String message) {
        LocalDate referenceDate = extractDate(message);
        String normalized = normalize(message);
        LocalDate today = LocalDate.now();

        if (referenceDate == null) {
            referenceDate = normalized.contains("tuan sau") ? today.plusWeeks(1) : today;
        }

        LocalDate monday = referenceDate.minusDays(referenceDate.getDayOfWeek().getValue() - 1L);
        LocalDate sunday = monday.plusDays(6);
        return new LocalDate[]{monday, sunday};
    }

    private LocalDate extractDate(String message) {
        String text = message == null ? "" : message;
        Matcher slashDate = Pattern.compile("(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})").matcher(text);
        if (slashDate.find()) {
            int day = Integer.parseInt(slashDate.group(1));
            int month = Integer.parseInt(slashDate.group(2));
            int year = Integer.parseInt(slashDate.group(3));
            return LocalDate.of(year, month, day);
        }

        Matcher isoDate = Pattern.compile("(\\d{4})-(\\d{1,2})-(\\d{1,2})").matcher(text);
        if (isoDate.find()) {
            try {
                return LocalDate.parse(isoDate.group(0));
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }

        String normalized = normalize(text);
        if (normalized.contains("ngay mai")) return LocalDate.now().plusDays(1);
        if (normalized.contains("hom nay")) return LocalDate.now();
        return null;
    }

    private String staffLabel(StaffDTO staff) {
        return staff.getId() + " - " + staff.getName();
    }

    private String displayShiftType(ShiftType type) {
        return switch (type) {
            case MORNING -> "Ca sáng (06:00 - 14:00)";
            case AFTERNOON -> "Ca chiều (14:00 - 22:00)";
            case NIGHT -> "Ca đêm (22:00 - 06:00)";
        };
    }

    private String normalize(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
    }

    private boolean isAiProviderConfigured() {
        return apiKey != null && !apiKey.isBlank()
                && apiUrl != null && !apiUrl.isBlank()
                && model != null && !model.isBlank();
    }

    private String callAiProvider(AiChatRequestDTO request, String context, String role) throws Exception {
        if ("gemini".equalsIgnoreCase(provider) || "google".equalsIgnoreCase(provider)) {
            return callGeminiProvider(request, context, role);
        }
        return callOpenAiProvider(request, context, role);
    }

    private String callOpenAiProvider(AiChatRequestDTO request, String context, String role) throws Exception {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "system",
                "content", buildSystemPrompt(role, context)
        ));

        if (request.getHistory() != null) {
            request.getHistory().stream()
                    .filter(Objects::nonNull)
                    .filter(m -> m.getContent() != null && !m.getContent().isBlank())
                    .limit(8)
                    .forEach(m -> messages.add(Map.of(
                            "role", normalizeRole(m.getRole()),
                            "content", trimTo(m.getContent(), 1200)
                    )));
        }

        messages.add(Map.of("role", "user", "content", trimTo(request.getMessage(), 2000)));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.2);
        body.put("max_tokens", 900);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(Math.max(5, timeoutSeconds)))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("AI provider returned HTTP " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.asText().isBlank()) {
            content = root.path("output_text");
        }
        return content.asText("");
    }

    private String callGeminiProvider(AiChatRequestDTO request, String context, String role) throws Exception {
        List<Map<String, Object>> contents = new ArrayList<>();

        if (request.getHistory() != null) {
            request.getHistory().stream()
                    .filter(Objects::nonNull)
                    .filter(m -> m.getContent() != null && !m.getContent().isBlank())
                    .limit(8)
                    .forEach(m -> contents.add(Map.of(
                            "role", "assistant".equals(m.getRole()) ? "model" : "user",
                            "parts", List.of(Map.of("text", trimTo(m.getContent(), 1200)))
                    )));
        }

        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", trimTo(request.getMessage(), 2000)))
        ));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", Map.of(
                "parts", List.of(Map.of("text", buildSystemPrompt(role, context)))
        ));
        body.put("contents", contents);
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature", 0.2);
        generationConfig.put("maxOutputTokens", 900);
        if (normalizeGeminiModel(model).contains("2.5")) {
            generationConfig.put("thinkingConfig", Map.of("thinkingBudget", 0));
        }
        body.put("generationConfig", generationConfig);

        String baseUrl = normalizeGeminiBaseUrl(apiUrl);
        String requestUrl = "%s/models/%s:generateContent?key=%s".formatted(baseUrl, normalizeGeminiModel(model), apiKey);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(requestUrl))
                .timeout(Duration.ofSeconds(Math.max(5, timeoutSeconds)))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Gemini provider returned HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
        if (parts.isArray()) {
            for (JsonNode part : parts) {
                String text = part.path("text").asText("");
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return "";
    }

    private String normalizeGeminiBaseUrl(String value) {
        String base = value == null || value.isBlank()
                ? "https://generativelanguage.googleapis.com/v1beta"
                : value.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

    private String normalizeGeminiModel(String value) {
        String selectedModel = value == null || value.isBlank() ? "gemini-2.5-flash" : value.trim();
        return selectedModel.startsWith("models/") ? selectedModel.substring("models/".length()) : selectedModel;
    }

    private String buildSystemPrompt(String role, String context) {
        return """
                Bạn là AI Copilot cho hệ thống SuperMarket Management System.
                Trả lời bằng tiếng Việt có dấu, ngắn gọn, đúng nghiệp vụ siêu thị.
                Riêng module lịch ca: chỉ đưa ra gợi ý, không tự tạo/sửa/xóa ca trong database.
                Nếu câu hỏi cần thao tác ghi dữ liệu, hãy nói cần có nút xác nhận như 'Áp dụng lịch'.
                Tôn trọng role hiện tại: %s. Không gợi ý truy cập chức năng vượt quyền.
                Nếu dữ liệu context không đủ để kết luận, nói rõ cần kiểm tra thêm.

                Context hiện tại:
                %s
                """.formatted(role, context);
    }

    private String buildBusinessContext(String role, String username, String pagePath) {
        StringBuilder context = new StringBuilder();
        context.append("Người dùng: ").append(username).append(" | Role: ").append(role).append('\n');
        context.append("Trang hiện tại: ").append(pagePath == null || pagePath.isBlank() ? "không rõ" : pagePath).append('\n');
        context.append("Route chính: /dashboard, /checkout, /staff, /shifts, /shift-management, /categories, /products, /suppliers, /purchase-orders, /goods-receiving, /promotions, /sales-history, /inventory, /inventory-adjustment, /barcode-print, /settings, /profile.\n");

        List<StaffDTO> staff = safeList(userService.getAllStaff());
        context.append("Nhân viên: ").append(staff.size()).append(" người. ");
        context.append(staff.stream()
                .limit(8)
                .map(s -> "%s-%s(%s)".formatted(s.getId(), s.getName(), s.getRole()))
                .collect(Collectors.joining(", ")))
                .append('\n');

        LocalDate today = LocalDate.now();
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1L);
        LocalDate sunday = monday.plusDays(6);
        List<ShiftDTO> weekShifts = safeList(shiftService.getShiftsBetweenDates(monday, sunday));
        Map<String, Long> shiftsByStaff = weekShifts.stream()
                .collect(Collectors.groupingBy(s -> s.getStaffId() + "-" + s.getStaffName(), LinkedHashMap::new, Collectors.counting()));
        context.append("Lịch ca từ ").append(monday).append(" đến ").append(sunday)
                .append(": ").append(weekShifts.size()).append(" ca. ");
        context.append(shiftsByStaff.entrySet().stream()
                .limit(8)
                .map(e -> e.getKey() + ": " + e.getValue() + " ca")
                .collect(Collectors.joining(", ")))
                .append('\n');

        List<Product> products = productRepository.findAll(PageRequest.of(0, 100)).getContent();
        List<Product> lowStock = products.stream()
                .filter(p -> Boolean.TRUE.equals(p.getStatus()))
                .filter(p -> p.getMinStockLevel() != null && p.getStockLevel() != null && p.getStockLevel() < p.getMinStockLevel())
                .limit(10)
                .toList();
        context.append("Sản phẩm đọc mẫu: ").append(products.size()).append(" sản phẩm đầu tiên; tồn thấp: ")
                .append(lowStock.size()).append(". ");
        context.append(lowStock.stream()
                .map(p -> "%s tồn %d/min %d".formatted(p.getName(), valueOrZero(p.getStockLevel()), valueOrZero(p.getMinStockLevel())))
                .collect(Collectors.joining(", ")))
                .append('\n');

        List<Supplier> suppliers = supplierRepository.findAll(PageRequest.of(0, 50)).getContent();
        long activeSupplierCount = suppliers.stream().filter(s -> Boolean.TRUE.equals(s.getStatus())).count();
        context.append("Nhà cung cấp đọc mẫu: ").append(suppliers.size())
                .append("; đang hoạt động: ").append(activeSupplierCount).append(". ");
        context.append(suppliers.stream()
                .limit(8)
                .map(s -> "%s(%s)".formatted(s.getName(), Boolean.TRUE.equals(s.getStatus()) ? "active" : "inactive"))
                .collect(Collectors.joining(", ")))
                .append('\n');

        context.append("Đơn nhập hàng theo trạng thái: ");
        context.append(List.of(OrderStatus.values()).stream()
                .map(status -> status.name() + "=" + safeCountPurchaseOrders(status))
                .collect(Collectors.joining(", ")))
                .append('\n');

        context.append("Lưu ý: AI chỉ đọc tóm tắt dữ liệu, không thay đổi database.");
        return context.toString();
    }

    private String buildLocalFallbackAnswer(String message, String context, String role) {
        String normalized = normalize(message);
        String prefix = "Mình đang dùng câu trả lời nội bộ vì AI provider chưa cấu hình đúng hoặc đang lỗi. ";

        if (normalized.contains("ca") || normalized.contains("lich") || normalized.contains("nhan vien")) {
            return prefix + "Với phần nhân sự/lịch ca, bạn nên kiểm tra các trang `/staff`, `/shifts` và `/shift-management`. Dữ liệu tóm tắt hiện tại:\n\n" + compactContext(context, "Lịch ca", "Sản phẩm");
        }
        if (normalized.contains("ton") || normalized.contains("san pham") || normalized.contains("hang")) {
            return prefix + "Với hàng hóa, hãy ưu tiên kiểm tra sản phẩm tồn thấp và tạo đơn nhập nếu cần. Dữ liệu tóm tắt hiện tại:\n\n" + compactContext(context, "Sản phẩm", "Nhà cung cấp");
        }
        if (normalized.contains("nhap") || normalized.contains("supplier") || normalized.contains("cung cap")) {
            return prefix + "Với nhập hàng, luồng chính là `/suppliers` -> `/purchase-orders` -> `/goods-receiving`. Dữ liệu tóm tắt hiện tại:\n\n" + compactContext(context, "Nhà cung cấp", "Đơn nhập");
        }
        if ("ADMIN".equals(role) && (normalized.contains("cai dat") || normalized.contains("setting"))) {
            return prefix + "Trang `/settings` chỉ dành cho ADMIN, dùng để chỉnh thông tin cửa hàng, VAT, tiền tệ và ngưỡng cảnh báo tồn kho thấp.";
        }
        return prefix + "Bạn có thể hỏi mình về nhân viên, lịch ca, sản phẩm tồn thấp, nhà cung cấp, đơn nhập hàng hoặc cách dùng từng màn hình.";
    }

    private String compactContext(String context, String startMarker, String nextMarker) {
        int start = context.indexOf(startMarker);
        if (start < 0) return context;
        int end = nextMarker == null ? -1 : context.indexOf(nextMarker, start + startMarker.length());
        return end > start ? context.substring(start, end).trim() : context.substring(start).trim();
    }

    private List<String> buildSuggestions(String role) {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("Sắp lịch cho 3 nhân viên tuần này");
        suggestions.add("Ai đang có ít ca nhất?");
        suggestions.add("Nhân viên nào chưa được phân ca?");
        suggestions.add("Tạo gợi ý lịch làm cho tuần sau");
        if ("ADMIN".equals(role)) {
            suggestions.add("Trang cài đặt hệ thống dùng để làm gì?");
        }
        return suggestions;
    }

    private String normalizeRole(String role) {
        if ("assistant".equals(role) || "user".equals(role)) return role;
        return "user";
    }

    private String trimTo(String value, int maxLength) {
        if (value == null) return "";
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private int safeCountPurchaseOrders(OrderStatus status) {
        Integer count = purchaseOrderRepository.countByStatus(status);
        return count == null ? 0 : count;
    }
}
