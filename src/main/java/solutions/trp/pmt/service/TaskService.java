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
    private final TimingRepository timingRepository;
    private final UserRepository userRepository;
    private final ScheduledRepository scheduledRepository;

    @Autowired
    public TaskService(TaskRepository repository, ProjectRepository projectRepository, ActiveRepository activeRepository, AppUserDetailsService appUserDetailsService, TimingRepository timingRepository, UserRepository userRepository, ScheduledRepository scheduledRepository) {
        this.repository = repository;
        this.projectRepository = projectRepository;
        this.activeRepository = activeRepository;
        this.appUserDetailsService = appUserDetailsService;
        this.timingRepository = timingRepository;
        this.userRepository = userRepository;
        this.scheduledRepository = scheduledRepository;
    }

    public List<TaskDto> getFromProjectId(int projectId) {
        List<TaskDto> tasks = repository.findAllByProjectEntity_Id(projectId).stream().map(TaskEntity::toDto).toList();

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
        task.setProjectEntity(project);
        task.setCreatorEntity(appUserDetailsService.getUserEntity());
        task.setCompleted(isCompleted);
        task.setTaskOrder(repository.findMaxOrder() + 1);
        task.setDeadline(deadline);
        task.setEstimatedTime(estimatedTime);
        task.setDescription(description);

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
        if(activeRepository.existsByUserEntity_IdAndTaskEntity_Id(user.getId(), taskId)) {
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
        if(!activeRepository.existsByUserEntity_IdAndTaskEntity_Id(user.getId(), taskId)) {
            throw new ConflictException("User is not timed on this task");
        }

        TaskEntity task = repository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));

        ActiveEntity active = activeRepository.findByUserEntity_IdAndTaskEntity_Id(user.getId(), taskId).orElseThrow(() -> new NotFoundException("Task not found"));

        TimingEntity timeTable = new TimingEntity();
        timeTable.setTaskEntity(task);
        timeTable.setUserEntity(user);
        timeTable.setTime(Duration.between(active.getStamp(), LocalDateTime.now()).getSeconds());

        timingRepository.save(timeTable);

        activeRepository.delete(active);
    }

    public void startTimeUser(int taskId, int userId) {
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if(activeRepository.existsByUserEntity_IdAndTaskEntity_Id(user.getId(), taskId)) {
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
        if(!activeRepository.existsByUserEntity_IdAndTaskEntity_Id(user.getId(), taskId)) {
            throw new ConflictException("User is not timed on this task");
        }

        TaskEntity task = repository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));

        ActiveEntity active = activeRepository.findByUserEntity_IdAndTaskEntity_Id(user.getId(), taskId).orElseThrow(() -> new NotFoundException("Task not found"));

        TimingEntity timeTable = new TimingEntity();
        timeTable.setTaskEntity(task);
        timeTable.setUserEntity(user);
        timeTable.setTime(Duration.between(active.getStamp(), LocalDateTime.now()).getSeconds());

        timingRepository.save(timeTable);

        activeRepository.delete(active);
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
}
