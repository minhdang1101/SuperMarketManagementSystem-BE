package fu.se.smms.service;

import fu.se.smms.dto.ShiftCreateRequestDTO;
import fu.se.smms.dto.ShiftDTO;

import java.time.LocalDate;
import java.util.List;

public interface ShiftService {
    List<ShiftDTO> getShiftsBetweenDates(LocalDate startDate, LocalDate endDate);

    List<ShiftDTO> getShiftsByStaffParams(String staffId);

    List<ShiftDTO> getUpcomingShiftsForCurrentUser(String username);

    ShiftDTO createShift(ShiftCreateRequestDTO request);

    void deleteShift(Integer shiftId);

    List<ShiftDTO> searchShifts(String query);
}
