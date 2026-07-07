package fu.se.smms.service;

import fu.se.smms.dto.LoginRequestDTO;
import fu.se.smms.dto.UserDetailDTO;
import fu.se.smms.dto.UserDTO;
import fu.se.smms.entity.User;

import java.util.List;

public interface AuthenticationService {
    UserDetailDTO authenticate(LoginRequestDTO loginUserDTO);
    UserDTO getCurrentUser(String username);
    User getUserEntity(String username);
    List<UserDTO> getAllUsers();
}
