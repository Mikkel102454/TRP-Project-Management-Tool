package solutions.trp.pmt.datasource.integration;

import jakarta.persistence.*;
import solutions.trp.pmt.datasource.users.UserEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "integration_operation", indexes = @Index(name = "idx_integration_operation_state", columnList = "state,updated_at"))
public class IntegrationOperationEntity {
    public enum Type { START, STOP, UPDATE }
    public enum State { PENDING_REMOTE, REMOTE_APPLIED, COMPLETED, COMPENSATED, RECONCILIATION_REQUIRED, FAILED }

    @Id
    @Column(length = 36)
    private String id;
    @Column(nullable = false, length = 64)
    private String provider;
    @Enumerated(EnumType.STRING) @Column(nullable = false, name = "operation_type", length = 16)
    private Type type;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private State state;
    @ManyToOne(optional = false) @JoinColumn(name = "binding_id", nullable = false)
    private ProjectBindingEntity projectBinding;
    @ManyToOne(optional = false) @JoinColumn(name = "snapshot_id", nullable = false)
    private WorkItemSnapshotEntity snapshot;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id", nullable = false)
    private UserEntity userEntity;
    @Column(nullable = false, name = "remote_account_id", length = 128)
    private String remoteAccountId;
    @Column(nullable = false, name = "task_ref", length = 255)
    private String taskRef;
    @Column(name = "remote_registration_id", length = 128)
    private String remoteRegistrationId;
    @Column(name = "timer_start")
    private Instant timerStart;
    @Column(name = "timer_end")
    private Instant timerEnd;
    @Column(name = "previous_timer_start")
    private Instant previousTimerStart;
    @Column(name = "previous_timer_end")
    private Instant previousTimerEnd;
    @Column(name = "last_error", length = 1000)
    private String lastError;
    @Column(nullable = false, name = "created_at")
    private Instant createdAt;
    @Column(nullable = false, name = "updated_at")
    private Instant updatedAt;

    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID().toString(); if (createdAt == null) createdAt = Instant.now(); updatedAt = Instant.now(); }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public String getId() { return id; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public State getState() { return state; }
    public void setState(State state) { this.state = state; }
    public ProjectBindingEntity getProjectBinding() { return projectBinding; }
    public void setProjectBinding(ProjectBindingEntity projectBinding) { this.projectBinding = projectBinding; }
    public WorkItemSnapshotEntity getSnapshot() { return snapshot; }
    public void setSnapshot(WorkItemSnapshotEntity snapshot) { this.snapshot = snapshot; }
    public UserEntity getUserEntity() { return userEntity; }
    public void setUserEntity(UserEntity userEntity) { this.userEntity = userEntity; }
    public String getRemoteAccountId() { return remoteAccountId; }
    public void setRemoteAccountId(String remoteAccountId) { this.remoteAccountId = remoteAccountId; }
    public String getTaskRef() { return taskRef; }
    public void setTaskRef(String taskRef) { this.taskRef = taskRef; }
    public String getRemoteRegistrationId() { return remoteRegistrationId; }
    public void setRemoteRegistrationId(String remoteRegistrationId) { this.remoteRegistrationId = remoteRegistrationId; }
    public Instant getTimerStart() { return timerStart; }
    public void setTimerStart(Instant timerStart) { this.timerStart = timerStart; }
    public Instant getTimerEnd() { return timerEnd; }
    public void setTimerEnd(Instant timerEnd) { this.timerEnd = timerEnd; }
    public Instant getPreviousTimerStart() { return previousTimerStart; }
    public void setPreviousTimerStart(Instant previousTimerStart) { this.previousTimerStart = previousTimerStart; }
    public Instant getPreviousTimerEnd() { return previousTimerEnd; }
    public void setPreviousTimerEnd(Instant previousTimerEnd) { this.previousTimerEnd = previousTimerEnd; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError == null ? null : lastError.substring(0, Math.min(1000, lastError.length())); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
