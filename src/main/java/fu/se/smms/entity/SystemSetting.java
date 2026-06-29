package fu.se.smms.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "system_setting")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setting_id")
    private Integer settingId;

    @Column(name = "setting_key", nullable = false, unique = true, columnDefinition = "NVARCHAR(100)")
    private String settingKey;

    @Column(name = "setting_value", nullable = false, columnDefinition = "NVARCHAR(1000)")
    private String settingValue;

    @Column(name = "description", columnDefinition = "NVARCHAR(500)")
    private String description;
}
