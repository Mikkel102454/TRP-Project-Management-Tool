package solutions.trp.pmt.controller.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import solutions.trp.pmt.controller.api.response.ApiResponse;
import solutions.trp.pmt.service.TaskService;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final TaskService taskService;

    public AdminController(TaskService taskService) {
        this.taskService = taskService;
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
}
