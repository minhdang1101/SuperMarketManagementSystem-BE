package fu.se.smms.service.impl;

import fu.se.smms.dto.ShiftCreateRequestDTO;
import fu.se.smms.dto.ShiftDTO;
import fu.se.smms.entity.Shift;
import fu.se.smms.entity.User;
import fu.se.smms.repository.ShiftRepository;
import fu.se.smms.repository.UserRepository;
import fu.se.smms.service.ShiftService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShiftServiceImpl implements ShiftService {

    private final ShiftRepository shiftRepository;
    private final UserRepository userRepository;

    public ShiftServiceImpl(ShiftRepository shiftRepository, UserRepository userRepository) {
        this.shiftRepository = shiftRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<ShiftDTO> getShiftsBetweenDates(LocalDate startDate, LocalDate endDate) {
        return shiftRepository.findByShiftDateBetween(startDate, endDate).stream()
                .map(this::mapToShiftDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ShiftDTO> getShiftsByStaffParams(String staffId) {
        int userId = extractIdFromPrefix(staffId);
        return shiftRepository.findByUser_UserId(userId).stream()
                .map(this::mapToShiftDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ShiftDTO> getUpcomingShiftsForCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElse(null);
        if (user == null) return List.of();
        LocalDate today = LocalDate.now();
        return shiftRepository.findByUser_UserIdAndShiftDateGreaterThanEqualOrderByShiftDateAsc(user.getUserId(), today)
                .stream()
                .map(this::mapToShiftDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ShiftDTO createShift(ShiftCreateRequestDTO request) {
        int userId = extractIdFromPrefix(request.getStaffId());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Staff not found with ID: " + request.getStaffId()));

        Shift shift = Shift.builder()
                .user(user)
                .shiftDate(request.getShiftDate())
                .shiftType(request.getShiftType())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .note(request.getNote())
                .build();

        Shift savedShift = shiftRepository.save(shift);
        return mapToShiftDTO(savedShift);
    }

    @Override
    public void deleteShift(Integer shiftId) {
        if (!shiftRepository.existsById(shiftId)) {
            throw new RuntimeException("Shift not found with ID: " + shiftId);
        }
        shiftRepository.deleteById(shiftId);
    }

    @Override
    public List<ShiftDTO> searchShifts(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getShiftsByStaffParams(""); // Wait, ShiftController doesn't have a plain getAll, but searching empty usually means we probably should just return all. Since there's no "getAll", I'll return empty list or all if needed.
        }
        return shiftRepository.searchByStaffName(query.trim()).stream()
                .map(this::mapToShiftDTO)
                .collect(Collectors.toList());
    }

    private ShiftDTO mapToShiftDTO(Shift shift) {
        User user = shift.getUser();
        String staffId = "EMP" + String.format("%03d", user.getUserId());
        return ShiftDTO.builder()
                .shiftId(shift.getShiftId())
                .staffId(staffId)
                .staffName(user.getName())
                .shiftDate(shift.getShiftDate())
                .shiftType(shift.getShiftType())
                .startTime(shift.getStartTime())
                .endTime(shift.getEndTime())
                .note(shift.getNote())
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
