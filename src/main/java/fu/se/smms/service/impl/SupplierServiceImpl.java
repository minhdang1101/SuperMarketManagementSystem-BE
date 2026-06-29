package fu.se.smms.service.impl;

import fu.se.smms.dto.PurchaseOrderResponseDTO;
import fu.se.smms.dto.SupplierDTO;
import fu.se.smms.dto.SupplierResponseDTO;
import fu.se.smms.entity.PurchaseOrder;
import fu.se.smms.entity.Supplier;
import fu.se.smms.exception.ResourceNotFoundException;
import fu.se.smms.repository.PurchaseOrderRepository;
import fu.se.smms.repository.SupplierRepository;
import fu.se.smms.service.SupplierService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SupplierServiceImpl implements SupplierService {
    private static final Logger log = LoggerFactory.getLogger(SupplierServiceImpl.class);

    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    public SupplierServiceImpl(SupplierRepository supplierRepository,
                               PurchaseOrderRepository purchaseOrderRepository) {
        this.supplierRepository = supplierRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
    }

    @Override
    public Page<SupplierResponseDTO> search(String keyword, Boolean status, Pageable pageable) {
        String trimmedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        return supplierRepository.search(trimmedKeyword, status, pageable)
                .map(this::toResponseDTO);
    }

    @Override
    public SupplierResponseDTO findById(Integer id) {
        log.debug("Find supplier by id: {}", id);
        Supplier supplier = getSupplierOrThrow(id);
        return toResponseDTO(supplier);
    }

    @Override
    public SupplierResponseDTO findByIdWithPurchaseOrders(Integer id) {
        log.debug("Find supplier by id with purchase orders: {}", id);
        Supplier supplier = getSupplierOrThrow(id);

        List<PurchaseOrder> purchaseOrders = purchaseOrderRepository
                .findBySupplierIdOrderByOrderDateDesc(id);

        SupplierResponseDTO response = toResponseDTO(supplier);
        response.setPurchaseOrders(purchaseOrders.stream()
                .map(this::toPurchaseOrderSummary)
                .toList());
        return response;
    }

    @Override
    @Transactional
    public SupplierResponseDTO create(SupplierDTO dto) {
        log.debug("Creating supplier: {}", dto.getName());

        Supplier supplier = Supplier.builder()
                .name(dto.getName())
                .contactPerson(dto.getContactPerson())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .address(dto.getAddress())
                .status(Boolean.TRUE.equals(dto.getStatus()) || dto.getStatus() == null)
                .build();

        supplier = supplierRepository.save(supplier);
        log.info("Supplier created: id={}, name={}", supplier.getSupplierId(), supplier.getName());
        return toResponseDTO(supplier);
    }

    @Override
    @Transactional
    public SupplierResponseDTO update(Integer id, SupplierDTO dto) {
        log.debug("Updating supplier id: {}", id);
        Supplier supplier = getSupplierOrThrow(id);

        supplier.setName(dto.getName());
        supplier.setContactPerson(dto.getContactPerson());
        supplier.setPhone(dto.getPhone());
        supplier.setEmail(dto.getEmail());
        supplier.setAddress(dto.getAddress());
        if (dto.getStatus() != null) {
            supplier.setStatus(dto.getStatus());
        }

        supplier = supplierRepository.save(supplier);
        log.info("Supplier updated: id={}", supplier.getSupplierId());
        return toResponseDTO(supplier);
    }

    @Override
    @Transactional
    public SupplierResponseDTO toggleStatus(Integer id) {
        log.debug("Toggling status for supplier id: {}", id);
        Supplier supplier = getSupplierOrThrow(id);
        supplier.setStatus(!Boolean.TRUE.equals(supplier.getStatus()));
        supplier = supplierRepository.save(supplier);
        log.info("Supplier status toggled: id={}, newStatus={}", supplier.getSupplierId(), supplier.getStatus());
        return toResponseDTO(supplier);
    }

    private Supplier getSupplierOrThrow(Integer id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhà cung cấp: " + id));
    }

    private SupplierResponseDTO toResponseDTO(Supplier supplier) {
        return SupplierResponseDTO.builder()
                .supplierId(supplier.getSupplierId())
                .name(supplier.getName())
                .contactPerson(supplier.getContactPerson())
                .phone(supplier.getPhone())
                .email(supplier.getEmail())
                .address(supplier.getAddress())
                .status(supplier.getStatus())
                .build();
    }

    private PurchaseOrderResponseDTO toPurchaseOrderSummary(PurchaseOrder po) {
        return PurchaseOrderResponseDTO.builder()
                .poId(po.getPoId())
                .orderDate(po.getOrderDate())
                .status(po.getStatus())
                .totalAmount(po.getTotalAmount())
                .note(po.getNote())
                .expectedDeliveryDate(po.getExpectedDeliveryDate())
                .supplierId(po.getSupplier().getSupplierId())
                .supplierName(po.getSupplier().getName())
                .createdByUserId(po.getCreatedBy().getUserId())
                .createdByName(po.getCreatedBy().getName())
                .build();
    }
}
