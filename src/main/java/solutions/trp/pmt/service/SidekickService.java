package solutions.trp.pmt.service;

import org.springframework.stereotype.Service;
import solutions.trp.pmt.datasource.actives.ActiveRepository;
import solutions.trp.pmt.datasource.integration.IntegrationActiveRepository;
import solutions.trp.pmt.datasource.users.UserEntity;
import solutions.trp.pmt.dto.*;

import java.util.*;
import java.time.Instant;
import java.time.Duration;
import solutions.trp.pmt.datasource.gadget.SidekickTimerEntity;
import solutions.trp.pmt.datasource.gadget.SidekickTimerRepository;
import solutions.trp.pmt.controller.api.execption.ConflictException;
import java.util.stream.Stream;

@Service
public class SidekickService {
    private final UserService userService;
    private final ProjectService projectService;
    private final TimeService timeService;
    private final ActiveRepository activeRepository;
    private final IntegrationActiveRepository integrationActiveRepository;
    private final SidekickTimerRepository timerRepository;
    private final SidekickCountdownService countdownService;

    public SidekickService(UserService userService, ProjectService projectService, TimeService timeService,
                           ActiveRepository activeRepository, IntegrationActiveRepository integrationActiveRepository,
                           SidekickTimerRepository timerRepository, SidekickCountdownService countdownService) {
        this.userService = userService;
        this.projectService = projectService;
        this.timeService = timeService;
        this.activeRepository = activeRepository;
        this.integrationActiveRepository = integrationActiveRepository;
        this.timerRepository = timerRepository;
        this.countdownService = countdownService;
    }

    public record Overlay(String type, String id, String direction, String state, String anchorTime,
                          long valueSeconds, String format, String position, int fontSize, String color,
                          String backgroundColor) {}

    public record Info(boolean active, boolean forcedClockedOut, Instant startTime, long valueSeconds, boolean paused, Overlay countdown) {
        public Info(boolean active, boolean forcedClockedOut, Instant startTime, long valueSeconds, boolean paused) {
            this(active, forcedClockedOut, startTime, valueSeconds, paused, null);
        }
        public Info(boolean active, boolean forcedClockedOut, Instant startTime) {
            this(active, forcedClockedOut, startTime, 0, false);
        }
        public String video() {
            if (countdown != null) return "shush.mp4";
            return forcedClockedOut ? "mad.mp4" : active ? "happy.mp4" : "neutral.mp4";
        }

        public List<Overlay> overlays() {
            if (countdown != null) return List.of(countdown);
            if (!active && !paused) return List.of();
            return List.of(new Overlay("timer", paused ? "pause-timer" : "work-timer", "up", "running", startTime.toString(),
                    paused ? 0 : valueSeconds, "hh:mm:ss", "center", 24, "#FFFFFF", "#00000080"));
        }
    }
    public record Display(List<ProjectDto> projects, List<TaskDto> recentTasks) {}

    private record ActiveSession(String key, String taskRef, Instant start) {}

    private ActiveSession currentSession(int userId) {
        var local = activeRepository.findFirstByUserEntity_IdOrderByStampDescIdDesc(userId)
                .map(active -> new ActiveSession("local:" + active.getId(), "local:" + active.getTaskEntity().getId(),
                        active.getStamp().toInstant()));
        var remote = integrationActiveRepository.findFirstByUserEntity_IdOrderByStartTimeDescIdDesc(userId)
                .map(active -> new ActiveSession("remote:" + active.getId(), active.getTaskRef(), active.getStartTime()));
        return Stream.concat(local.stream(), remote.stream()).max(Comparator.comparing(ActiveSession::start)).orElse(null);
    }

    private boolean pausedSession(SidekickTimerEntity saved, ActiveSession active) {
        // A task started elsewhere after pausing takes precedence over the paused selection.
        return saved != null && saved.isPaused()
                && (active == null || !active.start().isAfter(saved.getAnchorTime()));
    }

    private long carriedSeconds(SidekickTimerEntity saved, ActiveSession active) {
        return saved != null && !saved.isPaused() && active != null
                && active.key().equals(saved.getActiveKey())
                ? saved.getValueSeconds() : 0;
    }

    public synchronized Info info() {
        UserEntity user = userService.getCurrentUser();
        ActiveSession active = currentSession(user.getId());
        SidekickTimerEntity saved = timerRepository.findById(user.getId()).orElse(null);
        Overlay countdown = active == null || user.isForcedClockedOut() ? countdownService.overlay(user.getId()) : null;
        if (pausedSession(saved, active)) {
            return new Info(false, user.isForcedClockedOut(), saved.getAnchorTime(), saved.getValueSeconds(), true, countdown);
        }
        return new Info(active != null, user.isForcedClockedOut(), active == null ? null : active.start(),
                carriedSeconds(saved, active), false, countdown);
    }

