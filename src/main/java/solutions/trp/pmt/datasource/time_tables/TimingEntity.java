package solutions.trp.pmt.datasource.time_tables;

import jakarta.persistence.*;
import solutions.trp.pmt.datasource.tasks.TaskEntity;
import solutions.trp.pmt.datasource.users.UserEntity;
import solutions.trp.pmt.dto.TimeDto;
import solutions.trp.pmt.dto.UserDto;

import java.sql.Timestamp;
import java.time.ZoneOffset;

@Entity(name = "timing")
public class TimingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private TaskEntity taskEntity;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity userEntity;

    @Column(name = "start_time")
    private Timestamp startTime;

    @Column(name = "end_time")
    private Timestamp endTime;

    public int getId() {
        return id;
    }

    public TaskEntity getTaskEntity() {
        return taskEntity;
    }

    public void setTaskEntity(TaskEntity taskEntity) {
        this.taskEntity = taskEntity;
    }

    public UserEntity getUserEntity() {
        return userEntity;
    }

    public void setUserEntity(UserEntity userEntity) {
        this.userEntity = userEntity;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }

    public TimeDto toDto() {
        TimeDto dto = new TimeDto();
        dto.setId(getId());
        dto.setTaskId(taskEntity.getId());
        dto.setUserId(userEntity.getId());
        dto.setStartTime(startTime.toInstant().atOffset(ZoneOffset.UTC));
        dto.setEndTime(endTime.toInstant().atOffset(ZoneOffset.UTC));

        return dto;
    }
}
