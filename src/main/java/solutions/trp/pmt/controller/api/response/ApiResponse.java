package solutions.trp.pmt.controller.api.response;

public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ApiError error;

    private ApiResponse(boolean success, T data, ApiError error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    /* ---------- Factory Methods ---------- */

    // Success with data
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    // Success without data
    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(true, null, null);
    }

    // Failure
    public static <T> ApiResponse<T> fail(ApiErrorCode code, String message) {
        return new ApiResponse<>(
                false,
                null,
                new ApiError(code, message)
        );
    }

    /* ---------- Getters ---------- */

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public ApiError getError() {
        return error;
    }
}