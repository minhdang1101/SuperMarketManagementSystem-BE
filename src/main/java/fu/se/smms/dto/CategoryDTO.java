package fu.se.smms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryDTO {
    private Integer categoryId;

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(min = 1, max = 100, message = "Tên danh mục phải từ 1 đến 100 ký tự")
    private String name;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    private String description;

    private Boolean status;

    private Integer productCount;
}
