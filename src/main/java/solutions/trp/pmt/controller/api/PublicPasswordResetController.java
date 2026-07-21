package solutions.trp.pmt.controller.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import solutions.trp.pmt.controller.api.response.ApiResponse;
import solutions.trp.pmt.dto.request.PasswordResetConfirmRequest;
import solutions.trp.pmt.dto.request.PasswordResetRequest;
import solutions.trp.pmt.service.PasswordResetService;

@RestController
@RequestMapping("/api/public/password-reset")
public class PublicPasswordResetController {
    private final PasswordResetService passwordResetService;

    public PublicPasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/request")
    public ResponseEntity<ApiResponse<Void>> requestReset(
            @Valid @RequestBody PasswordResetRequest request,
            HttpServletRequest servletRequest
    ) {
        passwordResetService.requestReset(request.email(), resetPageUrl(servletRequest));
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmReset(
            @Valid @RequestBody PasswordResetConfirmRequest request
    ) {
        passwordResetService.confirmReset(request.token(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    private String resetPageUrl(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName() + serverPort(request)
                + request.getContextPath() + "/reset-password";
    }

    private String serverPort(HttpServletRequest request) {
        int port = request.getServerPort();
        if((request.getScheme().equals("http") && port == 80)
                || (request.getScheme().equals("https") && port == 443)) {
            return "";
        }
        return ":" + port;
    }
}
