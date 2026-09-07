package solutions.trp.pmt.datasource.integration;

import jakarta.persistence.*;
import solutions.trp.pmt.datasource.users.UserEntity;
import solutions.trp.pmt.dto.TimeDto;

import java.time.Instant;
import java.time.ZoneOffset;

@Entity
@Table(name = "integration_time_entry", indexes = {
        @Index(name = "idx_integration_time_user", columnList = "user_id,start_time"),
        @Index(name = "idx_integration_time_task", columnList = "task_ref")
})
public class IntegrationTimeEntryEntity {
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
    @Column(name = "remote_account_id", length = 128)
    private String remoteAccountId;
    @Column(nullable = false, name = "start_time")
    private Instant startTime;
    @Column(nullable = false, name = "end_time")
    private Instant endTime;
    @Column(nullable = false)
    private boolean attention;

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
    public String getRemoteAccountId() { return remoteAccountId; }
    public void setRemoteAccountId(String remoteAccountId) { this.remoteAccountId = remoteAccountId; }
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
    public boolean isAttention() { return attention; }
    public void setAttention(boolean attention) { this.attention = attention; }

    public TimeDto toDto() {
        TimeDto dto = new TimeDto();
        dto.setId(-Math.toIntExact(Math.min(id, Integer.MAX_VALUE)));
        dto.setTaskId(null);
        dto.setTaskRef(taskRef);
        dto.setSource("REMOTE");
        dto.setProvider(provider);
        dto.setRemotelyRegistered(true);
        dto.setReadOnly(remoteAccountId == null || remoteAccountId.isBlank());
        dto.setProjectId(projectBinding.getProjectEntity() == null ? null : projectBinding.getProjectEntity().getId());
        dto.setProjectTitle(projectBinding.getProjectEntity() == null ? projectBinding.getProjectTitleSnapshot() : projectBinding.getProjectEntity().getTitle());
        dto.setTaskTitle(snapshot.getTitle());
        dto.setUserId(userEntity.getId());
        dto.setStartTime(startTime.atOffset(ZoneOffset.UTC));
        dto.setEndTime(endTime.atOffset(ZoneOffset.UTC));
        dto.setAttention(attention);
        return dto;
    }
}
