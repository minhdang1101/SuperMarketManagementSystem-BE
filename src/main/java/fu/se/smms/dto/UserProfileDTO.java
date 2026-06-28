package fu.se.smms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDTO {
    private String name;
    private String email;
    private String phone;
    private String role;
    private String avatar;
    private String createdAt;
}
