package fu.se.smms.service.impl;

import fu.se.smms.dto.ChangePasswordRequestDTO;
import fu.se.smms.dto.StaffDTO;
import fu.se.smms.exception.BadRequestException;
import fu.se.smms.dto.UpdateProfileRequestDTO;
import fu.se.smms.dto.UserProfileDTO;
import fu.se.smms.entity.Role;
import fu.se.smms.entity.Shift;
import fu.se.smms.entity.User;
import fu.se.smms.repository.RoleRepository;
import fu.se.smms.repository.ShiftRepository;
import fu.se.smms.repository.UserRepository;
import fu.se.smms.service.FileStorageService;
import fu.se.smms.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

import java.util.List;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;
    private final ShiftRepository shiftRepository;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
            PasswordEncoder passwordEncoder, FileStorageService fileStorageService,
            ShiftRepository shiftRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileStorageService = fileStorageService;
        this.shiftRepository = shiftRepository;
    }

    @Override
    public UserProfileDTO getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        return UserProfileDTO.builder()
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole() != null ? user.getRole().getRoleName() : null)
                .avatar(user.getAvatar())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE)
                        : null)
                .build();
    }

    @Override
    public UserProfileDTO updateProfile(String username, UpdateProfileRequestDTO request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // Check if email is already taken by another user
        if (!user.getEmail().equalsIgnoreCase(request.getEmail())) {
            Optional<User> existingEmailUser = userRepository.findByEmail(request.getEmail());
            if (existingEmailUser.isPresent() && !existingEmailUser.get().getUserId().equals(user.getUserId())) {
                throw new RuntimeException("Email is already in use by another account");
            }
        }

        // Update fields
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        userRepository.save(user);

        return UserProfileDTO.builder()
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole() != null ? user.getRole().getRoleName() : null)
                .avatar(user.getAvatar())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE)
                        : null)
                .build();
    }

    @Override
    public UserProfileDTO uploadAvatar(String username, MultipartFile file) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // Store the file in "avatars" subdirectory
        String avatarPath = fileStorageService.storeFile(file, "avatars");

        // Update user avatar
        user.setAvatar(avatarPath);
        userRepository.save(user);

        return UserProfileDTO.builder()
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole() != null ? user.getRole().getRoleName() : null)
                .avatar(user.getAvatar())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE)
                        : null)
                .build();
    }

    @Override
    public void changePassword(String username, ChangePasswordRequestDTO request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("Incorrect old password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    // --- Staff Management Implementation ---

    @Override
    public List<StaffDTO> getAllStaff() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() != null)
                .map(this::mapToStaffDTO)
                .collect(Collectors.toList());
    }

    @Override
    public StaffDTO createStaff(StaffDTO request) {
        // Find role
        Role role = roleRepository.findByRoleName(request.getRole())
                .orElseThrow(() -> new RuntimeException("Role not found: " + request.getRole()));

        String username = (request.getUsername() != null && !request.getUsername().trim().isEmpty())
                ? request.getUsername().trim()
                : request.getName().replaceAll("\\s+", "").toLowerCase();
        String email = (request.getEmail() != null && !request.getEmail().trim().isEmpty())
                ? request.getEmail().trim()
                : username + "@smms.local";
        String rawPassword = (request.getPassword() != null && !request.getPassword().isEmpty())
                ? request.getPassword()
                : "123456";

        if (userRepository.findByUsername(username).isPresent()) {
            throw new BadRequestException("Tên đăng nhập đã tồn tại: " + username);
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BadRequestException("Email đã tồn tại: " + email);
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(rawPassword));
        newUser.setName(request.getName());
        newUser.setPhone(request.getPhone());
        newUser.setStatus("Active".equalsIgnoreCase(request.getStatus()));
        newUser.setRole(role);

        User savedUser = userRepository.save(newUser);
        StaffDTO dto = mapToStaffDTO(savedUser);
        dto.setPassword(null); // Never return password
        return dto;
    }

    @Override
    public StaffDTO updateStaff(String prefixId, StaffDTO request) {
        int id = extractIdFromPrefix(prefixId);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Staff not found with ID: " + prefixId));

        if (!user.getRole().getRoleName().equals(request.getRole())) {
            Role role = roleRepository.findByRoleName(request.getRole())
                    .orElseThrow(() -> new RuntimeException("Role not found: " + request.getRole()));
            user.setRole(role);
        }

        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setStatus("Active".equalsIgnoreCase(request.getStatus()));

        User updatedUser = userRepository.save(user);
        return mapToStaffDTO(updatedUser);
    }

    @Override
    public void deleteStaff(String prefixId) {
        int id = extractIdFromPrefix(prefixId);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Staff not found: " + prefixId));
        // Soft delete mapping to status = false
        user.setStatus(false);
        userRepository.save(user);
    }

    @Override
    public List<StaffDTO> searchStaff(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllStaff();
        }
        return userRepository.searchStaffByKeyword(query.trim()).stream()
                .filter(user -> user.getRole() != null)
                .map(this::mapToStaffDTO)
                .collect(Collectors.toList());
    }

    private StaffDTO mapToStaffDTO(User user) {
        String prefixId = "EMP" + String.format("%03d", user.getUserId());

        // Look up today's shift(s) for this user from the database
        LocalDate today = LocalDate.now();
        List<Shift> todayShifts = shiftRepository.findByUser_UserIdAndShiftDate(user.getUserId(), today);
        String shiftDisplay;
        if (todayShifts.isEmpty()) {
            shiftDisplay = "no shift";
        } else {
            shiftDisplay = todayShifts.stream()
                    .map(s -> s.getShiftType().name())
                    .collect(Collectors.joining(", "));
        }

        return StaffDTO.builder()
                .id(prefixId)
                .name(user.getName())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().getRoleName() : "N/A")
                .shift(shiftDisplay)
                .status(Boolean.TRUE.equals(user.getStatus()) ? "Active" : "Inactive")
                .phone(user.getPhone() != null ? user.getPhone() : "N/A")
                .build();
    }

    private int extractIdFromPrefix(String prefixId) {
        try {
            return Integer.parseInt(prefixId.replace("EMP", ""));
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid Staff ID format: " + prefixId);
        }
    }
}
