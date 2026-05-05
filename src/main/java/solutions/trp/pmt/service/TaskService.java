package solutions.trp.pmt.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import solutions.trp.pmt.controller.api.execption.ConflictException;
import solutions.trp.pmt.controller.api.execption.NotFoundException;
import solutions.trp.pmt.controller.api.execption.ServiceException;
import solutions.trp.pmt.datasource.actives.ActiveEntity;
import solutions.trp.pmt.datasource.actives.ActiveRepository;
import solutions.trp.pmt.datasource.projects.ProjectEntity;
import solutions.trp.pmt.datasource.projects.ProjectRepository;
import solutions.trp.pmt.datasource.scheduled.ScheduledEntity;
import solutions.trp.pmt.datasource.scheduled.ScheduledRepository;
import solutions.trp.pmt.datasource.tasks.TaskEntity;
import solutions.trp.pmt.datasource.tasks.TaskRepository;
import solutions.trp.pmt.datasource.time_tables.TimingEntity;
import solutions.trp.pmt.datasource.time_tables.TimingRepository;
import solutions.trp.pmt.datasource.users.UserEntity;
import solutions.trp.pmt.datasource.users.UserRepository;
import solutions.trp.pmt.dto.TaskDto;
import solutions.trp.pmt.dto.UserDto;
import solutions.trp.pmt.util.PasswordEncoding;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class TaskService {
    private final ProjectRepository projectRepository;
    private final TaskRepository repository;
    private final ActiveRepository activeRepository;
    private final AppUserDetailsService appUserDetailsService;
    private final TimingRepository timingRepository;
    private final UserRepository userRepository;
    private final ScheduledRepository scheduledRepository;
    private final TimeService timeService;

    @Autowired
    public TaskService(TaskRepository repository, ProjectRepository projectRepository, ActiveRepository activeRepository, AppUserDetailsService appUserDetailsService, TimingRepository timingRepository, UserRepository userRepository, ScheduledRepository scheduledRepository, TimeService timeService) {
        this.repository = repository;
        this.projectRepository = projectRepository;
        this.activeRepository = activeRepository;
        this.appUserDetailsService = appUserDetailsService;
        this.timingRepository = timingRepository;
        this.userRepository = userRepository;
        this.scheduledRepository = scheduledRepository;
        this.timeService = timeService;
    }

    public List<TaskDto> getFromProjectId(int projectId) {
        List<TaskDto> tasks = repository.findAllByProjectEntity_IdOrderByTaskOrder(projectId).stream().map(taskEntity -> taskEntity.toDto(timeService)).toList();

        for (TaskDto task : tasks) {
            List<UserDto> users = getTaskActives(task.getId()).stream().map(UserEntity::toDto).toList();
            task.setActives(users);

            task.setScheduled(getScheduled(task.getId()).stream().map(UserEntity::toDto).toList());
        }
        return tasks;
    }

    public void createTask(String title, int projectId, boolean isCompleted, Timestamp deadline, int estimatedTime, String description) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));

        TaskEntity task = new TaskEntity();
        task.setTitle(title);
        task.setStatus(TaskEntity.TaskStatus.TODO);
        task.setProjectEntity(project);
        task.setCreatorEntity(appUserDetailsService.getUserEntity());
        task.setCompleted(isCompleted);
        task.setTaskOrder(repository.findMaxOrderByProjectId(projectId) + 1);
        task.setDeadline(deadline);
        task.setEstimatedTime(estimatedTime);
        task.setDescription(description);

        try {
            repository.save(task);
        } catch (Exception e) {
            throw new ServiceException("Failed to create task for project");
        }
    }

    public void updateTask(String title, int taskId, Boolean isCompleted, Timestamp deadline, Integer estimatedTime, String description, String status){
        TaskEntity task = repository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));

        if(title != null && !title.isBlank() && !Objects.equals(task.getTitle(), title)) {
            task.setTitle(title);
        }

        if(isCompleted != null) {
            task.setCompleted(isCompleted);
        }

        if(deadline != null) {
            task.setDeadline(deadline);
        }

        if(estimatedTime != null) {
            task.setEstimatedTime(estimatedTime);
        }

        if(description != null && !description.isBlank() && !Objects.equals(task.getDescription(), description)) {
            task.setDescription(description);
        }

        if(status != null && !status.isBlank() && !Objects.equals(task.getStatus(), TaskEntity.TaskStatus.valueOf(status))) {
            task.setStatus(TaskEntity.TaskStatus.valueOf(status));
        }

        repository.save(task);
    }

    @Transactional
    public void deleteTask(int taskId, boolean shift) {

        TaskEntity task = repository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));

        int projectId = task.getProjectEntity().getId();
        int deletedPriority = task.getTaskOrder();

        timingRepository.deleteByTaskEntity_Id(taskId);
        activeRepository.deleteByTaskEntity_Id(taskId);
        scheduledRepository.deleteByTaskEntity_Id(taskId);

        repository.delete(task);

        if(shift) {
            repository.shiftAfterDelete(projectId, deletedPriority);
        }
    }

    public List<UserEntity> getTaskActives(int taskId){
        return activeRepository.findAllByTaskEntity_Id(taskId).stream().map(ActiveEntity::getUserEntity).toList();
    }

    public List<UserEntity> getScheduled(int taskId){
        return scheduledRepository.findAllByTaskEntity_Id(taskId).stream().map(ScheduledEntity::getUserEntity).toList();
    }

    public void scheduleUser(int taskId, int userId) {
        if(scheduledRepository.existsByUserEntity_IdAndTaskEntity_Id(userId, taskId)) {
            throw new ConflictException("User is already scheduled for this task");
        }

        TaskEntity task = repository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));

        ScheduledEntity scheduled = new ScheduledEntity();
        scheduled.setTaskEntity(task);
        scheduled.setUserEntity(user);

        scheduledRepository.save(scheduled);
    }

    public void unscheduleUser(int taskId, int userId) {
        if(!scheduledRepository.existsByUserEntity_IdAndTaskEntity_Id(userId, taskId)) {
            throw new ConflictException("User is not scheduled for this task");
        }

        ScheduledEntity scheduled = scheduledRepository.findByUserEntity_IdAndTaskEntity_Id(userId, taskId).orElseThrow(() -> new NotFoundException("Schedule not found"));

        scheduledRepository.delete(scheduled);
    }

    @Transactional
    public void changeTaskPriority(int taskId, int newPriority) {

        TaskEntity task = repository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        int projectId = task.getProjectEntity().getId();
        int oldPriority = task.getTaskOrder();

        int maxPriority = repository.findMaxOrderByProjectId(projectId);

        // clamp
        if (newPriority < 1) newPriority = 1;
        if (newPriority > maxPriority) newPriority = maxPriority;

        if (oldPriority == newPriority) return;

        // move task completely out of range
        task.setTaskOrder(Integer.MAX_VALUE);
        repository.saveAndFlush(task);

        if (oldPriority < newPriority) {
            // moving DOWN shift others UP (decrement)

            repository.bumpRangeUp(projectId, oldPriority, newPriority);
            repository.shiftRangeDown(projectId, oldPriority, newPriority);

        } else {
            // moving UP shift others DOWN (increment)

            repository.bumpRangeDown(projectId, newPriority, oldPriority);
            repository.shiftRangeUp(projectId, newPriority, oldPriority);
        }

        // place task in final position
        task.setTaskOrder(newPriority);
        repository.save(task);
    }
}
