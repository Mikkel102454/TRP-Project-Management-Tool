package solutions.trp.pmt.service.integration;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import solutions.trp.pmt.controller.api.execption.ConflictException;
import solutions.trp.pmt.controller.api.execption.NotFoundException;
import solutions.trp.pmt.datasource.integration.*;
import solutions.trp.pmt.datasource.projects.ProjectEntity;
import solutions.trp.pmt.datasource.projects.ProjectRepository;
import solutions.trp.pmt.datasource.tasks.TaskRepository;
import solutions.trp.pmt.datasource.users.UserEntity;
import solutions.trp.pmt.datasource.users.UserRepository;
import solutions.trp.pmt.dto.AdminUserDto;
import solutions.trp.pmt.dto.UserDto;
import solutions.trp.pmt.integration.ExternalUserProfile;
import solutions.trp.pmt.integration.feature.FeatureApiClient;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.EnumSet;

@Service
public class IntegrationBindingService {
    private final ProjectBindingRepository projectBindingRepository;
    private final UserBindingRepository userBindingRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProviderRegistry providerRegistry;
    private final IntegrationActiveRepository activeRepository;
    private final IntegrationTimeEntryRepository timeEntryRepository;
    private final IntegrationOperationRepository operationRepository;
    private final WorkItemSnapshotRepository snapshotRepository;

    public IntegrationBindingService(ProjectBindingRepository projectBindingRepository,
                                     UserBindingRepository userBindingRepository,
                                     ProjectRepository projectRepository,
                                     TaskRepository taskRepository,
                                     UserRepository userRepository,
                                     ProviderRegistry providerRegistry,
                                     IntegrationActiveRepository activeRepository,
                                     IntegrationTimeEntryRepository timeEntryRepository,
                                     IntegrationOperationRepository operationRepository,
                                     WorkItemSnapshotRepository snapshotRepository) {
        this.projectBindingRepository = projectBindingRepository;
        this.userBindingRepository = userBindingRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.providerRegistry = providerRegistry;
        this.activeRepository = activeRepository;
        this.timeEntryRepository = timeEntryRepository;
        this.operationRepository = operationRepository;
        this.snapshotRepository = snapshotRepository;
    }

    public Optional<ProjectBindingEntity> activeProjectBinding(int projectId) {
        return projectBindingRepository.findByProjectEntity_IdAndActiveTrue(projectId);
    }

    public boolean isIntegratedProject(int projectId) {
        return activeProjectBinding(projectId).isPresent();
    }

    public void requireLocalProject(int projectId) {
        if (isIntegratedProject(projectId)) {
            throw new ConflictException("PM tasks are read-only; local task changes are not allowed for this project");
        }
    }

