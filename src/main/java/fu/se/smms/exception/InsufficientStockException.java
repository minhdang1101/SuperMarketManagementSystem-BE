package fu.se.smms.exception;

import lombok.Getter;

@Getter
public class InsufficientStockException extends RuntimeException {

    private final String messageCode;
    private final Integer productId;
    private final Integer requestedQuantity;
    private final Integer availableStock;

    public InsufficientStockException(String messageCode, String message) {
        super(message);
        this.messageCode = messageCode;
        this.productId = null;
        this.requestedQuantity = null;
        this.availableStock = null;
    }

    public InsufficientStockException(String messageCode, String message, 
            Integer productId, Integer requestedQuantity, Integer availableStock) {
        super(message);
        this.messageCode = messageCode;
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
        this.availableStock = availableStock;
    }
}
