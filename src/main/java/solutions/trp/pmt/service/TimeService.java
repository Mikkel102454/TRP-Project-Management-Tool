package solutions.trp.pmt.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import solutions.trp.pmt.controller.api.execption.ConflictException;
import solutions.trp.pmt.controller.api.execption.NotFoundException;
import solutions.trp.pmt.controller.api.execption.UnauthorizedException;
import solutions.trp.pmt.datasource.actives.ActiveEntity;
import solutions.trp.pmt.datasource.actives.ActiveRepository;
import solutions.trp.pmt.datasource.tasks.TaskEntity;
import solutions.trp.pmt.datasource.tasks.TaskRepository;
import solutions.trp.pmt.datasource.time_tables.TimingEntity;
import solutions.trp.pmt.datasource.time_tables.TimingRepository;
import solutions.trp.pmt.datasource.users.UserEntity;
import solutions.trp.pmt.datasource.users.UserRepository;
import solutions.trp.pmt.datasource.integration.IntegrationActiveEntity;
import solutions.trp.pmt.datasource.integration.IntegrationActiveRepository;
import solutions.trp.pmt.datasource.integration.IntegrationTimeEntryEntity;
import solutions.trp.pmt.datasource.integration.IntegrationTimeEntryRepository;
import solutions.trp.pmt.dto.TimeDto;
import solutions.trp.pmt.dto.TimeValidationDto;
import solutions.trp.pmt.service.integration.IntegrationBindingService;
import solutions.trp.pmt.service.integration.IntegrationTimeCoordinator;

import java.sql.Timestamp;
import java.time.*;
import java.util.*;

@Service
public class TimeService {
    private final TimingRepository timingRepository;
    private final ActiveRepository activeRepository;
    private final AppUserDetailsService appUserDetailsService;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final IntegrationActiveRepository integrationActiveRepository;
    private final IntegrationTimeEntryRepository integrationTimeEntryRepository;
    private final IntegrationTimeCoordinator integrationTimeCoordinator;
    private final IntegrationBindingService integrationBindingService;

    public TimeService(TimingRepository timingRepository, ActiveRepository activeRepository, AppUserDetailsService appUserDetailsService, TaskRepository taskRepository, UserRepository userRepository) {
        this(timingRepository, activeRepository, appUserDetailsService, taskRepository, userRepository, null, null, null, null);
    }

    @Autowired
    public TimeService(TimingRepository timingRepository, ActiveRepository activeRepository,
                       AppUserDetailsService appUserDetailsService, TaskRepository taskRepository,
                       UserRepository userRepository, IntegrationActiveRepository integrationActiveRepository,
                       IntegrationTimeEntryRepository integrationTimeEntryRepository,
                       IntegrationTimeCoordinator integrationTimeCoordinator,
                       IntegrationBindingService integrationBindingService) {
        this.timingRepository = timingRepository;
        this.activeRepository = activeRepository;
        this.appUserDetailsService = appUserDetailsService;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.integrationActiveRepository = integrationActiveRepository;
        this.integrationTimeEntryRepository = integrationTimeEntryRepository;
        this.integrationTimeCoordinator = integrationTimeCoordinator;
        this.integrationBindingService = integrationBindingService;
    }

    public int calculateTime(int taskId, List<TimingEntity> timings) {


        int spent = 0;
        for(ActiveEntity activeEntity : activeRepository.findAllByTaskEntity_Id(taskId)) {
            spent += (int) Duration.between(
                    activeEntity.getStamp().toInstant(),
                    Instant.now()
            ).getSeconds();
        }

        for(TimingEntity timingEntity : timings) {
            spent += (int) Duration.between(
                    timingEntity.getStartTime().toInstant(),
                    timingEntity.getEndTime().toInstant()
            ).getSeconds();
        }

        return spent;
    }

    public List<TimingEntity> getAllTime() {
        return timingRepository.findAll();
    }

    public List<TimingEntity> getAllTimeByUserId(int userId) {
        if(!appUserDetailsService.getUserEntity().isAdmin() && appUserDetailsService.getUserEntity().getId() != userId) {
            throw new UnauthorizedException("Unauthorized attempt to get time table");
        }
        return timingRepository.findAllByUserEntity_Id(userId);
    }

    public List<TimeDto> getAllTimeDtos() {
        List<TimeDto> entries = new ArrayList<>(timingRepository.findAll().stream().map(TimingEntity::toDto).toList());
        if (integrationTimeEntryRepository != null) entries.addAll(integrationTimeEntryRepository.findAll().stream().map(IntegrationTimeEntryEntity::toDto).toList());
        entries.sort(Comparator.comparing(TimeDto::getStartTime).reversed());
        return entries;
    }

