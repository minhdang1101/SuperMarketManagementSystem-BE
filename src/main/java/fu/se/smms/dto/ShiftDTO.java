package fu.se.smms.dto;

import fu.se.smms.enums.ShiftType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftDTO {
    private Integer shiftId;
    private String staffId; // e.g. "EMP001"
    private String staffName;
    private LocalDate shiftDate;
    private ShiftType shiftType;
    private LocalTime startTime;
    private LocalTime endTime;
    private String note;
}
