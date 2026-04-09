package solutions.trp.pmt.controller.api.execption;

public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
