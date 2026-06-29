package fu.se.smms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierDTO {
    private Integer supplierId;

    @NotBlank(message = "Tên nhà cung cấp không được để trống")
    @Size(min = 1, max = 200, message = "Tên nhà cung cấp phải từ 1 đến 200 ký tự")
    private String name;

    @Size(max = 100, message = "Tên người liên hệ tối đa 100 ký tự")
    private String contactPerson;

    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    private String phone;

    @Email(message = "Email không hợp lệ")
    @Size(max = 100, message = "Email tối đa 100 ký tự")
    private String email;

    @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự")
    private String address;

    private Boolean status;
}
