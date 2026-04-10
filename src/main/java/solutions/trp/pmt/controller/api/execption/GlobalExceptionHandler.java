package solutions.trp.pmt.controller.api.execption;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import solutions.trp.pmt.controller.api.response.ApiErrorCode;
import solutions.trp.pmt.controller.api.response.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

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