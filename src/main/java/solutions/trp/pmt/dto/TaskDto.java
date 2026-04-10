package solutions.trp.pmt.dto;

import jakarta.persistence.*;
import solutions.trp.pmt.datasource.projects.ProjectEntity;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public class TaskDto {
    private int id;
    private String title;
    private int projectId;
    private Boolean isCompleted;
    private int taskOrder;
    private LocalDateTime deadline;
    private int estimatedTime;
    private int creatorId;
    private String description;
    private List<UserDto> actives;
    private List<UserDto> scheduled;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
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

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public int getEstimatedTime() {
        return estimatedTime;
    }

    public void setEstimatedTime(int estimatedTime) {
        this.estimatedTime = estimatedTime;
    }

    public int getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(int creatorId) {
        this.creatorId = creatorId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<UserDto> getActives() {
        return actives;
    }

    public void setActives(List<UserDto> actives) {
        this.actives = actives;
    }

    public List<UserDto> getScheduled() {
        return scheduled;
    }

    public void setScheduled(List<UserDto> scheduled) {
        this.scheduled = scheduled;
    }
}
