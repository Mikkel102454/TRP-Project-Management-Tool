package solutions.trp.pmt.datasource.integration;

import jakarta.persistence.*;
import solutions.trp.pmt.datasource.projects.ProjectEntity;

import java.time.Instant;

@Entity
@Table(name = "integration_project_binding",
        indexes = @Index(name = "idx_integration_project_active", columnList = "project_id,active"))
public class ProjectBindingEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @OneToOne
    @JoinColumn(name = "project_id", unique = true)
    private ProjectEntity projectEntity;
    @Column(nullable = false, length = 64)
    private String provider;
    @Column(nullable = false, name = "scope_value")
    private String scope;
    @Column(nullable = false, name = "project_title_snapshot")
    private String projectTitleSnapshot;
    @Column(nullable = false)
    private boolean active;
    @Column(nullable = false, name = "created_at")
    private Instant createdAt;
    @Column(nullable = false, name = "updated_at")
    private Instant updatedAt;

    @PrePersist void createTimestamps() { if (createdAt == null) createdAt = Instant.now(); updatedAt = Instant.now(); }
    @PreUpdate void updateTimestamp() { updatedAt = Instant.now(); }
    public long getId() { return id; }
    public ProjectEntity getProjectEntity() { return projectEntity; }
    public void setProjectEntity(ProjectEntity projectEntity) { this.projectEntity = projectEntity; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public String getProjectTitleSnapshot() { return projectTitleSnapshot; }
    public void setProjectTitleSnapshot(String projectTitleSnapshot) { this.projectTitleSnapshot = projectTitleSnapshot; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
