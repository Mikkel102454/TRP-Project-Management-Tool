package solutions.trp.pmt.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import solutions.trp.pmt.controller.api.response.ApiResponse;
import solutions.trp.pmt.dto.ProjectDto;
import solutions.trp.pmt.service.ProjectService;

import java.util.List;

@RestController
@RequestMapping("/api/display")
public class DisplayController {
    private final ProjectService projectService;

    public DisplayController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/projects")
    public ResponseEntity<ApiResponse<List<ProjectDto>>> getProjects() {
        List<ProjectDto> projects = projectService.getAll().stream()
                .filter(project -> !project.isArchived())
                .map(projectService::toDto)
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(projects));
    }
}
