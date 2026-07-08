package fu.se.smms.service;

import fu.se.smms.dto.SystemSettingsDTO;

public interface SystemSettingService {
    SystemSettingsDTO getSettings();

    SystemSettingsDTO updateSettings(SystemSettingsDTO request);
}
