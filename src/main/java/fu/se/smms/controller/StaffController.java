package fu.se.smms.controller;

import fu.se.smms.dto.StaffDTO;
import fu.se.smms.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/staff")
// You might want to restrict this later, e.g.,
// @PreAuthorize("hasRole('Manager')")
public class StaffController {

    private final UserService userService;

    public StaffController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<StaffDTO>> getAllStaff() {
        return ResponseEntity.ok(userService.getAllStaff());
    }

    @GetMapping("/search")
    public ResponseEntity<List<StaffDTO>> searchStaff(@RequestParam("query") String query) {
        return ResponseEntity.ok(userService.searchStaff(query));
    }

    @PostMapping
    public ResponseEntity<StaffDTO> createStaff(@RequestBody StaffDTO staffDTO) {
        return new ResponseEntity<>(userService.createStaff(staffDTO), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StaffDTO> updateStaff(@PathVariable String id, @RequestBody StaffDTO staffDTO) {
        return ResponseEntity.ok(userService.updateStaff(id, staffDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStaff(@PathVariable String id) {
        userService.deleteStaff(id);
        return ResponseEntity.noContent().build();
    }
}
