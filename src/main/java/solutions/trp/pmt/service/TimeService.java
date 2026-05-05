package solutions.trp.pmt.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
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
import solutions.trp.pmt.dto.TimeDto;
import solutions.trp.pmt.dto.TimeValidationDto;

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

    public TimeService(TimingRepository timingRepository, ActiveRepository activeRepository, AppUserDetailsService appUserDetailsService, TaskRepository taskRepository, UserRepository userRepository) {
        this.timingRepository = timingRepository;
        this.activeRepository = activeRepository;
        this.appUserDetailsService = appUserDetailsService;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    public int calculateTime(Integer taskId, Integer projectId, List<Integer> userIds) {


        int spent = 0;
        for(ActiveEntity activeEntity : activeRepository.findAllByTaskEntity_Id(taskId)) {
            spent += (int) Duration.between(
                    activeEntity.getStamp().toInstant(),
                    Instant.now()
            ).getSeconds();
        }

        if(userIds == null || userIds.isEmpty()) {
            return timingRepository.sumTime(taskId, projectId) + spent;
        }
        return timingRepository.sumTime(taskId, projectId, userIds) + spent;
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

    public void deleteTimeEntry(int id) {
        timingRepository.deleteById(id);
    }

    public void updateTimeEntry(int id, OffsetDateTime startTime, OffsetDateTime endTime) {
        TimingEntity timingEntity = timingRepository.findById(id).orElseThrow(() -> new NotFoundException("Could not find time entry"));
        if(!appUserDetailsService.getUserEntity().isAdmin() && appUserDetailsService.getUserEntity().getId() != timingEntity.getUserEntity().getId()) {
            throw new UnauthorizedException("Unauthorized attempt to get time table");
        }

        timingEntity.setStartTime(Timestamp.from(startTime.toInstant()));
        timingEntity.setEndTime(Timestamp.from(endTime.toInstant()));
        timingEntity.setAttention(false);

        timingRepository.save(timingEntity);


        List<TimingEntity> allEntries = timingRepository.findAllByUserEntity_Id(id);
        for(TimingEntity entry : allEntries) {
            if(entry.isAttention()) return;
        }

        UserEntity user = appUserDetailsService.getUserEntity();
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
    }

    public void startTimeUser(int taskId) {
        UserEntity user = appUserDetailsService.getUserEntity();
        if(activeRepository.existsByUserEntity_IdAndTaskEntity_Id(user.getId(), taskId)) {
            throw new ConflictException("User is already timed on this task");
        }

        TaskEntity task = taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));

        ActiveEntity active = new ActiveEntity();
        active.setTaskEntity(task);
        active.setUserEntity(user);
        active.setStamp(Timestamp.from(Instant.now()));

        activeRepository.save(active);
    }

    public void stopTimeUser(int taskId) {
        UserEntity user = appUserDetailsService.getUserEntity();
        if(!activeRepository.existsByUserEntity_IdAndTaskEntity_Id(user.getId(), taskId)) {
            throw new ConflictException("User is not timed on this task");
        }

        TaskEntity task = taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));

        ActiveEntity active = activeRepository.findByUserEntity_IdAndTaskEntity_Id(user.getId(), taskId).orElseThrow(() -> new NotFoundException("Task not found"));

        TimingEntity timeTable = new TimingEntity();
        timeTable.setTaskEntity(task);
        timeTable.setUserEntity(user);
        timeTable.setStartTime(active.getStamp());
        timeTable.setEndTime(Timestamp.from(Instant.now()));
        timeTable.setAttention(false);

        timingRepository.save(timeTable);

        activeRepository.delete(active);
    }

    public void startTimeUser(int taskId, int userId) {
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if(activeRepository.existsByUserEntity_IdAndTaskEntity_Id(user.getId(), taskId)) {
            throw new ConflictException("User is already timed on this task");
        }

        TaskEntity task = taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));

        ActiveEntity active = new ActiveEntity();
        active.setTaskEntity(task);
        active.setUserEntity(user);
        active.setStamp(Timestamp.from(Instant.now()));

        activeRepository.save(active);
    }

    public void stopTimeUser(int taskId, int userId) {
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if(!activeRepository.existsByUserEntity_IdAndTaskEntity_Id(user.getId(), taskId)) {
            throw new ConflictException("User is not timed on this task");
        }

        TaskEntity task = taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));

        ActiveEntity active = activeRepository.findByUserEntity_IdAndTaskEntity_Id(user.getId(), taskId).orElseThrow(() -> new NotFoundException("Task not found"));

        TimingEntity timeTable = new TimingEntity();
        timeTable.setTaskEntity(task);
        timeTable.setUserEntity(user);
        timeTable.setStartTime(active.getStamp());
        timeTable.setEndTime(Timestamp.from(Instant.now()));
        timeTable.setAttention(true);

        user.setForcedClockedOut(true);
        userRepository.save(user);

        timingRepository.save(timeTable);

        activeRepository.delete(active);
    }
}
