package fu.se.smms.service.impl;

import fu.se.smms.dto.LoginRequestDTO;
import fu.se.smms.dto.UserDetailDTO;
import fu.se.smms.dto.UserDTO;
import fu.se.smms.entity.User;
import fu.se.smms.exception.BadRequestException;
import fu.se.smms.repository.UserRepository;
import fu.se.smms.service.AuthenticationService;
import fu.se.smms.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationServiceImpl.class);
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public AuthenticationServiceImpl(AuthenticationManager authenticationManager,
                                     UserRepository userRepository,
                                     PasswordEncoder passwordEncoder,
                                     EmailService emailService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
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

    @Override
    public Map<String, Object> forgotPassword(String email, String frontendUrl) {
        String normalizedEmail = email == null ? "" : email.trim();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("Email không tồn tại trong hệ thống."));

        if (!Boolean.TRUE.equals(user.getStatus())) {
            throw new BadRequestException("Tài khoản đã bị vô hiệu hóa, không thể đặt lại mật khẩu.");
        }

        String token = UUID.randomUUID().toString();
        user.setResetPasswordToken(token);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        String resetLink = normalizeFrontendUrl(frontendUrl) + "/reset-password?token=" + token;
        boolean mailSent = emailService.sendPasswordResetEmail(user.getEmail(), resetLink);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", mailSent
                ? "Liên kết đặt lại mật khẩu đã được gửi tới email của bạn."
                : "Đã tạo liên kết đặt lại mật khẩu. Mail chưa được cấu hình nên dùng resetLink để test.");
        response.put("mailSent", mailSent);
        if (!mailSent) {
            response.put("resetLink", resetLink);
        }
        return response;
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new BadRequestException("Token đặt lại mật khẩu không hợp lệ.");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new BadRequestException("Mật khẩu mới phải có ít nhất 6 ký tự.");
        }

        User user = userRepository.findByResetPasswordToken(token.trim())
                .orElseThrow(() -> new BadRequestException("Token đặt lại mật khẩu không hợp lệ hoặc đã được sử dụng."));

        if (user.getResetPasswordTokenExpiry() == null || user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            user.setResetPasswordToken(null);
            user.setResetPasswordTokenExpiry(null);
            userRepository.save(user);
            throw new BadRequestException("Token đặt lại mật khẩu đã hết hạn.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        userRepository.save(user);
        log.info("Password reset successfully for user: {}", user.getUsername());
    }

    private String normalizeFrontendUrl(String frontendUrl) {
        String url = frontendUrl == null || frontendUrl.isBlank() ? "http://localhost:5173" : frontendUrl.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }
}
