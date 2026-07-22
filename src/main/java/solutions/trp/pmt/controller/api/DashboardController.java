package solutions.trp.pmt.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import solutions.trp.pmt.controller.api.response.ApiResponse;
import solutions.trp.pmt.datasource.users.UserEntity;
import solutions.trp.pmt.dto.DashboardDto;
import solutions.trp.pmt.dto.ProjectDto;
import solutions.trp.pmt.dto.UserDto;
import solutions.trp.pmt.service.ProjectService;
import solutions.trp.pmt.service.TimeService;
import solutions.trp.pmt.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final UserService userService;
    private final ProjectService projectService;
    private final TimeService timeService;

    public DashboardController(UserService userService, ProjectService projectService, TimeService timeService) {
        this.userService = userService;
        this.projectService = projectService;
        this.timeService = timeService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<DashboardDto>> getDashboard() {
        DashboardDto dashboard = new DashboardDto();

        UserDto currentUser = userService.getCurrentUser().toDto();
        List<UserDto> users = userService.getAllUsers().stream().map(UserEntity::toDto).toList();
        List<ProjectDto> projects = projectService.search("", 0).stream()
                .map(project -> project.toDto(timeService))
                .toList();

        dashboard.setCurrentUser(currentUser);
        dashboard.setUsers(users);
        dashboard.setProjects(projects);

        return ResponseEntity.ok(ApiResponse.ok(dashboard));
    }
}
