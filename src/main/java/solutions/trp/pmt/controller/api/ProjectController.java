package solutions.trp.pmt.controller.api;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import solutions.trp.pmt.controller.api.execption.NotFoundException;
import solutions.trp.pmt.controller.api.response.ApiResponse;
import solutions.trp.pmt.datasource.projects.ProjectEntity;
import solutions.trp.pmt.datasource.users.UserEntity;
import solutions.trp.pmt.dto.ProjectDto;
import solutions.trp.pmt.dto.TaskDto;
import solutions.trp.pmt.dto.request.*;
import solutions.trp.pmt.service.ProjectService;
import solutions.trp.pmt.service.TaskService;
import solutions.trp.pmt.service.TimeService;
import solutions.trp.pmt.service.UserService;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/project")
public class ProjectController {
    private final TaskService taskService;
    private final ProjectService projectService;
    private final TimeService timeService;
    private final UserService userService;

    @Autowired
    public ProjectController(ProjectService projectService, TaskService taskService, TimeService timeService,
                             UserService userService) {
        this.projectService = projectService;
        this.taskService = taskService;
        this.timeService = timeService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectDto>>> getProjects(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) String title,
            Authentication authentication
    ) {

        List<ProjectEntity> projects = projectService.search(title, offset);

        List<ProjectDto> projectDtos = visibleProjects(projects, authentication).stream()
                .map(projectService::toDto).toList();

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok(projectDtos));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectDto>> getProject(
            @PathVariable int projectId,
            Authentication authentication
    ) {
        ProjectEntity project = projectService.getFromId(projectId);
        requireVisible(project, authentication);

        ProjectDto projectDto = projectService.toDto(project);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok(projectDto));
    }

    @GetMapping("/details")
    public ResponseEntity<ApiResponse<List<ProjectDto>>> getAllProjectDetails(Authentication authentication) {

        List<ProjectEntity> projects = projectService.getAll();

        List<ProjectDto> projectDtos = visibleProjects(projects, authentication).stream().map(project -> {
            return projectService.toDto(project);

        }).toList();

        return ResponseEntity.ok(ApiResponse.ok(projectDtos));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createProject(
            @Valid @RequestBody CreateProjectRequest request
    ) {

        projectService.createProject(request.title(), request.pmRelease(), userService.getCurrentUser().getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok());
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<Void>> renameProject(
            @Valid @RequestBody RenameProjectRequest request
    ) {

        projectService.renameProject(request.id(), request.title());

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok());
    }

    @PostMapping("/order")
    public ResponseEntity<ApiResponse<Void>> changeProjectPriority(
            @RequestParam int projectId,
            @RequestParam int priority
    ) {

        projectService.changeProjectPriority(projectId, priority);

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

    @PostMapping("/{projectId}/archive")
    public ResponseEntity<ApiResponse<Void>> archiveProject(
            @PathVariable int projectId
    ) {

        projectService.archiveProject(projectId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok());
    }

    @DeleteMapping("/{projectId}/archive")
    public ResponseEntity<ApiResponse<Void>> unarchiveProject(
            @PathVariable int projectId
    ) {

        projectService.unarchiveProject(projectId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok());
    }

    private List<ProjectEntity> visibleProjects(List<ProjectEntity> projects, Authentication authentication) {
        if (hasApiRole(authentication)) return projects;
        int userId = userService.getCurrentUser().getId();
        return projects.stream()
                .filter(project -> projectService.isVisibleToUser(project, userId))
                .toList();
    }

    private void requireVisible(ProjectEntity project, Authentication authentication) {
        if (hasApiRole(authentication)) return;
        if (!projectService.isVisibleToUser(project, userService.getCurrentUser().getId())) {
            throw new NotFoundException("Could not find project with id: " + project.getId());
        }
    }

    private boolean hasApiRole(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_API".equals(authority.getAuthority()));
    }
}
