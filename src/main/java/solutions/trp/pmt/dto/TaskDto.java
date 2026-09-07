package solutions.trp.pmt.dto;

import jakarta.persistence.*;
import solutions.trp.pmt.datasource.projects.ProjectEntity;
import solutions.trp.pmt.datasource.tasks.TaskEntity;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class TaskDto {
    private int id;
    private String title;
    private String status;
    private int projectId;
    private Boolean isCompleted;
    private int taskOrder;
    private LocalDateTime deadline;
    private int estimatedTime;
    private UserDto creator;
    private String description;
    private List<UserDto> actives;
    private List<UserDto> scheduled;
    private int spent;
    private String taskRef;
    private String source;
    private String provider;
    private String externalId;
    private String externalStatus;
    private boolean readOnly;
    private String module;
    private String type;
    private String developmentNotes;
    private Map<String, String> metadata;
    private boolean workedOn;
    private int unmappedScheduledCount;
    private int unmappedActiveCount;
    private List<Integer> managedActiveUserIds = List.of();

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

    public String getStatus() {
        return status;
    }

    public void setStatus(TaskEntity.TaskStatus status) {
        this.status = status == null ? null : status.name();
    }

    public void setStatus(String status) { this.status = status; }

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

    public UserDto getCreator() {
        return creator;
    }

    public void setCreator(UserDto creator) {
        this.creator = creator;
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

    public int getSpent() {
        return spent;
    }

    public void setSpent(int spent) {
        this.spent = spent;
    }

    public String getTaskRef() { return taskRef; }
    public void setTaskRef(String taskRef) { this.taskRef = taskRef; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getExternalStatus() { return externalStatus; }
    public void setExternalStatus(String externalStatus) { this.externalStatus = externalStatus; }
    public boolean isReadOnly() { return readOnly; }
    public void setReadOnly(boolean readOnly) { this.readOnly = readOnly; }
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDevelopmentNotes() { return developmentNotes; }
    public void setDevelopmentNotes(String developmentNotes) { this.developmentNotes = developmentNotes; }
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    public boolean isWorkedOn() { return workedOn; }
    public void setWorkedOn(boolean workedOn) { this.workedOn = workedOn; }
    public int getUnmappedScheduledCount() { return unmappedScheduledCount; }
    public void setUnmappedScheduledCount(int unmappedScheduledCount) { this.unmappedScheduledCount = unmappedScheduledCount; }
    public int getUnmappedActiveCount() { return unmappedActiveCount; }
    public void setUnmappedActiveCount(int unmappedActiveCount) { this.unmappedActiveCount = unmappedActiveCount; }
    public List<Integer> getManagedActiveUserIds() { return managedActiveUserIds; }
    public void setManagedActiveUserIds(List<Integer> managedActiveUserIds) {
        this.managedActiveUserIds = managedActiveUserIds == null ? List.of() : managedActiveUserIds;
    }
}
