package solutions.trp.pmt.controller.api.execption;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
