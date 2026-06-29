package fu.se.smms.controller;

import fu.se.smms.dto.SystemSettingsDTO;
import fu.se.smms.service.SystemSettingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/settings")
public class SystemSettingController {

    private final SystemSettingService systemSettingService;

    public SystemSettingController(SystemSettingService systemSettingService) {
        this.systemSettingService = systemSettingService;
    }

    @GetMapping
    public ResponseEntity<SystemSettingsDTO> getSettings() {
        return ResponseEntity.ok(systemSettingService.getSettings());
    }

    @PutMapping
    public ResponseEntity<SystemSettingsDTO> updateSettings(@Valid @RequestBody SystemSettingsDTO request) {
        return ResponseEntity.ok(systemSettingService.updateSettings(request));
    }
}
