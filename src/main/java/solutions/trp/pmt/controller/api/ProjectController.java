package solutions.trp.pmt.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import solutions.trp.pmt.controller.api.response.ApiResponse;
import solutions.trp.pmt.datasource.projects.ProjectEntity;
import solutions.trp.pmt.datasource.tasks.TaskEntity;
import solutions.trp.pmt.dto.FullProjectDto;
import solutions.trp.pmt.dto.ProjectDto;
import solutions.trp.pmt.service.ProjectService;
import solutions.trp.pmt.service.TaskService;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/project")
public class ProjectController {
    private final TaskService taskService;
    private final ProjectService projectService;

    @Autowired
    public ProjectController(ProjectService projectService, TaskService taskService) {
        this.projectService = projectService;
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectDto>>> getProjects(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String title
    ) {

        List<ProjectEntity> projects = projectService.search(title, offset, limit);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok(projects.stream().map(ProjectEntity::toDto).toList()));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<FullProjectDto>> getProject(
            @PathVariable int projectId
    ) {
        List<TaskEntity> tasks = taskService.getFromProjectId(projectId);
        ProjectEntity project = projectService.getFromId(projectId);

        FullProjectDto projectDto = project.toFullDto();
        projectDto.setTasks(tasks.stream().map(TaskEntity::toDto).toList());

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok(projectDto));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createProject(
            @RequestParam String title
    ) {

        projectService.createProject(title);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok());
    }

    @PostMapping("/{projectId}/task")
    public ResponseEntity<ApiResponse<Void>> createTask(
            @PathVariable int projectId,
            @RequestParam String title,
            @RequestParam(defaultValue = "false") boolean isCompleted,
            @RequestParam(required = false) LocalDateTime deadline,
            @RequestParam(defaultValue = "0") int estimatedTime
    ) {

        taskService.createTask(title, projectId, isCompleted, Timestamp.valueOf(deadline), estimatedTime);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok());
    }

    @DeleteMapping("/{projectId}/task")
    public ResponseEntity<ApiResponse<Void>> removeTask(
            @PathVariable int projectId,
            @RequestParam int taskId
    ) {

        taskService.deleteTask(taskId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok());
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<ApiResponse<Void>> removeProject(
            @PathVariable int projectId
    ) {

        projectService.deleteProject(projectId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok());
    }

    @PostMapping("/{projectId}/leader")
    public ResponseEntity<ApiResponse<Void>> addProjectLeader(
            @PathVariable int projectId,
            @RequestParam int userId
    ) {

        projectService.addProjectLeader(projectId, userId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok());
    }

    @DeleteMapping("/{projectId}/leader")
    public ResponseEntity<ApiResponse<Void>> removeProjectLeader(
            @PathVariable int projectId,
            @RequestParam int userId
    ) {

        projectService.removeProjectLeader(projectId, userId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok());
    }
}
