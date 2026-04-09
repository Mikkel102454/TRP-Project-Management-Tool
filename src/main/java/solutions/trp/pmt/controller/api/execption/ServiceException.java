package solutions.trp.pmt.controller.api.execption;

public class ServiceException extends RuntimeException {
    public ServiceException(String message) {
        super(message);
    }
}