package fu.se.smms.controller;

import fu.se.smms.dto.ChangePasswordRequestDTO;
import fu.se.smms.dto.UpdateProfileRequestDTO;
import fu.se.smms.dto.UserProfileDTO;
import fu.se.smms.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileDTO> getProfile(Authentication authentication) {
        String username = authentication.getName();
        log.info("Fetching profile for user: {}", username);
        UserProfileDTO profile = userService.getProfile(username);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileDTO> updateProfile(
            Authentication authentication,
            @RequestBody UpdateProfileRequestDTO request) {
        String username = authentication.getName();
        log.info("Updating profile for user: {}", username);
        try {
            UserProfileDTO updatedProfile = userService.updateProfile(username, request);
            return ResponseEntity.ok(updatedProfile);
        } catch (Exception e) {
            log.error("Error updating profile for user: {}", username, e);
            throw e; // Or return a proper error response
        }
    }

    @PostMapping("/upload-avatar")
    public ResponseEntity<UserProfileDTO> uploadAvatar(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        String username = authentication.getName();
        log.info("Uploading avatar for user: {}", username);
        try {
            UserProfileDTO updatedProfile = userService.uploadAvatar(username, file);
            return ResponseEntity.ok(updatedProfile);
        } catch (Exception e) {
            log.error("Error uploading avatar for user: {}", username, e);
            throw e;
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(
            Authentication authentication,
            @RequestBody ChangePasswordRequestDTO request) {
        String username = authentication.getName();
        log.info("Changing password for user: {}", username);
        try {
            userService.changePassword(username, request);
            return ResponseEntity.ok("Password changed successfully");
        } catch (Exception e) {
            log.error("Error changing password for user: {}", username, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
