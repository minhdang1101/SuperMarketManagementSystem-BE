package fu.se.smms.controller;

import fu.se.smms.dto.GoodsReceiptDTO;
import fu.se.smms.dto.GoodsReceiptResponseDTO;
import fu.se.smms.service.GoodsReceiptService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/v1/goods-receipts")
public class GoodsReceiptController {
    private static final Logger log = LoggerFactory.getLogger(GoodsReceiptController.class);
    private final GoodsReceiptService goodsReceiptService;

    public GoodsReceiptController(GoodsReceiptService goodsReceiptService) {
        this.goodsReceiptService = goodsReceiptService;
    }

    @PostMapping
    public ResponseEntity<GoodsReceiptResponseDTO> receiveGoods(@Valid @RequestBody GoodsReceiptDTO dto) {
        log.info("Receive goods for PO id: {}", dto.getPoId());
        return ResponseEntity.status(HttpStatus.CREATED).body(goodsReceiptService.receiveGoods(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GoodsReceiptResponseDTO> findById(@PathVariable @Positive Integer id) {
        log.debug("Find goods receipt by id: {}", id);
        return ResponseEntity.ok(goodsReceiptService.findById(id));
    }
}
