package fu.se.smms.dto;

import fu.se.smms.enums.ShiftType;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ShiftCreateRequestDTO {
    @NotBlank(message = "Staff ID is required")
    private String staffId; // e.g. "EMP001"

    @NotNull(message = "Shift Date is required")
    private LocalDate shiftDate;

    @NotNull(message = "Shift Type is required")
    private ShiftType shiftType;

    private LocalTime startTime;
    private LocalTime endTime;

    private String note;
}
