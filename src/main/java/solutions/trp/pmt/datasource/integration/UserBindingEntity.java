package solutions.trp.pmt.datasource.integration;

import jakarta.persistence.*;
import solutions.trp.pmt.datasource.users.UserEntity;

import java.time.Instant;

@Entity
@Table(name = "integration_user_binding",
        uniqueConstraints = @UniqueConstraint(name = "uk_integration_user_provider", columnNames = {"user_id", "provider"}),
        indexes = @Index(name = "idx_integration_remote_account", columnList = "provider,remote_account_id,active"))
public class UserBindingEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity userEntity;
    @Column(nullable = false, length = 64)
    private String provider;
    @Column(nullable = false, name = "remote_account_id", length = 128)
    private String remoteAccountId;
    @Column(nullable = false, name = "profile_name")
    private String profileName;
    @Column(nullable = false)
    private boolean active;
    @Column(nullable = false, name = "validated_at")
    private Instant validatedAt;

    public long getId() { return id; }
    public UserEntity getUserEntity() { return userEntity; }
    public void setUserEntity(UserEntity userEntity) { this.userEntity = userEntity; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getRemoteAccountId() { return remoteAccountId; }
    public void setRemoteAccountId(String remoteAccountId) { this.remoteAccountId = remoteAccountId; }
    public String getProfileName() { return profileName; }
    public void setProfileName(String profileName) { this.profileName = profileName; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getValidatedAt() { return validatedAt; }
    public void setValidatedAt(Instant validatedAt) { this.validatedAt = validatedAt; }
}
