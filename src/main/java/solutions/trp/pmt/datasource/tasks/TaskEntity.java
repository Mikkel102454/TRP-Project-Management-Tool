package solutions.trp.pmt.datasource.tasks;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import solutions.trp.pmt.datasource.projects.ProjectEntity;
import solutions.trp.pmt.datasource.users.UserEntity;
import solutions.trp.pmt.dto.TaskDto;

import java.sql.Timestamp;

@Entity(name = "task")
public class TaskEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, name = "title")
    private String title;

    @ManyToOne(optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ProjectEntity projectEntity;

    @ManyToOne(optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private UserEntity creatorEntity;

    @Column(nullable = false, name = "is_completed")
    private Boolean isCompleted;

    @Column(unique = true, nullable = false, name = "task_order")
    private int taskOrder;

    @Column(name = "deadline")
    private Timestamp deadline;

    @Column(name = "estimated_time")
    private int estimatedTime;

    @Column(nullable = false, name = "description")
    private String description;

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public TaskDto toDto() {
        TaskDto dto = new TaskDto();
        dto.setId(id);
        dto.setTitle(title);
        dto.setProjectId(projectEntity.getId());
        dto.setCompleted(isCompleted);
        dto.setTaskOrder(taskOrder);
        dto.setEstimatedTime(estimatedTime);
        dto.setTaskOrder(taskOrder);
        dto.setCreatorId(creatorEntity.getId());
        dto.setDescription(description);
        return dto;
    }
}
