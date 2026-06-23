package solutions.trp.pmt.datasource.tasks;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import solutions.trp.pmt.datasource.actives.ActiveEntity;
import solutions.trp.pmt.datasource.projects.ProjectEntity;
import solutions.trp.pmt.datasource.scheduled.ScheduledEntity;
import solutions.trp.pmt.datasource.time_tables.TimingEntity;
import solutions.trp.pmt.datasource.users.UserEntity;
import solutions.trp.pmt.dto.TaskDto;
import solutions.trp.pmt.service.TimeService;

import java.sql.Timestamp;
import java.util.List;

@Entity(name = "task")
@Table(
        name = "task",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_project_task_order",
                        columnNames = {"project_id", "task_order"}
                )
        }
)
public class TaskEntity {
    public enum TaskStatus {
        TODO,
        AWAITING_CONFIRMATION,
        FINISHED,
        CLOSED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, name = "title")
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @ManyToOne(optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ProjectEntity projectEntity;

    @ManyToOne(optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private UserEntity creatorEntity;

    @Column(nullable = false, name = "is_completed")
    private Boolean isCompleted;

    @Column(unique = false, nullable = false, name = "task_order")
    private int taskOrder;

    @Column(name = "deadline")
    private Timestamp deadline;

    @Column(name = "estimated_time")
    private int estimatedTime;

    @Column(nullable = false, name = "description")
    private String description;

    @Column(nullable = false, name = "has_printed")
    private boolean hasPrinted;

    @OneToMany(mappedBy = "taskEntity")
    private List<TimingEntity> timings;

    @OneToMany(mappedBy = "taskEntity")
    private List<ActiveEntity> actives;

    @OneToMany(mappedBy = "taskEntity")
    private List<ScheduledEntity> scheduled;

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public ProjectEntity getProjectEntity() {
        return projectEntity;
    }

    public void setProjectEntity(ProjectEntity projectEntity) {
        this.projectEntity = projectEntity;
    }

    public UserEntity getCreatorEntity() {
        return creatorEntity;
    }

    public void setCreatorEntity(UserEntity creatorEntity) {
        this.creatorEntity = creatorEntity;
    }

    public Boolean getCompleted() {
        return isCompleted;
    }

    public void setCompleted(Boolean completed) {
        isCompleted = completed;
    }

    public int getTaskOrder() {
        return taskOrder;
    }

    public void setTaskOrder(int taskOrder) {
        this.taskOrder = taskOrder;
    }

    public Timestamp getDeadline() {
        return deadline;
    }

    public void setDeadline(Timestamp deadline) {
        this.deadline = deadline;
    }

    public int getEstimatedTime() {
        return estimatedTime;
    }

    public void setEstimatedTime(int estimatedTime) {
        this.estimatedTime = estimatedTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isHasPrinted() {
        return hasPrinted;
    }

    public void setHasPrinted(boolean hasPrinted) {
        this.hasPrinted = hasPrinted;
    }

    public List<TimingEntity> getTimings() {
        return timings;
    }

    public void setTimings(List<TimingEntity> timings) {
        this.timings = timings;
    }

    public List<ScheduledEntity> getScheduled() {
        return scheduled;
    }

    public void setScheduled(List<ScheduledEntity> scheduled) {
        this.scheduled = scheduled;
    }

    public List<ActiveEntity> getActives() {
        return actives;
    }

    public void setActives(List<ActiveEntity> actives) {
        this.actives = actives;
    }

    public TaskDto toDto(TimeService timeService) {
        TaskDto dto = new TaskDto();
        dto.setId(id);
        dto.setTitle(title);
        dto.setStatus(status);
        dto.setProjectId(projectEntity.getId());
        dto.setCompleted(isCompleted);
        dto.setTaskOrder(taskOrder);
        dto.setEstimatedTime(estimatedTime);
        dto.setTaskOrder(taskOrder);
        dto.setCreator(creatorEntity.toDto());
        dto.setDescription(description);
        dto.setDeadline(deadline != null ? deadline.toLocalDateTime() : null);
        dto.setSpent(timeService.calculateTime(id, timings));
        dto.setActives(
                actives.stream()
                        .map(ActiveEntity::getUserEntity)
                        .distinct()
                        .map(UserEntity::toDto)
                        .toList()
        );
        dto.setScheduled(scheduled.stream().map(ScheduledEntity::getUserEntity).map(UserEntity::toDto).toList());
        return dto;
    }
}