    public synchronized void button1Pressed() {
        int userId = userService.getCurrentUser().getId();
        ActiveSession active = currentSession(userId);
        SidekickTimerEntity saved = timerRepository.findById(userId).orElse(null);
        if (pausedSession(saved, active)) {
            timeService.startTimeUser(null, saved.getTaskRef());
            ActiveSession resumed = currentSession(userId);
            if (resumed == null || !resumed.taskRef().equals(saved.getTaskRef())) {
                throw new ConflictException("Resumed task timer could not be found");
            }
            saved.setPaused(false);
            saved.setActiveKey(resumed.key());
            saved.setAnchorTime(resumed.start());
            timerRepository.saveAndFlush(saved);
        } else if (active != null) {
            Instant pausedAt = Instant.now();
            long elapsed = carriedSeconds(saved, active) + Math.max(0, Duration.between(active.start(), pausedAt).getSeconds());
            // Use the normal stop path so paused time is excluded from local and PM registrations.
            timeService.stopTimeUser(null, active.taskRef());
            SidekickTimerEntity paused = new SidekickTimerEntity();
            paused.setUserId(userId);
            paused.setTaskRef(active.taskRef());
            paused.setAnchorTime(pausedAt);
            paused.setValueSeconds(elapsed);
            paused.setPaused(true);
            timerRepository.saveAndFlush(paused);
        }
    }

    public synchronized void button1Held() {
        int userId = userService.getCurrentUser().getId();
        ActiveSession active = currentSession(userId);
        SidekickTimerEntity saved = timerRepository.findById(userId).orElse(null);
        if (!pausedSession(saved, active) && active != null) {
            timeService.stopTimeUser(null, active.taskRef());
        }
        // A paused timer has already been clocked out; holding discards its resume state.
        if (saved != null) timerRepository.delete(saved);
    }

    public synchronized void button2Pressed() {
        UserEntity user = userService.getCurrentUser();
        if (user.isForcedClockedOut() || currentSession(user.getId()) == null) {
            countdownService.addTime(user.getId());
            timerRepository.findById(user.getId()).filter(SidekickTimerEntity::isPaused)
                    .ifPresent(timerRepository::delete);
        }
    }

    public synchronized void button2Held() {
        countdownService.clear(userService.getCurrentUser().getId());
    }

    public Display display() {
        int userId = userService.getCurrentUser().getId();
        List<ProjectDto> assignedProjects = new ArrayList<>();
        Map<String, TaskDto> availableTasks = new HashMap<>();
        for (var project : projectService.getAll()) {
            if (project.isArchived() || !projectService.isVisibleToUser(project, userId)) continue;
            ProjectDto dto = projectService.toDto(project);
            List<TaskDto> unfinished = dto.getTasks().stream().filter(this::unfinished).toList();
            unfinished.forEach(task -> availableTasks.put(taskRef(task), task));
            List<TaskDto> assigned = unfinished.stream()
                    .filter(task -> hasUser(task.getScheduled(), userId)).toList();
            if (assigned.isEmpty()) continue;
            dto.setTasks(assigned);
            dto.setScheduled(assigned.stream().flatMap(task -> task.getScheduled().stream()).distinct().toList());
            dto.setIsWorkedOn(assigned.stream().anyMatch(task -> hasUser(task.getActives(), userId)));
            assignedProjects.add(dto);
        }
        // TimeService merges local and connected PM history in newest-first order.
        Instant recentCutoff = Instant.now().minus(Duration.ofDays(7));
        List<TaskDto> recent = timeService.getAllTimeDtosByUserId(userId).stream()
                .filter(time -> time.getStartTime() != null && !time.getStartTime().toInstant().isBefore(recentCutoff))
                .map(time -> time.getTaskRef() != null ? time.getTaskRef() : "local:" + time.getTaskId())
                .distinct().map(availableTasks::get).filter(Objects::nonNull).limit(3).toList();
        return new Display(assignedProjects, recent);
    }

    private boolean unfinished(TaskDto task) {
        return !Boolean.TRUE.equals(task.getCompleted())
                && !"FINISHED".equals(task.getStatus()) && !"CLOSED".equals(task.getStatus());
    }

    private boolean hasUser(List<UserDto> users, int userId) {
        return users != null && users.stream().anyMatch(user -> user.getId() == userId);
    }

    private String taskRef(TaskDto task) {
        return task.getTaskRef() != null ? task.getTaskRef() : "local:" + task.getId();
    }
}
