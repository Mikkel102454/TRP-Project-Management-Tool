package solutions.trp.pmt.datasource.time_tables;

import jakarta.persistence.*;
import solutions.trp.pmt.datasource.tasks.TaskEntity;
import solutions.trp.pmt.datasource.users.UserEntity;

import java.time.LocalDateTime;

@Entity(name = "time_tables")
public class TimeTableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private TaskEntity taskEntity;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity userEntity;

    @Column(unique = false, nullable = false, name = "start_stamp")
    private LocalDateTime startStamp;

    @Column(unique = false, nullable = false, name = "end_stamp")
    private LocalDateTime endStamp;

    public int getId() {
        return id;
    }

    public LocalDateTime getStartStamp() {
        return startStamp;
    }

    public void setStartStamp(LocalDateTime startStamp) {
        this.startStamp = startStamp;
    }

    public TaskEntity getTaskEntity() {
        return taskEntity;
    }

    public void setTaskEntity(TaskEntity taskEntity) {
        this.taskEntity = taskEntity;
    }

    public LocalDateTime getEndStamp() {
        return endStamp;
    }

    public void setEndStamp(LocalDateTime endStamp) {
        this.endStamp = endStamp;
    }
}
