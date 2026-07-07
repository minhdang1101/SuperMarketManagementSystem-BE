package fu.se.smms.exception;

public class InvalidPromotionRuleException extends BusinessRuleException {

    public InvalidPromotionRuleException(String messageCode, String message) {
        super(messageCode, message);
    }
}
