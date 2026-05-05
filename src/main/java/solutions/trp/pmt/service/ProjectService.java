package solutions.trp.pmt.service;

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

import java.util.ArrayList;
import java.util.List;

@Service
public class ProjectService {
    private final ProjectRepository repository;
    private final UserRepository userRepository;
    private final LeaderRepository leaderRepository;
    private final TaskService taskService;

    @Autowired
    public ProjectService(ProjectRepository repository, UserRepository userRepository, LeaderRepository leaderRepository, TaskService taskService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.leaderRepository = leaderRepository;
        this.taskService = taskService;
    }

    public ProjectEntity getFromId(int id){
        return repository.findById(id).orElseThrow(() ->
                new NotFoundException("Could not find project with id: " + id));
    }

    public void createProject(String title) {
        if(repository.existsByTitle(title)) {
            throw new ConflictException("A project already exists with that name");
        }
        ProjectEntity project = new ProjectEntity();
        project.setTitle(title);
        project.setProjectOrder(repository.findMaxOrder() + 1);
        try {
            repository.save(project);
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
        } catch (Exception e) {
            throw new ServiceException("Failed to rename project");
        }
    }

    public List<ProjectEntity> search(String title, int offset, int limit) {
        if (limit > 50) limit = 50;
        Pageable pageable = PageRequest.of(offset / limit, limit);

        Page<ProjectEntity> page = repository.findProjects(
                title,
                pageable
        );

        return page.getContent();
    }

    public void deleteProject(int projectId) {
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
        List<TaskDto> tasks = taskService.getFromProjectId(project.getId());

        for (TaskDto task : tasks){
            if(task.getStatus() == TaskEntity.TaskStatus.FINISHED) continue;
            List<UserEntity> taskUsers = taskService.getScheduled(task.getId());
            for(UserEntity user : taskUsers){
                if(!scheduledUsers.contains(user)) scheduledUsers.add(user);
            }
        }
        return scheduledUsers;
    }

    public List<UserEntity> getProjectLeaders(ProjectEntity project) {
        return leaderRepository.findAllByProjectEntity_Id(project.getId()).stream().map(LeaderEntity::getUserEntity).toList();
    }

    public List<ProjectEntity> getAll() {
        return repository.findAll();
    }
}
