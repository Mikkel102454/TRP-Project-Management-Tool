package solutions.trp.pmt.controller.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import solutions.trp.pmt.controller.api.response.ApiResponse;
import solutions.trp.pmt.dto.request.CreateTaskRequest;
import solutions.trp.pmt.service.TaskService;

import java.sql.Timestamp;

@RestController
@RequestMapping("/api/task")
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/time/start")
    public ResponseEntity<ApiResponse<Void>> startTimeUser(
            @RequestParam() int taskId
    ) {

        taskService.startTimeUser(taskId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok());
    }

    @PostMapping("/time/stop")
    public ResponseEntity<ApiResponse<Void>> stopTimeUser(
            @RequestParam() int taskId
    ) {

        taskService.stopTimeUser(taskId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok());
    }

    @PostMapping("//schedule")
    public ResponseEntity<ApiResponse<Void>> scheduleUser(
            @RequestParam int taskId,
            @RequestParam int userId
    ) {

        taskService.scheduleUser(taskId, userId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok());
    }

    @DeleteMapping("/schedule")
    public ResponseEntity<ApiResponse<Void>> unscheduleUser(
            @RequestParam int taskId,
            @RequestParam int userId
    ) {

        taskService.unscheduleUser(taskId, userId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok());
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> removeTask(
            @RequestParam int taskId
    ) {

        taskService.deleteTask(taskId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok());
    }

    @PostMapping("/{projectId}/task")
    public ResponseEntity<ApiResponse<Void>> createTask(
            @RequestBody CreateTaskRequest request
    ) {

        taskService.createTask(
                request.title(),
                request.projectId(),
                request.isCompleted() != null ? request.isCompleted() : false,
                request.deadline() != null ? Timestamp.valueOf(request.deadline()) : null,
                request.estimatedTime() != null ? request.estimatedTime() : 0,
                request.description() != null ? request.description() : ""
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok());
    }
}
