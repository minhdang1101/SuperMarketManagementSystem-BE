package fu.se.smms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffDTO {
    private String id; // e.g. "EMP001" mapped from Integer id
    private String name;
    private String username; // Tên đăng nhập (dùng khi tạo tài khoản)
    private String email; // Email đăng nhập
    private String password; // Chỉ dùng khi tạo mới, không trả về trong response
    private String role;
    private String shift; // Will keep it mapped to "Morning" for now based on plan
    private String status; // "Active" or "Inactive" mapped from Boolean
    private String phone;
}
