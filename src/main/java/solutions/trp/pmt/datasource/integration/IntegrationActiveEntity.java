package solutions.trp.pmt.datasource.integration;

import jakarta.persistence.*;
import solutions.trp.pmt.datasource.users.UserEntity;

import java.time.Instant;

@Entity
@Table(name = "integration_active_timer",
        uniqueConstraints = @UniqueConstraint(name = "uk_integration_active_user_task", columnNames = {"user_id", "task_ref"}),
        indexes = @Index(name = "idx_integration_active_task", columnList = "task_ref"))
public class IntegrationActiveEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(nullable = false, length = 64)
    private String provider;
    @ManyToOne(optional = false) @JoinColumn(name = "binding_id", nullable = false)
    private ProjectBindingEntity projectBinding;
    @ManyToOne(optional = false) @JoinColumn(name = "snapshot_id", nullable = false)
    private WorkItemSnapshotEntity snapshot;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id", nullable = false)
    private UserEntity userEntity;
    @Column(nullable = false, name = "task_ref", length = 255)
    private String taskRef;
    @Column(nullable = false, name = "remote_registration_id", length = 128)
    private String remoteRegistrationId;
    @Column(nullable = false, name = "start_time")
    private Instant startTime;

    public long getId() { return id; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public ProjectBindingEntity getProjectBinding() { return projectBinding; }
    public void setProjectBinding(ProjectBindingEntity projectBinding) { this.projectBinding = projectBinding; }
    public WorkItemSnapshotEntity getSnapshot() { return snapshot; }
    public void setSnapshot(WorkItemSnapshotEntity snapshot) { this.snapshot = snapshot; }
    public UserEntity getUserEntity() { return userEntity; }
    public void setUserEntity(UserEntity userEntity) { this.userEntity = userEntity; }
    public String getTaskRef() { return taskRef; }
    public void setTaskRef(String taskRef) { this.taskRef = taskRef; }
    public String getRemoteRegistrationId() { return remoteRegistrationId; }
    public void setRemoteRegistrationId(String remoteRegistrationId) { this.remoteRegistrationId = remoteRegistrationId; }
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
}