    public List<TimeDto> getAllTimeDtosByUserId(int userId) {
        if(!appUserDetailsService.getUserEntity().isAdmin() && appUserDetailsService.getUserEntity().getId() != userId) {
            throw new UnauthorizedException("Unauthorized attempt to get time table");
        }
        List<TimeDto> entries = new ArrayList<>(timingRepository.findAllByUserEntity_Id(userId).stream().map(TimingEntity::toDto).toList());
        if (integrationTimeEntryRepository != null) entries.addAll(integrationTimeEntryRepository.findAllByUserEntity_Id(userId).stream().map(IntegrationTimeEntryEntity::toDto).toList());
        entries.sort(Comparator.comparing(TimeDto::getStartTime).reversed());
        return entries;
    }

    public void deleteTimeEntry(int id) {
        if (id < 0) throw new ConflictException("Remotely registered time entries are read-only");
        timingRepository.deleteById(id);
    }

    public void updateTimeEntry(int id, OffsetDateTime startTime, OffsetDateTime endTime) {
        if (id < 0) {
            updateIntegrationTimeEntry(-(long) id, startTime, endTime);
            return;
        }
        TimingEntity timingEntity = timingRepository.findById(id).orElseThrow(() -> new NotFoundException("Could not find time entry"));
        if(!appUserDetailsService.getUserEntity().isAdmin() && appUserDetailsService.getUserEntity().getId() != timingEntity.getUserEntity().getId()) {
            throw new UnauthorizedException("Unauthorized attempt to get time table");
        }

        if(timingEntity.getTaskEntity().getStatus() == TaskEntity.TaskStatus.CLOSED){
            throw new UnauthorizedException("Unauthorized attempt to update time on closed task");
        }

        timingEntity.setStartTime(Timestamp.from(startTime.toInstant()));
        timingEntity.setEndTime(Timestamp.from(endTime.toInstant()));
        timingEntity.setAttention(false);

        timingRepository.save(timingEntity);
        clearForcedClockedOutIfResolved(timingEntity.getUserEntity());
    }

    private void updateIntegrationTimeEntry(long id, OffsetDateTime startTime, OffsetDateTime endTime) {
        IntegrationTimeEntryEntity entry = integrationTimeEntryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Could not find PM time entry"));
        UserEntity currentUser = appUserDetailsService.getUserEntity();
        if (!currentUser.isAdmin() && currentUser.getId() != entry.getUserEntity().getId()) {
            throw new UnauthorizedException("Unauthorized attempt to update time table");
        }
        integrationTimeCoordinator.update(entry, startTime.toInstant(), endTime.toInstant());
        clearForcedClockedOutIfResolved(entry.getUserEntity());
    }

    private void clearForcedClockedOutIfResolved(UserEntity user) {
        if (timingRepository.findAllByUserEntity_Id(user.getId()).stream().anyMatch(TimingEntity::isAttention)) {
            return;
        }
        if (integrationTimeEntryRepository != null
                && integrationTimeEntryRepository.findAllByUserEntity_Id(user.getId()).stream()
                .anyMatch(IntegrationTimeEntryEntity::isAttention)) {
            return;
        }
        user.setForcedClockedOut(false);
        userRepository.save(user);
    }

    public TimeValidationDto getValidatedTime(Integer userId) {
        return null;
    }

    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void validateTime(){
        List<ActiveEntity> activeEntities = activeRepository.findAll();
        for(ActiveEntity activeEntity : activeEntities) {
            if(Duration.between(activeEntity.getStamp().toInstant(), Instant.now()).getSeconds() > Duration.ofHours(12).getSeconds()) {
                stopTimeUser(activeEntity.getTaskEntity().getId(), activeEntity.getUserEntity().getId());
            }
        }
        if (integrationActiveRepository != null) {
            for (IntegrationActiveEntity active : integrationActiveRepository.findAll()) {
                if (Duration.between(active.getStartTime(), Instant.now()).compareTo(Duration.ofHours(12)) > 0) {
                    integrationTimeCoordinator.stop(active.getUserEntity(), active.getTaskRef(), true);
                    active.getUserEntity().setForcedClockedOut(true);
                    userRepository.save(active.getUserEntity());
                }
            }
        }
    }

