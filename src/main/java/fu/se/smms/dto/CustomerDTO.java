package fu.se.smms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDTO {
    private String id; // Matches memberCardId
    private Integer customerId; // The internal integer ID
    private String name;
    private String phone;
    private String email;
    private Integer points;
    private String rank;
    private String joinDate; // Format as YYYY-MM-DD
}
