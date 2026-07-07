package fu.se.smms.service;

import fu.se.smms.dto.ChangePasswordRequestDTO;
import fu.se.smms.dto.StaffDTO;
import fu.se.smms.dto.UpdateProfileRequestDTO;
import fu.se.smms.dto.UserProfileDTO;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    UserProfileDTO getProfile(String username);

    UserProfileDTO updateProfile(String username, UpdateProfileRequestDTO request);

    UserProfileDTO uploadAvatar(String username, MultipartFile file);

    void changePassword(String username, ChangePasswordRequestDTO request);

    // Staff Management APIs
    List<StaffDTO> getAllStaff();

    StaffDTO createStaff(StaffDTO request);

    StaffDTO updateStaff(String prefixId, StaffDTO request);

    void deleteStaff(String prefixId);

    List<StaffDTO> searchStaff(String query);
}