    public void stopIntegrationTimersForProject(long bindingId) {
        if (integrationActiveRepository == null) return;
        for (IntegrationActiveEntity active : integrationActiveRepository.findAllByProjectBinding_Id(bindingId)) {
            integrationTimeCoordinator.stop(active.getUserEntity(), active.getTaskRef(), false);
        }
    }

    public void startTimeUser(Integer taskId, String taskRef) {
        startTime(resolveUser(null), taskId, taskRef);
    }

    public void stopTimeUser(Integer taskId, String taskRef) {
        stopTime(resolveUser(null), taskId, taskRef, false);
    }

    public void startTimeUser(Integer taskId, String taskRef, int userId) {
        startTime(resolveUser(userId), taskId, taskRef);
    }

    public void stopTimeUser(Integer taskId, String taskRef, int userId) {
        stopTime(resolveUser(userId), taskId, taskRef, true);
    }

    private void startTime(UserEntity user, Integer taskId, String taskRef) {
        if (taskRef != null && !taskRef.isBlank()) {
            if (taskRef.startsWith("local:")) startLocal(parseLocalTaskRef(taskRef), user);
            else integrationTimeCoordinator.start(user, taskRef);
            return;
        }
        if (taskId == null) throw new solutions.trp.pmt.controller.api.execption.BadRequestException("taskId or taskRef is required");
        startLocal(taskId, user);
    }

    private void stopTime(UserEntity user, Integer taskId, String taskRef, boolean attention) {
        if (taskRef != null && !taskRef.isBlank()) {
            if (taskRef.startsWith("local:")) stopLocal(parseLocalTaskRef(taskRef), user, attention);
            else integrationTimeCoordinator.stop(user, taskRef, attention);
            if (attention) { user.setForcedClockedOut(true); userRepository.save(user); }
            return;
        }
        if (taskId == null) throw new solutions.trp.pmt.controller.api.execption.BadRequestException("taskId or taskRef is required");
        stopLocal(taskId, user, attention);
    }

    private UserEntity resolveUser(Integer userId) {
        return userId == null ? appUserDetailsService.getUserEntity()
                : userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
    }

    private int parseLocalTaskRef(String taskRef) {
        try { return Integer.parseInt(taskRef.substring("local:".length())); }
        catch (RuntimeException exception) { throw new NotFoundException("Task not found"); }
    }

    public void startTimeUser(int taskId) {
        startLocal(taskId, appUserDetailsService.getUserEntity());
    }

    private void startLocal(int taskId, UserEntity user) {

        if(activeRepository.existsByUserEntity_IdAndTaskEntity_Id(user.getId(), taskId)) {
            throw new ConflictException("User is already timed on this task");
        }

        TaskEntity task = taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));
        if (integrationBindingService != null) integrationBindingService.requireLocalProject(task.getProjectEntity().getId());

        if(task.getStatus() == TaskEntity.TaskStatus.CLOSED){
            throw new ConflictException("Task is already closed");
        }

        ActiveEntity active = new ActiveEntity();
        active.setTaskEntity(task);
        active.setUserEntity(user);
        active.setStamp(Timestamp.from(Instant.now()));

        activeRepository.save(active);
    }

    public void stopTimeUser(int taskId) {
        stopLocal(taskId, appUserDetailsService.getUserEntity(), false);
    }

    private void stopLocal(int taskId, UserEntity user, boolean attention) {
        if(!activeRepository.existsByUserEntity_IdAndTaskEntity_Id(user.getId(), taskId)) {
            throw new ConflictException("User is not timed on this task");
        }

        TaskEntity task = taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));
        if (integrationBindingService != null) integrationBindingService.requireLocalProject(task.getProjectEntity().getId());

        ActiveEntity active = activeRepository.findByUserEntity_IdAndTaskEntity_Id(user.getId(), taskId).orElseThrow(() -> new NotFoundException("Task not found"));

        TimingEntity timeTable = new TimingEntity();
        timeTable.setTaskEntity(task);
        timeTable.setUserEntity(user);
        timeTable.setStartTime(active.getStamp());
        timeTable.setEndTime(Timestamp.from(Instant.now()));
        timeTable.setAttention(attention);

        if (attention) {
            user.setForcedClockedOut(true);
            userRepository.save(user);
        }

        timingRepository.save(timeTable);

        activeRepository.delete(active);
    }

    public void startTimeUser(int taskId, int userId) {
        startLocal(taskId, resolveUser(userId));
    }

    public void stopTimeUser(int taskId, int userId) {
        stopLocal(taskId, resolveUser(userId), true);
    }
}
