package fu.se.smms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSettingsDTO {
    @NotBlank(message = "Store Name is required")
    private String storeName;

    @NotBlank(message = "Store Address is required")
    private String storeAddress;

    @NotBlank(message = "Store Phone is required")
    private String storePhone;

    @NotBlank(message = "Store Email is required")
    private String storeEmail;

    @NotNull(message = "VAT Rate is required")
    @Min(value = 0, message = "VAT Rate cannot be less than 0")
    @Max(value = 100, message = "VAT Rate cannot exceed 100")
    private Double vatRate;

    @NotBlank(message = "Currency is required")
    private String currency; // e.g., "Vietnamese Dong (VND)"

    @NotNull(message = "Low Stock Warning Threshold is required")
    @Min(value = 0, message = "Low Stock Threshold cannot be negative")
    private Integer lowStockThreshold;
}
