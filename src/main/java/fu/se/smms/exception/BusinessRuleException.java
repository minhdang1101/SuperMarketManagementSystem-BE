package fu.se.smms.exception;

import lombok.Getter;

@Getter
public class BusinessRuleException extends RuntimeException {

    private final String messageCode;

    public BusinessRuleException(String messageCode, String message) {
        super(message);
        this.messageCode = messageCode;
    }
}
