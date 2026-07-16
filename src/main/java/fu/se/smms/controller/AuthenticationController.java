package fu.se.smms.controller;

import fu.se.smms.config.JwtTokenProvider;
import fu.se.smms.dto.*;
import fu.se.smms.entity.RefreshToken;
import fu.se.smms.entity.User;
import fu.se.smms.service.AuthenticationService;
import fu.se.smms.service.RefreshTokenService;
import fu.se.smms.service.UserService;
import jakarta.validation.Valid;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationController.class);
    private static final String REFRESH_COOKIE_NAME = "refresh_token";

    private final AuthenticationService authenticationService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserDetailsService userDetailsService;
    private final UserService userService;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpirationMs;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public AuthenticationController(AuthenticationService authenticationService,
                                    JwtTokenProvider jwtTokenProvider,
                                    RefreshTokenService refreshTokenService,
                                    UserDetailsService userDetailsService,
                                    UserService userService) {
        this.authenticationService = authenticationService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.userDetailsService = userDetailsService;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequestDTO userDTO,
                                               HttpServletResponse response) {
        log.info("Login attempt for user: {}", userDTO.getUsername());
        UserDetailDTO userPrincipal = authenticationService.authenticate(userDTO);
        String accessToken = jwtTokenProvider.generateToken(userPrincipal);

        User user = authenticationService.getUserEntity(userPrincipal.getUsername());
        if (user != null) {
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
            addRefreshCookie(response, refreshToken.getToken(), (int) (refreshExpirationMs / 1000));
        }

        log.info("Login successful for user: {}", userDTO.getUsername());
        return ResponseEntity.ok(new LoginResponse(accessToken, jwtTokenProvider.getJwtExpiration()));
    }

    @PostMapping("/register")
    public ResponseEntity<StaffDTO> register(@RequestBody StaffDTO request) {
        if (request.getRole() == null || request.getRole().isBlank()) {
            request.setRole("CASHIER");
        }
        request.setStatus("Active");
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createStaff(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshValue = getRefreshTokenFromCookie(request);
        if (refreshValue == null || refreshValue.isBlank()) {
            clearRefreshCookie(response);
            return ResponseEntity.status(401).body(null);
        }
        RefreshToken refreshToken = refreshTokenService.findByToken(refreshValue);
        if (refreshToken == null) {
            clearRefreshCookie(response);
            return ResponseEntity.status(401).body(null);
        }
        var userDetail = (fu.se.smms.dto.UserDetailDTO) userDetailsService.loadUserByUsername(
                refreshToken.getUser().getUsername());
        String newAccessToken = jwtTokenProvider.generateToken(userDetail);
        return ResponseEntity.ok(new LoginResponse(newAccessToken, jwtTokenProvider.getJwtExpiration()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshValue = getRefreshTokenFromCookie(request);
        if (refreshValue != null && !refreshValue.isBlank()) {
            refreshTokenService.revokeByToken(refreshValue);
        }
        clearRefreshCookie(response);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        return ResponseEntity.ok(authenticationService.forgotPassword(request.getEmail(), frontendUrl));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        authenticationService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Mật khẩu đã được đặt lại thành công."));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> me(@AuthenticationPrincipal UserDetailDTO principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        UserDTO user = authenticationService.getCurrentUser(principal.getUsername());
        return user != null ? ResponseEntity.ok(user) : ResponseEntity.status(404).build();
    }

    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (REFRESH_COOKIE_NAME.equals(c.getName())) return c.getValue();
        }
        return null;
    }

    private void addRefreshCookie(HttpServletResponse response, String token, int maxAgeSeconds) {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // set true when using HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSeconds);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
