package fu.se.smms.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierResponseDTO {
    private Integer supplierId;
    private String name;
    private String contactPerson;
    private String phone;
    private String email;
    private String address;
    private Boolean status;
    private List<PurchaseOrderResponseDTO> purchaseOrders;
}
