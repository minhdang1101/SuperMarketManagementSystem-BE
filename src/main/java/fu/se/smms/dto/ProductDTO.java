package fu.se.smms.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDTO {
    private Integer productId;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(min = 1, max = 200, message = "Tên sản phẩm phải từ 1 đến 200 ký tự")
    private String name;

    @NotBlank(message = "Mã vạch không được để trống")
    @Size(min = 1, max = 50, message = "Mã vạch phải từ 1 đến 50 ký tự")
    private String barcode;

    @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
    private String description;

    @Size(max = 50, message = "Đơn vị tối đa 50 ký tự")
    private String unit;

    @NotNull(message = "Giá nhập không được để trống")
    @DecimalMin(value = "0", inclusive = false, message = "Giá nhập phải lớn hơn 0")
    @Digits(integer = 15, fraction = 4, message = "Giá nhập không hợp lệ")
    private BigDecimal costPrice;

    @NotNull(message = "Giá bán không được để trống")
    @DecimalMin(value = "0", inclusive = false, message = "Giá bán phải lớn hơn 0")
    @Digits(integer = 15, fraction = 4, message = "Giá bán không hợp lệ")
    private BigDecimal sellingPrice;

    @Min(value = 0, message = "Số lượng tồn kho không được âm")
    private Integer stockLevel;

    @Min(value = 0, message = "Mức tồn kho tối thiểu không được âm")
    private Integer minStockLevel;

    @Min(value = 0, message = "Mức tồn kho tối đa không được âm")
    private Integer maxStockLevel;

    @Builder.Default
    private List<String> images = new ArrayList<>();

    @NotNull(message = "Danh mục không được để trống")
    @Positive(message = "ID danh mục phải là số dương")
    private Integer categoryId;

    private Integer supplierId;

    private Boolean status;
}
