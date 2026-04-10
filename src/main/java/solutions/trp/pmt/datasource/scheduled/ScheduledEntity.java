package solutions.trp.pmt.datasource.scheduled;

import jakarta.persistence.*;
import solutions.trp.pmt.datasource.tasks.TaskEntity;
import solutions.trp.pmt.datasource.users.UserEntity;

import java.time.LocalDateTime;

@Entity(name = "schedule")
public class ScheduledEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity userEntity;

    @ManyToOne(optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private TaskEntity taskEntity;

    public int getId() {
        return id;
    }

    public UserEntity getUserEntity() {
        return userEntity;
    }

    public void setUserEntity(UserEntity userEntity) {
        this.userEntity = userEntity;
    }

    public TaskEntity getTaskEntity() {
        return taskEntity;
    }

    public void setTaskEntity(TaskEntity taskEntity) {
        this.taskEntity = taskEntity;
    }
}
