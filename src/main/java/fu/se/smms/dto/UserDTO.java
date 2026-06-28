package fu.se.smms.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Integer userId;
    private String username;
    private String password;
    private String name;
    private String email;
    private String avatar;
    private String role;
    private Boolean status;
}
