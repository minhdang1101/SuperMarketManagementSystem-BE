package fu.se.smms.exception;

public class PromotionOverlapException extends BusinessRuleException {

    public PromotionOverlapException(String messageCode, String message) {
        super(messageCode, message);
    }
}
