package solutions.trp.pmt.datasource.integration;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "integration_work_item_snapshot",
        uniqueConstraints = @UniqueConstraint(name = "uk_integration_work_item", columnNames = {"binding_id", "external_id"}),
        indexes = @Index(name = "idx_integration_task_ref", columnList = "task_ref", unique = true))
public class WorkItemSnapshotEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "binding_id", nullable = false)
    private ProjectBindingEntity projectBinding;
    @Column(nullable = false, name = "external_id", length = 128)
    private String externalId;
    @Column(nullable = false, name = "task_ref", length = 255)
    private String taskRef;
    @Column(nullable = false)
    private String title;
    @Column(name = "raw_status")
    private String rawStatus;
    @Column(name = "module_name")
    private String module;
    @Column(name = "item_type")
    private String type;
    @Column(name = "remote_account_id", length = 128)
    private String remoteAccountId;
    @Lob @Column(name = "metadata_json")
    private String metadataJson;
    @Lob private String description;
    @Lob @Column(name = "development_notes")
    private String developmentNotes;
    @Column(nullable = false, name = "refreshed_at")
    private Instant refreshedAt;

    public long getId() { return id; }
    public ProjectBindingEntity getProjectBinding() { return projectBinding; }
    public void setProjectBinding(ProjectBindingEntity projectBinding) { this.projectBinding = projectBinding; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getTaskRef() { return taskRef; }
    public void setTaskRef(String taskRef) { this.taskRef = taskRef; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getRawStatus() { return rawStatus; }
    public void setRawStatus(String rawStatus) { this.rawStatus = rawStatus; }
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getRemoteAccountId() { return remoteAccountId; }
    public void setRemoteAccountId(String remoteAccountId) { this.remoteAccountId = remoteAccountId; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDevelopmentNotes() { return developmentNotes; }
    public void setDevelopmentNotes(String developmentNotes) { this.developmentNotes = developmentNotes; }
    public Instant getRefreshedAt() { return refreshedAt; }
    public void setRefreshedAt(Instant refreshedAt) { this.refreshedAt = refreshedAt; }
}
