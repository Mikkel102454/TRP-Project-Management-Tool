package solutions.trp.pmt.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import solutions.trp.pmt.controller.api.execption.ConflictException;
import solutions.trp.pmt.controller.api.execption.NotFoundException;
import solutions.trp.pmt.controller.api.execption.ServiceException;
import solutions.trp.pmt.datasource.leaders.LeaderEntity;
import solutions.trp.pmt.datasource.leaders.LeaderRepository;
import solutions.trp.pmt.datasource.projects.ProjectEntity;
import solutions.trp.pmt.datasource.projects.ProjectRepository;
import solutions.trp.pmt.datasource.tasks.TaskEntity;
import solutions.trp.pmt.datasource.users.UserEntity;
import solutions.trp.pmt.datasource.users.UserRepository;
import solutions.trp.pmt.dto.TaskDto;
import solutions.trp.pmt.dto.ProjectDto;
import solutions.trp.pmt.dto.ProjectIntegrationDto;
import solutions.trp.pmt.datasource.integration.ProjectBindingEntity;
import solutions.trp.pmt.integration.IntegrationException;
import solutions.trp.pmt.integration.feature.FeatureApiClient;
import solutions.trp.pmt.service.integration.IntegrationBindingService;
import solutions.trp.pmt.service.integration.RemoteWorkItemService;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProjectService {
    private final ProjectRepository repository;
    private final UserRepository userRepository;
    private final LeaderRepository leaderRepository;
    private final TaskService taskService;
    private final TimeService timeService;
    private final IntegrationBindingService integrationBindingService;
    private final RemoteWorkItemService remoteWorkItemService;

    public ProjectService(ProjectRepository repository, UserRepository userRepository,
                          LeaderRepository leaderRepository, TaskService taskService) {
        this(repository, userRepository, leaderRepository, taskService, null, null, null);
    }

    @Autowired
    public ProjectService(ProjectRepository repository, UserRepository userRepository, LeaderRepository leaderRepository,
                          TaskService taskService, TimeService timeService,
                          IntegrationBindingService integrationBindingService, RemoteWorkItemService remoteWorkItemService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.leaderRepository = leaderRepository;
        this.taskService = taskService;
        this.timeService = timeService;
        this.integrationBindingService = integrationBindingService;
        this.remoteWorkItemService = remoteWorkItemService;
    }

    public ProjectEntity getFromId(int id){
        return repository.findById(id).orElseThrow(() ->
                new NotFoundException("Could not find project with id: " + id));
    }

    public void createProject(String title) {
        createProject(title, null, null);
    }

    @Transactional
    public void createProject(String title, String pmRelease) {
        createProject(title, pmRelease, null);
    }

    @Transactional
    public void createProject(String title, String pmRelease, Integer creatingUserId) {
        if(repository.existsByTitle(title)) {
            throw new ConflictException("A project already exists with that name");
        }
        boolean pmLinked = pmRelease != null && !pmRelease.isBlank();
        if (pmLinked && (creatingUserId == null || integrationBindingService == null
                || integrationBindingService.activeUserBinding(creatingUserId, FeatureApiClient.PROVIDER_KEY).isEmpty())) {
            throw new ConflictException("Connect a PM user account before creating a PM-linked project");
        }
        ProjectEntity project = new ProjectEntity();
        project.setTitle(title);
        project.setProjectOrder(repository.findMaxOrder() + 1);
        project.setArchived(false);
        try {
            ProjectEntity saved = repository.save(project);
            if (pmLinked) {
                integrationBindingService.setProjectRelease(saved.getId(), pmRelease);
            }
        } catch (Exception e) {
            throw new ServiceException("Failed to create project");
        }
    }

    public void renameProject(int id, String title) {
        if(repository.existsByTitle(title)) {
            throw new ConflictException("A project already exists with that name");
        }
        ProjectEntity project = repository.findById(id).orElseThrow(() -> new NotFoundException("Could not find project with id: " + id));
        project.setTitle(title);
        try {
            repository.save(project);
            if (integrationBindingService != null) integrationBindingService.updateProjectTitleSnapshot(id, title);
        } catch (Exception e) {
            throw new ServiceException("Failed to rename project");
        }
    }

    public List<ProjectEntity> search(String title, int offset) {
        Pageable pageable = PageRequest.of(offset / 500, 500);

        Page<ProjectEntity> page = repository.findProjects(
                title,
                pageable
        );

        return page.getContent();
    }

    @Transactional
    public void deleteProject(int projectId) {
        if (integrationBindingService != null) integrationBindingService.deleteProjectBinding(projectId);
        for(TaskDto task : taskService.getFromProjectId(projectId)) {
            taskService.deleteTask(task.getId(), false);
        }
        repository.deleteById(projectId);
    }

    public void addProjectLeader(int projectId, int userId) {
        if(leaderRepository.existsByUserEntity_IdAndProjectEntity_Id(userId, projectId)) {
            throw new ConflictException("This user is already a leader in this project");
        }

        ProjectEntity project = repository.findById(projectId).orElseThrow(() -> new NotFoundException("Could not find project with id: " + projectId));
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Could not find user with id: " + userId));

        LeaderEntity leaderEntity = new LeaderEntity();
        leaderEntity.setProjectEntity(project);
        leaderEntity.setUserEntity(user);

        leaderRepository.save(leaderEntity);
    }

    public void removeProjectLeader(int projectId, int userId) {
        if(!leaderRepository.existsByUserEntity_IdAndProjectEntity_Id(userId, projectId)) {
            throw new ConflictException("This user is not a leader in this project");
        }

        LeaderEntity leaderEntity = leaderRepository.findByUserEntity_IdAndProjectEntity_Id(userId, projectId).orElseThrow(() -> new NotFoundException("Could not find leader with user id: " + userId + " in this project"));

        leaderRepository.delete(leaderEntity);
    }

    public boolean isProjectWorkedOn(ProjectEntity project) {
        List<TaskDto> tasks = taskService.getFromProjectId(project.getId());
        for (TaskDto task : tasks){
            if(!task.getActives().isEmpty()) return true;
        }
        return false;
    }

    public List<UserEntity> getAllScheduledUsers(ProjectEntity project) {
        List<UserEntity> scheduledUsers = new ArrayList<>();

        for (TaskEntity task : project.getTasks()){
            if(task.getStatus() == TaskEntity.TaskStatus.FINISHED) continue;
            List<UserEntity> taskUsers = taskService.getScheduled(task.getId());
            for(UserEntity user : taskUsers){
                if(!scheduledUsers.contains(user)) scheduledUsers.add(user);
            }
        }
        return scheduledUsers;
    }

    public List<UserEntity> getProjectLeaders(ProjectEntity project) {
        return project.getLeaders().stream().map(LeaderEntity::getUserEntity).toList();
    }

    public List<ProjectEntity> getAll() {
        return repository.findAllByOrderByProjectOrder();
    }

    public boolean isVisibleToUser(ProjectEntity project, int userId) {
        if (integrationBindingService == null) return true;
        return integrationBindingService.activeProjectBinding(project.getId())
                .map(binding -> integrationBindingService
                        .activeUserBinding(userId, binding.getProvider())
                        .isPresent())
                .orElse(true);
    }

    public boolean hasConnectedPmUser(int userId) {
        return integrationBindingService != null
                && integrationBindingService.activeUserBinding(userId, FeatureApiClient.PROVIDER_KEY).isPresent();
    }

    public ProjectDto toDto(ProjectEntity project) {
        ProjectDto dto = project.toDto(timeService);
        if (integrationBindingService != null) integrationBindingService.activeProjectBinding(project.getId()).ifPresent(binding -> applyIntegration(dto, binding));
        return dto;
    }

    private void applyIntegration(ProjectDto dto, ProjectBindingEntity binding) {
        ProjectIntegrationDto integration = new ProjectIntegrationDto();
        integration.setProvider(binding.getProvider());
        integration.setRelease(binding.getScope());
        integration.setTasksReadOnly(true);
        try {
            List<TaskDto> tasks = remoteWorkItemService.list(binding);
            dto.setTasks(tasks);
            dto.setScheduled(tasks.stream().flatMap(task -> task.getScheduled().stream()).distinct().toList());
            dto.setIsWorkedOn(tasks.stream().anyMatch(TaskDto::isWorkedOn));
            integration.setAvailable(true);
        } catch (IntegrationException exception) {
            dto.setTasks(List.of());
            dto.setScheduled(List.of());
            dto.setIsWorkedOn(false);
            integration.setAvailable(false);
            integration.setUnavailableReason("PM tasks unavailable");
        }
        dto.setIntegration(integration);
    }

    @Transactional
    public void changeProjectPriority(int projectId, int newPriority) {
        ProjectEntity projectToMove = repository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Could not find project with id: " + projectId));

        if (projectToMove.isArchived()) {
            throw new ConflictException("Archived projects cannot be reordered from the dashboard");
        }

        List<ProjectEntity> activeProjects = new ArrayList<>(repository.findAllByArchivedOrderByProjectOrder(false));
        List<ProjectEntity> archivedProjects = repository.findAllByArchivedOrderByProjectOrder(true);

        if (newPriority < 1) newPriority = 1;
        if (newPriority > activeProjects.size()) newPriority = activeProjects.size();

        int oldIndex = activeProjects.indexOf(projectToMove);
        if (oldIndex < 0) {
            throw new NotFoundException("Could not find project with id: " + projectId);
        }

        int newIndex = newPriority - 1;
        if (oldIndex == newIndex) return;

        activeProjects.remove(oldIndex);
        activeProjects.add(newIndex, projectToMove);

        List<ProjectEntity> orderedProjects = new ArrayList<>();
        orderedProjects.addAll(activeProjects);
        orderedProjects.addAll(archivedProjects);

        int temporaryOrder = Integer.MIN_VALUE;
        for (ProjectEntity project : orderedProjects) {
            project.setProjectOrder(temporaryOrder++);
        }
        repository.saveAllAndFlush(orderedProjects);

        int order = 1;
        for (ProjectEntity project : orderedProjects) {
            project.setProjectOrder(order++);
        }
        repository.saveAll(orderedProjects);
    }

    public void archiveProject(int id){
        ProjectEntity project = repository.findById(id).orElseThrow(() -> new NotFoundException("Could not find project with id: " + id));
        project.setArchived(true);

        repository.save(project);
    }

    public void unarchiveProject(int id){
        ProjectEntity project = repository.findById(id).orElseThrow(() -> new NotFoundException("Could not find project with id: " + id));
        project.setArchived(false);

        repository.save(project);
    }
}
