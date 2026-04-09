package solutions.trp.pmt.controller.api.execption;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}