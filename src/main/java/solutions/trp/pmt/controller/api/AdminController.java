package solutions.trp.pmt.controller.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import solutions.trp.pmt.controller.api.response.ApiResponse;
import solutions.trp.pmt.datasource.users.UserEntity;
import solutions.trp.pmt.dto.UserDto;
import solutions.trp.pmt.dto.AdminUserDto;
import solutions.trp.pmt.dto.request.CreateUserRequest;
import solutions.trp.pmt.dto.request.SetUserIntegrationRequest;
import solutions.trp.pmt.dto.request.UpdateUserRequest;
import solutions.trp.pmt.service.TaskService;
import solutions.trp.pmt.service.TimeService;
import solutions.trp.pmt.service.UserService;
import solutions.trp.pmt.service.integration.IntegrationBindingService;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final TaskService taskService;
    private final UserService userService;
    private final TimeService timeService;
    private final IntegrationBindingService integrationBindingService;

    public AdminController(TaskService taskService, UserService userService, TimeService timeService,
                           IntegrationBindingService integrationBindingService) {
        this.taskService = taskService;
        this.userService = userService;
        this.timeService = timeService;
        this.integrationBindingService = integrationBindingService;
    }

    @PostMapping("/task/time/start")
    public ResponseEntity<ApiResponse<Void>> startTimeUser(
            @RequestParam(required = false) Integer taskId,
            @RequestParam(required = false) String taskRef,
            @RequestParam() int userId
    ) {

        timeService.startTimeUser(taskId, taskRef, userId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok());
    }

    @PostMapping("/task/time/stop")
    public ResponseEntity<ApiResponse<Void>> stopTimeUser(
            @RequestParam(required = false) Integer taskId,
            @RequestParam(required = false) String taskRef,
            @RequestParam() int userId
    ) {

        timeService.stopTimeUser(taskId, taskRef, userId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok());
    }

    @PostMapping("/user")
    public ResponseEntity<ApiResponse<Void>> createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {

        userService.addUser(
                request.username(),
                request.initial(),
                request.password(),
                request.email(),
                request.isAdmin() != null ? request.isAdmin() : false,
                request.isEnabled() != null ? request.isEnabled() : true,
                request.pmUserId()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok());
    }

    @PatchMapping("/user")
    public ResponseEntity<ApiResponse<Void>> updateUser(
            @Valid @RequestBody UpdateUserRequest request
    ) {

        userService.updateUser(
                request.userId(),
                request.username(),
                request.initial(),
                request.password(),
                request.email(),
                request.isAdmin(),
                request.isEnabled(),
                request.pmUserId()
        );

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok());
    }

    @GetMapping("/user")
    public ResponseEntity<ApiResponse<List<AdminUserDto>>> getUsers() {
        return ResponseEntity.ok(ApiResponse.ok(integrationBindingService.getAdminUsers()));
    }

    @PutMapping("/user/{userId}/pm-account")
    public ResponseEntity<ApiResponse<AdminUserDto>> setUserPmAccount(@PathVariable int userId,
                                                                      @RequestBody SetUserIntegrationRequest request) {
        integrationBindingService.setUserBinding(userId, request.pmUserId());
        return ResponseEntity.ok(ApiResponse.ok(integrationBindingService.getAdminUser(userId)));
    }

    @DeleteMapping("/user/{userId}/pm-account")
    public ResponseEntity<ApiResponse<Void>> removeUserPmAccount(@PathVariable int userId) {
        integrationBindingService.setUserBinding(userId, null);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
