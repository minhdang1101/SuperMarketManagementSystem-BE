package fu.se.smms.dto;

import fu.se.smms.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderStatusDTO {
    @NotNull(message = "Trạng thái không được để trống")
    private OrderStatus status;
}
