package fu.se.smms.controller;

import fu.se.smms.dto.ShiftCreateRequestDTO;
import fu.se.smms.dto.ShiftDTO;
import fu.se.smms.service.ShiftService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/shifts")
public class ShiftController {

    private final ShiftService shiftService;

    public ShiftController(ShiftService shiftService) {
        this.shiftService = shiftService;
    }

    @GetMapping("/me")
    public ResponseEntity<List<ShiftDTO>> getMyUpcomingShifts(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        String username = authentication.getName();
        return ResponseEntity.ok(shiftService.getUpcomingShiftsForCurrentUser(username));
    }

    @GetMapping
    public ResponseEntity<List<ShiftDTO>> getShiftsBetweenDates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(shiftService.getShiftsBetweenDates(start, end));
    }

    @GetMapping("/staff/{staffId}")
    public ResponseEntity<List<ShiftDTO>> getShiftsByStaffParams(@PathVariable String staffId) {
        return ResponseEntity.ok(shiftService.getShiftsByStaffParams(staffId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ShiftDTO>> searchShifts(@RequestParam("query") String query) {
        return ResponseEntity.ok(shiftService.searchShifts(query));
    }

    @PostMapping
    public ResponseEntity<ShiftDTO> createShift(@Valid @RequestBody ShiftCreateRequestDTO request) {
        return new ResponseEntity<>(shiftService.createShift(request), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShift(@PathVariable Integer id) {
        shiftService.deleteShift(id);
        return ResponseEntity.noContent().build();
    }
}
