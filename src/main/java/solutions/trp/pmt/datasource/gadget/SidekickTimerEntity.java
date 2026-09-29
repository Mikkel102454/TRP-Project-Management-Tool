package solutions.trp.pmt.datasource.gadget;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sidekick_timer")
public class SidekickTimerEntity {
    @Id
    private int userId;
    @Column(nullable = false, length = 255)
    private String taskRef;
    @Column(length = 64)
    private String activeKey;
    @Column(nullable = false)
    private Instant anchorTime;
    @Column(nullable = false)
    private long valueSeconds;
    @Column(nullable = false)
    private boolean paused;

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getTaskRef() { return taskRef; }
    public void setTaskRef(String taskRef) { this.taskRef = taskRef; }
    public String getActiveKey() { return activeKey; }
    public void setActiveKey(String activeKey) { this.activeKey = activeKey; }
    public Instant getAnchorTime() { return anchorTime; }
    public void setAnchorTime(Instant anchorTime) { this.anchorTime = anchorTime; }
    public long getValueSeconds() { return valueSeconds; }
    public void setValueSeconds(long valueSeconds) { this.valueSeconds = valueSeconds; }
    public boolean isPaused() { return paused; }
    public void setPaused(boolean paused) { this.paused = paused; }
}
