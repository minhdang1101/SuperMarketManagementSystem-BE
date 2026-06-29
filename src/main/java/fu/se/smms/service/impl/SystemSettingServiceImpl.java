package fu.se.smms.service.impl;

import fu.se.smms.dto.SystemSettingsDTO;
import fu.se.smms.entity.SystemSetting;
import fu.se.smms.repository.SystemSettingRepository;
import fu.se.smms.service.SystemSettingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemSettingServiceImpl implements SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;

    public SystemSettingServiceImpl(SystemSettingRepository systemSettingRepository) {
        this.systemSettingRepository = systemSettingRepository;
    }

    @Override
    public SystemSettingsDTO getSettings() {
        return SystemSettingsDTO.builder()
                .storeName(getSettingValue("STORE_NAME", "SuperMart"))
                .storeAddress(getSettingValue("STORE_ADDRESS", "123 Default Street, Default City"))
                .storePhone(getSettingValue("STORE_PHONE", "0123-456-789"))
                .storeEmail(getSettingValue("STORE_EMAIL", "contact@supermart.local"))
                .vatRate(Double.parseDouble(getSettingValue("VAT_RATE", "10.0")))
                .currency(getSettingValue("CURRENCY", "Vietnamese Dong (VND)"))
                .lowStockThreshold(Integer.parseInt(getSettingValue("LOW_STOCK_THRESHOLD", "10")))
                .build();
    }

    @Override
    @Transactional
    public SystemSettingsDTO updateSettings(SystemSettingsDTO request) {
        saveOrUpdateSetting("STORE_NAME", request.getStoreName(), "The name of the store");
        saveOrUpdateSetting("STORE_ADDRESS", request.getStoreAddress(), "The physical address of the store");
        saveOrUpdateSetting("STORE_PHONE", request.getStorePhone(), "The contact phone number");
        saveOrUpdateSetting("STORE_EMAIL", request.getStoreEmail(), "The contact email address");
        saveOrUpdateSetting("VAT_RATE", String.valueOf(request.getVatRate()), "Value Added Tax rate percentage");
        saveOrUpdateSetting("CURRENCY", request.getCurrency(), "The primary currency used in the system");
        saveOrUpdateSetting("LOW_STOCK_THRESHOLD", String.valueOf(request.getLowStockThreshold()),
                "Threshold to trigger low stock alerts");

        return getSettings();
    }

    private String getSettingValue(String key, String defaultValue) {
        return systemSettingRepository.findBySettingKey(key)
                .map(SystemSetting::getSettingValue)
                .orElseGet(() -> {
                    // Create default setting if it doesn't exist yet
                    SystemSetting newSetting = SystemSetting.builder()
                            .settingKey(key)
                            .settingValue(defaultValue)
                            // Basic description, can be updated later
                            .description("Auto-generated default for " + key)
                            .build();
                    systemSettingRepository.save(newSetting);
                    return defaultValue;
                });
    }

    private void saveOrUpdateSetting(String key, String value, String description) {
        SystemSetting setting = systemSettingRepository.findBySettingKey(key)
                .orElse(SystemSetting.builder()
                        .settingKey(key)
                        .description(description)
                        .build());

        setting.setSettingValue(value);
        if (setting.getDescription() == null) {
            setting.setDescription(description);
        }

        systemSettingRepository.save(setting);
    }
}
