package solutions.trp.pmt.integration;

public class IntegrationException extends RuntimeException {
    private final IntegrationFailure failure;

    public IntegrationException(IntegrationFailure failure, String message) {
        super(message);
        this.failure = failure;
    }

    public IntegrationException(IntegrationFailure failure, String message, Throwable cause) {
        super(message, cause);
        this.failure = failure;
    }

    public IntegrationFailure getFailure() {
        return failure;
    }
}
