package solutions.trp.pmt.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import solutions.trp.pmt.controller.api.response.ApiResponse;
import solutions.trp.pmt.datasource.projects.ProjectEntity;
import solutions.trp.pmt.datasource.users.UserEntity;
import solutions.trp.pmt.dto.FullProjectDto;
import solutions.trp.pmt.dto.ProjectDto;
import solutions.trp.pmt.dto.TaskDto;
import solutions.trp.pmt.dto.request.*;
import solutions.trp.pmt.service.ProjectService;
import solutions.trp.pmt.service.TaskService;

import java.sql.Timestamp;
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

        List<ProjectDto> projectDtos = projects.stream().map(project -> {
            ProjectDto dto = project.toDto();
            dto.setIsWorkedOn(projectService.isProjectWorkedOn(project));
            dto.setScheduled(projectService.getAllScheduledUsers(project).stream().map(UserEntity::toDto).toList());
            dto.setLeader(projectService.getProjectLeaders(project).stream().map(UserEntity::toDto).toList());
            return dto;
        }).toList();

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok(projectDtos));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<FullProjectDto>> getProject(
            @PathVariable int projectId
    ) {
        List<TaskDto> tasks = taskService.getFromProjectId(projectId);
        ProjectEntity project = projectService.getFromId(projectId);

        FullProjectDto projectDto = project.toFullDto();
        projectDto.setTasks(tasks);
        projectDto.setIsWorkedOn(projectService.isProjectWorkedOn(project));
        projectDto.setLeader(projectService.getProjectLeaders(project).stream().map(UserEntity::toDto).toList());
        projectDto.setScheduled(projectService.getAllScheduledUsers(project).stream().map(UserEntity::toDto).toList());

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok(projectDto));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createProject(
            @RequestBody CreateProjectRequest request
    ) {

        projectService.createProject(request.title());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok());
    }

    @PostMapping("/{projectId}/task")
    public ResponseEntity<ApiResponse<Void>> createTask(
            @PathVariable int projectId,
            @RequestBody CreateTaskRequest request
    ) {

        taskService.createTask(
                request.title(),
                projectId,
                request.isCompleted() != null ? request.isCompleted() : false,
                request.deadline() != null ? Timestamp.valueOf(request.deadline()) : null,
                request.estimatedTime() != null ? request.estimatedTime() : 0,
                request.description() != null ? request.description() : ""
        );

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

    @PostMapping("/{projectId}/schedule")
    public ResponseEntity<ApiResponse<Void>> scheduleUser(
            @PathVariable int projectId,
            @RequestParam int taskId,
            @RequestParam int userId
    ) {

        taskService.scheduleUser(taskId, userId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok());
    }

    @DeleteMapping("/{projectId}/schedule")
    public ResponseEntity<ApiResponse<Void>> unscheduleUser(
            @PathVariable int projectId,
            @RequestParam int taskId,
            @RequestParam int userId
    ) {

        taskService.unscheduleUser(taskId, userId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok());
    }
}
