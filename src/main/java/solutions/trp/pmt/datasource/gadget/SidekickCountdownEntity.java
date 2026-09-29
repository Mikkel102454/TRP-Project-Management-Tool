package solutions.trp.pmt.datasource.gadget;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sidekick_countdown")
public class SidekickCountdownEntity {
    @Id
    private int userId;
    @Column(nullable = false)
    private Instant anchorTime;
    @Column(nullable = false)
    private Instant endTime;

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public Instant getAnchorTime() { return anchorTime; }
    public void setAnchorTime(Instant anchorTime) { this.anchorTime = anchorTime; }
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
}