    @Transactional
    public ProjectBindingEntity setProjectRelease(int projectId, String release) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));
        String normalized = normalizeOptional(release);
        Optional<ProjectBindingEntity> existing = projectBindingRepository.findByProjectEntity_Id(projectId);
        if (existing.isPresent() && !activeRepository.findAllByProjectBinding_Id(existing.get().getId()).isEmpty()
                && (normalized == null || !normalized.equals(existing.get().getScope()))) {
            throw new ConflictException("Stop active PM timers before changing or removing the PM release");
        }
        if (existing.isPresent() && operationRepository.existsByProjectBinding_IdAndStateIn(existing.get().getId(), recoverableStates())
                && (normalized == null || !normalized.equals(existing.get().getScope()))) {
            throw new ConflictException("Wait for pending PM timer operations before changing the PM release");
        }
        if (normalized == null) {
            existing.ifPresent(binding -> { binding.setActive(false); projectBindingRepository.save(binding); });
            return null;
        }
        if (!taskRepository.findAllByProjectEntity_IdOrderByTaskOrder(projectId).isEmpty()) {
            throw new ConflictException("Remove local tasks before linking this project to PM");
        }
        ProjectBindingEntity binding = existing.orElseGet(ProjectBindingEntity::new);
        binding.setProjectEntity(project);
        binding.setProvider(FeatureApiClient.PROVIDER_KEY);
        binding.setScope(normalized);
        binding.setProjectTitleSnapshot(project.getTitle());
        binding.setActive(true);
        return projectBindingRepository.save(binding);
    }

    @Transactional
    public void detachProject(int projectId) {
        projectBindingRepository.findByProjectEntity_Id(projectId).ifPresent(binding -> {
            if (operationRepository.existsByProjectBinding_IdAndStateIn(binding.getId(), recoverableStates())) {
                throw new ConflictException("Wait for pending PM timer operations before deleting the project");
            }
            binding.setActive(false);
            binding.setProjectEntity(null);
            projectBindingRepository.save(binding);
        });
    }

    @Transactional
    public void deleteProjectBinding(int projectId) {
        projectBindingRepository.findByProjectEntity_Id(projectId).ifPresent(binding -> {
            long bindingId = binding.getId();
            if (activeRepository.existsByProjectBinding_Id(bindingId)
                    || timeEntryRepository.existsByProjectBinding_Id(bindingId)) {
                throw new ConflictException("This PM project cannot be deleted because time has been registered on it");
            }
            if (operationRepository.existsByProjectBinding_IdAndStateIn(bindingId, recoverableStates())) {
                throw new ConflictException("Wait for pending PM timer operations before deleting the project");
            }

            operationRepository.deleteAllByProjectBinding_Id(bindingId);
            operationRepository.flush();
            snapshotRepository.deleteAllByProjectBinding_Id(bindingId);
            snapshotRepository.flush();
            projectBindingRepository.delete(binding);
            projectBindingRepository.flush();
        });
    }

    @Transactional
    public void updateProjectTitleSnapshot(int projectId, String title) {
        projectBindingRepository.findByProjectEntity_Id(projectId).ifPresent(binding -> {
            binding.setProjectTitleSnapshot(title);
            projectBindingRepository.save(binding);
        });
    }

    @Transactional
    public UserBindingEntity setUserBinding(UserEntity user, String pmUserId) {
        String normalized = normalizeOptional(pmUserId);
        Optional<UserBindingEntity> existing = userBindingRepository
                .findByUserEntity_IdAndProvider(user.getId(), FeatureApiClient.PROVIDER_KEY);
        if (activeRepository.existsByUserEntity_IdAndProvider(user.getId(), FeatureApiClient.PROVIDER_KEY)
                && (normalized == null || existing.isEmpty() || !normalized.equals(existing.get().getRemoteAccountId()))) {
            throw new ConflictException("Stop the active PM timer before changing or removing the PM user ID");
        }
        if (operationRepository.existsByUserEntity_IdAndProviderAndStateIn(user.getId(), FeatureApiClient.PROVIDER_KEY, recoverableStates())
                && (normalized == null || existing.isEmpty() || !normalized.equals(existing.get().getRemoteAccountId()))) {
            throw new ConflictException("Wait for pending PM timer operations before changing the PM user ID");
        }
        if (normalized == null) {
            existing.ifPresent(binding -> { binding.setActive(false); userBindingRepository.save(binding); });
            return null;
        }
        ExternalUserProfile profile = providerRegistry.get(FeatureApiClient.PROVIDER_KEY).validateUser(normalized);
        userBindingRepository.findByProviderAndRemoteAccountIdAndActiveTrue(FeatureApiClient.PROVIDER_KEY, normalized)
                .filter(binding -> binding.getUserEntity().getId() != user.getId())
                .ifPresent(binding -> { throw new ConflictException("PM user ID is already linked to another user"); });
        UserBindingEntity binding = existing.orElseGet(UserBindingEntity::new);
        binding.setUserEntity(user);
        binding.setProvider(FeatureApiClient.PROVIDER_KEY);
        binding.setRemoteAccountId(normalized);
        binding.setProfileName(profile.name());
        binding.setValidatedAt(Instant.now());
        binding.setActive(true);
        return userBindingRepository.save(binding);
    }

    @Transactional
    public UserBindingEntity setUserBinding(int userId, String pmUserId) {
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        return setUserBinding(user, pmUserId);
    }

    public Optional<UserBindingEntity> activeUserBinding(int userId, String provider) {
        return userBindingRepository.findByUserEntity_IdAndProviderAndActiveTrue(userId, provider);
    }

    public List<AdminUserDto> getAdminUsers() {
        return userRepository.findAll().stream().map(this::toAdminDto).toList();
    }

    public AdminUserDto getAdminUser(int userId) {
        return toAdminDto(userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found")));
    }

    public AdminUserDto toAdminDto(UserEntity user) {
        UserDto source = user.toDto();
        AdminUserDto dto = new AdminUserDto();
        dto.setId(source.getId()); dto.setUsername(source.getUsername()); dto.setInitial(source.getInitial());
        dto.setEmail(source.getEmail()); dto.setAdmin(source.isAdmin()); dto.setEnabled(source.isEnabled());
        dto.setForcedClockedOut(source.isForcedClockedOut());
        activeUserBinding(user.getId(), FeatureApiClient.PROVIDER_KEY).ifPresent(binding -> {
            dto.setPmUserId(binding.getRemoteAccountId());
            dto.setPmProfileName(binding.getProfileName());
        });
        return dto;
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private static EnumSet<IntegrationOperationEntity.State> recoverableStates() {
        return EnumSet.of(IntegrationOperationEntity.State.PENDING_REMOTE,
                IntegrationOperationEntity.State.REMOTE_APPLIED,
                IntegrationOperationEntity.State.RECONCILIATION_REQUIRED);
    }
}
