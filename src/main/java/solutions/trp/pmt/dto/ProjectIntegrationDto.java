package solutions.trp.pmt.dto;

public class ProjectIntegrationDto {
    private String provider;
    private String release;
    private boolean available;
    private boolean tasksReadOnly;
    private String unavailableReason;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getRelease() { return release; }
    public void setRelease(String release) { this.release = release; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public boolean isTasksReadOnly() { return tasksReadOnly; }
    public void setTasksReadOnly(boolean tasksReadOnly) { this.tasksReadOnly = tasksReadOnly; }
    public String getUnavailableReason() { return unavailableReason; }
    public void setUnavailableReason(String unavailableReason) { this.unavailableReason = unavailableReason; }
}
