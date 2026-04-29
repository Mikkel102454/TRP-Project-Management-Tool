package solutions.trp.pmt.controller.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import solutions.trp.pmt.controller.api.response.ApiResponse;
import solutions.trp.pmt.datasource.users.UserEntity;
import solutions.trp.pmt.dto.UserDto;
import solutions.trp.pmt.dto.request.ChangePasswordRequest;
import solutions.trp.pmt.dto.request.RenameProjectRequest;
import solutions.trp.pmt.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/whoami")
    public ResponseEntity<ApiResponse<UserDto>> getWhoAmAi() {
        UserDto user = userService.getCurrentUser().toDto();
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.updatePassword(request.oldPassword(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers() {

        List<UserDto> users = userService.getAllUsers().stream().map(UserEntity::toDto).toList();

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok(users));
    }
}
