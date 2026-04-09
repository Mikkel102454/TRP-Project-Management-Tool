package solutions.trp.pmt.controller.api.response;

public class ApiError {

    private final ApiErrorCode code;
    private final String message;

    public ApiError(ApiErrorCode code, String message) {
        this.code = code;
        this.message = message;
    }

    public ApiErrorCode getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}