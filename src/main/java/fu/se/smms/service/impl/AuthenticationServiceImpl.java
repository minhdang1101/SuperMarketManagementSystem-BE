package fu.se.smms.service.impl;

import fu.se.smms.dto.LoginRequestDTO;
import fu.se.smms.dto.UserDetailDTO;
import fu.se.smms.dto.UserDTO;
import fu.se.smms.entity.User;
import fu.se.smms.repository.UserRepository;
import fu.se.smms.service.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationServiceImpl.class);
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    public AuthenticationServiceImpl(AuthenticationManager authenticationManager,
                                     UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
    }
    @Override
    public UserDetailDTO authenticate(LoginRequestDTO loginUserDTO) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginUserDTO.getUsername(), loginUserDTO.getPassword()));
        } catch (BadCredentialsException e) {
            log.warn("Login failed - invalid credentials for user: {}", loginUserDTO.getUsername());
            throw e;
        }
        User user = userRepository.findByUsername(loginUserDTO.getUsername()).orElseThrow();
        log.debug("User authenticated: {} (id: {})", user.getUsername(), user.getUserId());
        UserDTO userDTO = UserDTO.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .password(user.getPassword())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().getRoleName() : null)
                .status(user.getStatus())
                .build();
        return new UserDetailDTO(userDTO);
    }
    @Override
    public User getUserEntity(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    @Override
    public UserDTO getCurrentUser(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return null;
        return UserDTO.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .role(user.getRole() != null ? user.getRole().getRoleName() : null)
                .status(user.getStatus())
                .build();
    }
    @Override
    public List<UserDTO> getAllUsers() {
        log.debug("Get all users");
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            return List.of();
        }
        return users.stream().map(u -> UserDTO.builder()
                .userId(u.getUserId())
                .username(u.getUsername())
                .password(u.getPassword())
                .name(u.getName())
                .email(u.getEmail())
                .role(u.getRole() != null ? u.getRole().getRoleName() : null)
                .status(u.getStatus())
                .build()).toList();
    }
}
