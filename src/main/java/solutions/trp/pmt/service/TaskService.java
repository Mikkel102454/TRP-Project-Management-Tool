package solutions.trp.pmt.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import solutions.trp.pmt.controller.api.execption.ConflictException;
import solutions.trp.pmt.controller.api.execption.NotFoundException;
import solutions.trp.pmt.controller.api.execption.ServiceException;
import solutions.trp.pmt.datasource.actives.ActiveEntity;
import solutions.trp.pmt.datasource.actives.ActiveRepository;
import solutions.trp.pmt.datasource.projects.ProjectEntity;
import solutions.trp.pmt.datasource.projects.ProjectRepository;
import solutions.trp.pmt.datasource.tasks.TaskEntity;
import solutions.trp.pmt.datasource.tasks.TaskRepository;
import solutions.trp.pmt.datasource.time_tables.TimeTableEntity;
import solutions.trp.pmt.datasource.time_tables.TimeTableRepository;
import solutions.trp.pmt.datasource.users.UserEntity;
import solutions.trp.pmt.datasource.users.UserRepository;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskService {
    private final ProjectRepository projectRepository;
    private final TaskRepository repository;
    private final ActiveRepository activeRepository;
    private final AppUserDetailsService appUserDetailsService;
    private final TimeTableRepository timeTableRepository;
    private final UserRepository userRepository;

    @Autowired
    public TaskService(TaskRepository repository, ProjectRepository projectRepository, ActiveRepository activeRepository, AppUserDetailsService appUserDetailsService, TimeTableRepository timeTableRepository, UserRepository userRepository) {
        this.repository = repository;
        this.projectRepository = projectRepository;
        this.activeRepository = activeRepository;
        this.appUserDetailsService = appUserDetailsService;
        this.timeTableRepository = timeTableRepository;
        this.userRepository = userRepository;
    }

    public List<TaskEntity> getFromProjectId(int projectId) {
        return repository.findAllByProjectId(projectId);
    }

    public void createTask(String title, int projectId, boolean isCompleted, Timestamp deadline, int estimatedTime) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));

        TaskEntity task = new TaskEntity();
        task.setTitle(title);
        task.setProjectEntity(project);
        task.setCompleted(isCompleted);
        task.setTaskOrder(repository.findMaxOrder() + 1);
        task.setDeadline(deadline);
        task.setEstimatedTime(estimatedTime);

        try {
            repository.save(task);
        } catch (Exception e) {
            throw new ServiceException("Failed to create task for project");
        }
    }

    public void deleteTask(int taskId) {
        repository.deleteById(taskId);
    }

    public void startTimeUser(int taskId) {
        UserEntity user = appUserDetailsService.getUserEntity();
        if(activeRepository.existsByUserIdAndTaskId(user.getId(), taskId)) {
            throw new ConflictException("User is already timed on this task");
        }

        TaskEntity task = repository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));

        ActiveEntity active = new ActiveEntity();
        active.setTaskEntity(task);
        active.setUserEntity(user);
        active.setStamp(LocalDateTime.now());

        activeRepository.save(active);
    }

    public void stopTimeUser(int taskId) {
        UserEntity user = appUserDetailsService.getUserEntity();
        if(activeRepository.existsByUserIdAndTaskId(user.getId(), taskId)) {
            throw new ConflictException("User is already timed on this task");
        }

        TaskEntity task = repository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));

        ActiveEntity active = activeRepository.findByUserIdAndTaskId(user.getId(), taskId).orElseThrow(() -> new NotFoundException("Task not found"));

        TimeTableEntity timeTable = new TimeTableEntity();
        timeTable.setTaskEntity(task);
        timeTable.setUserEntity(user);
        timeTable.setTime(Duration.between(active.getStamp(), LocalDateTime.now()).getSeconds());

        timeTableRepository.save(timeTable);

        activeRepository.delete(active);
    }

    public void startTimeUser(int taskId, int userId) {
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if(activeRepository.existsByUserIdAndTaskId(user.getId(), taskId)) {
            throw new ConflictException("User is already timed on this task");
        }

        TaskEntity task = repository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));

        ActiveEntity active = new ActiveEntity();
        active.setTaskEntity(task);
        active.setUserEntity(user);
        active.setStamp(LocalDateTime.now());

        activeRepository.save(active);
    }

    public void stopTimeUser(int taskId, int userId) {
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if(activeRepository.existsByUserIdAndTaskId(user.getId(), taskId)) {
            throw new ConflictException("User is already timed on this task");
        }

        TaskEntity task = repository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));

        ActiveEntity active = activeRepository.findByUserIdAndTaskId(user.getId(), taskId).orElseThrow(() -> new NotFoundException("Task not found"));

        TimeTableEntity timeTable = new TimeTableEntity();
        timeTable.setTaskEntity(task);
        timeTable.setUserEntity(user);
        timeTable.setTime(Duration.between(active.getStamp(), LocalDateTime.now()).getSeconds());

        timeTableRepository.save(timeTable);

        activeRepository.delete(active);
    }
}
