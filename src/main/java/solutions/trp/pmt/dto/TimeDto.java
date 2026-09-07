package solutions.trp.pmt.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public class TimeDto {
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    int id;
    Integer taskId;
    int userId;
    boolean attention;
    private String taskRef;
    private String source;
    private String provider;
    private boolean remotelyRegistered;
    private boolean readOnly;
    private Integer projectId;
    private String projectTitle;
    private String taskTitle;

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(OffsetDateTime startTime) {
        this.startTime = startTime;
    }

    public OffsetDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(OffsetDateTime endTime) {
        this.endTime = endTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public boolean isAttention() {
        return attention;
    }

    public void setAttention(boolean attention) {
        this.attention = attention;
    }

    public String getTaskRef() { return taskRef; }
    public void setTaskRef(String taskRef) { this.taskRef = taskRef; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public boolean isRemotelyRegistered() { return remotelyRegistered; }
    public void setRemotelyRegistered(boolean remotelyRegistered) { this.remotelyRegistered = remotelyRegistered; }
    public boolean isReadOnly() { return readOnly; }
    public void setReadOnly(boolean readOnly) { this.readOnly = readOnly; }
    public Integer getProjectId() { return projectId; }
    public void setProjectId(Integer projectId) { this.projectId = projectId; }
    public String getProjectTitle() { return projectTitle; }
    public void setProjectTitle(String projectTitle) { this.projectTitle = projectTitle; }
    public String getTaskTitle() { return taskTitle; }
    public void setTaskTitle(String taskTitle) { this.taskTitle = taskTitle; }
}
