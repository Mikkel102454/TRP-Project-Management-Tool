package solutions.trp.pmt.controller.api.execption;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import solutions.trp.pmt.controller.api.response.ApiErrorCode;
import solutions.trp.pmt.controller.api.response.ApiResponse;
import solutions.trp.pmt.integration.IntegrationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IntegrationException.class)
    public ResponseEntity<ApiResponse<Void>> handleIntegration(IntegrationException ex) {
        ApiErrorCode code = switch (ex.getFailure()) {
            case AUTHENTICATION -> ApiErrorCode.INTEGRATION_AUTHENTICATION;
            case TIMEOUT -> ApiErrorCode.INTEGRATION_TIMEOUT;
            case INVALID_ACCOUNT -> ApiErrorCode.INTEGRATION_INVALID_ACCOUNT;
            case NOT_FOUND -> ApiErrorCode.INTEGRATION_NOT_FOUND;
            case BAD_REQUEST -> ApiErrorCode.BAD_REQUEST;
            case UNAVAILABLE -> ApiErrorCode.INTEGRATION_UNAVAILABLE;
        };
        int status = switch (ex.getFailure()) {
            case INVALID_ACCOUNT, BAD_REQUEST -> 400;
            case NOT_FOUND -> 404;
            case TIMEOUT -> 504;
            case AUTHENTICATION, UNAVAILABLE -> 503;
        };
        return ResponseEntity.status(status).body(ApiResponse.fail(code, ex.getMessage()));
    }

    /* ===================== 401 ===================== */

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(
            UnauthorizedException ex) {

        return ResponseEntity.status(401)
                .body(ApiResponse.fail(
                        ApiErrorCode.UNAUTHORIZED,
                        ex.getMessage()
                ));
    }

    /* ===================== 404 ===================== */

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(
            NotFoundException ex) {

        return ResponseEntity.status(404)
                .body(ApiResponse.fail(
                        ApiErrorCode.NOT_FOUND,
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handleNotFound(NoResourceFoundException ex) {
        return ResponseEntity.status(404)
                .body(ApiResponse.fail(
                        ApiErrorCode.NOT_FOUND,
                        "Could not find the request resource."

                ));
    }

    /* ===================== 409 ===================== */

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(
            ConflictException ex) {

        return ResponseEntity.status(409)
                .body(ApiResponse.fail(
                        ApiErrorCode.CONFLICT,
                        ex.getMessage()
                ));
    }

    /* ===================== 400 ===================== */

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            ValidationException ex) {

        return ResponseEntity.status(400)
                .body(ApiResponse.fail(
                        ApiErrorCode.VALIDATION_ERROR,
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationArgument(
            MethodArgumentNotValidException ex) {

        return ResponseEntity.status(400)
                .body(ApiResponse.fail(
                        ApiErrorCode.BAD_REQUEST,
                        "Bad request"
                ));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(
            BadRequestException ex) {

        return ResponseEntity.status(400)
                .body(ApiResponse.fail(
                        ApiErrorCode.BAD_REQUEST,
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleBadRequest(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(400)
                .body(ApiResponse.fail(
                        ApiErrorCode.BAD_REQUEST,
                        "Request body could not be read properly."
                ));
    }

    /* ===================== 500 (Service Errors) ===================== */

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleService(
            ServiceException ex) {

        ex.printStackTrace(); // internal logging

        return ResponseEntity.status(500)
                .body(ApiResponse.fail(
                        ApiErrorCode.INTERNAL_ERROR,
                        "Service temporarily unavailable"
                ));
    }

    /* ===================== 500 (Fallback) ===================== */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(
            Exception ex) {

        ex.printStackTrace(); // log for debugging

        return ResponseEntity.status(500)
                .body(ApiResponse.fail(
                        ApiErrorCode.INTERNAL_ERROR,
                        "Internal server error"
                ));
    }
}
