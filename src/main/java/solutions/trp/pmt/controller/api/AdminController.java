package solutions.trp.pmt.controller.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import solutions.trp.pmt.controller.api.response.ApiResponse;
import solutions.trp.pmt.dto.request.CreateUserRequest;
import solutions.trp.pmt.dto.request.UpdateUserRequest;
import solutions.trp.pmt.service.TaskService;
import solutions.trp.pmt.service.UserService;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final TaskService taskService;
    private final UserService userService;

    public AdminController(TaskService taskService, UserService userService) {
        this.taskService = taskService;
        this.userService = userService;
    }

    @PostMapping("/task/time/start")
    public ResponseEntity<ApiResponse<Void>> startTimeUser(
            @RequestParam() int taskId,
            @RequestParam() int userId
    ) {

        taskService.startTimeUser(taskId, userId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok());
    }

    @PostMapping("/task/time/stop")
    public ResponseEntity<ApiResponse<Void>> stopTimeUser(
            @RequestParam() int taskId,
            @RequestParam() int userId
    ) {

        taskService.stopTimeUser(taskId, userId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok());
    }

    @PostMapping("/user")
    public ResponseEntity<ApiResponse<Void>> createUser(
            @RequestBody CreateUserRequest request
    ) {

        userService.addUser(
                request.username(),
                request.password(),
                request.isAdmin() != null ? request.isAdmin() : false,
                request.isEnabled() != null ? request.isEnabled() : true
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok());
    }

    @PatchMapping("/user")
    public ResponseEntity<ApiResponse<Void>> updateUser(
            @RequestBody UpdateUserRequest request
    ) {

        userService.updateUser(
                request.userId(),
                request.username(),
                request.password(),
                request.isAdmin(),
                request.isEnabled()
        );

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok());
    }
}
