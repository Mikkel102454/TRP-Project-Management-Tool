package solutions.trp.pmt.service.integration;

import com.google.gson.Gson;
import org.springframework.stereotype.Service;
import solutions.trp.pmt.controller.api.execption.NotFoundException;
import solutions.trp.pmt.datasource.integration.*;
import solutions.trp.pmt.dto.TaskDto;
import solutions.trp.pmt.dto.UserDto;
import solutions.trp.pmt.integration.ExternalActivity;
import solutions.trp.pmt.integration.ExternalWorkItem;
import solutions.trp.pmt.integration.ProjectManagementProvider;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class RemoteWorkItemService {
    private final ProviderRegistry providerRegistry;
    private final WorkItemSnapshotRepository snapshotRepository;
    private final UserBindingRepository userBindingRepository;
    private final IntegrationActiveRepository activeRepository;
    private final IntegrationTimeEntryRepository timeEntryRepository;
    private final Gson gson = new Gson();

    public RemoteWorkItemService(ProviderRegistry providerRegistry,
                                 WorkItemSnapshotRepository snapshotRepository,
                                 UserBindingRepository userBindingRepository,
                                 IntegrationActiveRepository activeRepository,
                                 IntegrationTimeEntryRepository timeEntryRepository) {
        this.providerRegistry = providerRegistry;
        this.snapshotRepository = snapshotRepository;
        this.userBindingRepository = userBindingRepository;
        this.activeRepository = activeRepository;
        this.timeEntryRepository = timeEntryRepository;
    }

    /** Always calls the provider. Snapshots are deliberately never used as an offline list. */
    public List<TaskDto> list(ProjectBindingEntity binding) {
        ProjectManagementProvider provider = providerRegistry.get(binding.getProvider());
        List<ExternalWorkItem> remoteItems = provider.listWorkItems(binding.getScope());
        ActivityContext activityContext = new ActivityContext(provider);
        return remoteItems.stream()
                .map(item -> toDto(refreshSnapshot(binding, item), item, activityContext))
                .toList();
    }

    public TaskDto details(String taskRef) {
        WorkItemSnapshotEntity snapshot = snapshotRepository.findByTaskRef(taskRef)
                .orElseThrow(() -> new NotFoundException("PM task not found"));
        ProjectBindingEntity binding = snapshot.getProjectBinding();
        ProjectManagementProvider provider = providerRegistry.get(binding.getProvider());
        ExternalWorkItem item = provider.getWorkItem(snapshot.getExternalId());
        return toDto(refreshSnapshot(binding, item), item, new ActivityContext(provider));
    }

    public WorkItemSnapshotEntity requireSnapshot(String taskRef) {
        WorkItemSnapshotEntity snapshot = snapshotRepository.findByTaskRef(taskRef)
                .orElseThrow(() -> new NotFoundException("PM task not found"));
        if (!snapshot.getProjectBinding().isActive()) throw new NotFoundException("PM task is no longer linked to this project");
        return snapshot;
    }

    private WorkItemSnapshotEntity refreshSnapshot(ProjectBindingEntity binding, ExternalWorkItem item) {
        WorkItemSnapshotEntity snapshot = snapshotRepository
                .findByProjectBinding_IdAndExternalId(binding.getId(), item.id())
                .orElseGet(WorkItemSnapshotEntity::new);
        snapshot.setProjectBinding(binding);
        snapshot.setExternalId(item.id());
        snapshot.setTaskRef(taskRef(binding, item.id()));
        snapshot.setTitle(item.title() == null ? "PM feature " + item.id() : item.title());
        snapshot.setRawStatus(item.status()); snapshot.setModule(item.module()); snapshot.setType(item.type());
        snapshot.setRemoteAccountId(item.accountId()); snapshot.setMetadataJson(gson.toJson(item.metadata()));
        if (item.description() != null) snapshot.setDescription(item.description());
        if (item.developmentNotes() != null) snapshot.setDevelopmentNotes(item.developmentNotes());
        snapshot.setRefreshedAt(Instant.now());
        return snapshotRepository.save(snapshot);
    }

    private TaskDto toDto(WorkItemSnapshotEntity snapshot, ExternalWorkItem item, ActivityContext activityContext) {
        TaskDto dto = new TaskDto();
        dto.setId(0); dto.setTaskRef(snapshot.getTaskRef()); dto.setSource("REMOTE");
        dto.setProvider(snapshot.getProjectBinding().getProvider()); dto.setExternalId(snapshot.getExternalId());
        dto.setExternalStatus(item.status()); dto.setStatus(item.status()); dto.setReadOnly(true);
        dto.setProjectId(snapshot.getProjectBinding().getProjectEntity().getId()); dto.setTitle(snapshot.getTitle());
        dto.setTaskOrder(0); dto.setCompleted(false); dto.setEstimatedTime(0);
        dto.setDescription(item.description()); dto.setDevelopmentNotes(item.developmentNotes());
        dto.setModule(item.module()); dto.setType(item.type());
        dto.setMetadata(item.metadata());

        String provider = snapshot.getProjectBinding().getProvider();
        UserDto assigned = activityContext.resolveUser(provider, item.accountId());
        dto.setScheduled(assigned == null ? Collections.emptyList() : List.of(assigned));
        dto.setUnmappedScheduledCount(assigned == null && hasText(item.accountId()) ? 1 : 0);
        dto.setCreator(dto.getScheduled().stream().findFirst().orElse(null));

        List<IntegrationActiveEntity> managedActives = activeRepository.findAllByTaskRef(snapshot.getTaskRef());
        Map<Integer, UserDto> activeUsers = new LinkedHashMap<>();
        Set<String> managedRegistrationIds = new HashSet<>();
        for (IntegrationActiveEntity active : managedActives) {
            UserDto user = active.getUserEntity().toDto();
            activeUsers.put(user.getId(), user);
            managedRegistrationIds.add(active.getRemoteRegistrationId());
        }

        Set<String> unmappedActiveAccounts = new LinkedHashSet<>();
        for (ExternalActivity activity : activityContext.forWorkItem(item.id())) {
            if (hasText(activity.accountId())) {
                UserDto user = activityContext.resolveUser(provider, activity.accountId());
                if (user == null) unmappedActiveAccounts.add("account:" + activity.accountId());
                else activeUsers.put(user.getId(), user);
            } else if (!managedRegistrationIds.contains(activity.registrationId())) {
                unmappedActiveAccounts.add("registration:" + activity.registrationId());
            }
        }
        dto.setActives(List.copyOf(activeUsers.values()));
        dto.setUnmappedActiveCount(unmappedActiveAccounts.size());
        dto.setManagedActiveUserIds(managedActives.stream()
                .map(active -> active.getUserEntity().getId()).distinct().toList());
        dto.setWorkedOn(!managedActives.isEmpty() || !activityContext.forWorkItem(item.id()).isEmpty());

        int spent = timeEntryRepository.findAllByTaskRef(snapshot.getTaskRef()).stream()
                .mapToInt(entry -> (int) Duration.between(entry.getStartTime(), entry.getEndTime()).getSeconds()).sum();
        for (IntegrationActiveEntity active : managedActives) {
            spent += (int) Duration.between(active.getStartTime(), Instant.now()).getSeconds();
        }
        dto.setSpent(spent);
        return dto;
    }

    public static String taskRef(ProjectBindingEntity binding, String externalId) {
        return "remote:" + binding.getProvider() + ":" + binding.getId() + ":" + externalId;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private final class ActivityContext {
        private final Map<String, List<ExternalActivity>> byWorkItem = new HashMap<>();
        private final Map<String, Optional<UserBindingEntity>> userBindings = new HashMap<>();

        private ActivityContext(ProjectManagementProvider provider) {
            Map<String, ExternalActivity> activities = new LinkedHashMap<>();
            int anonymousIndex = 0;
            List<ExternalActivity> globalActivities;
            try {
                globalActivities = provider.getActivities();
            } catch (RuntimeException ignored) {
                // Activity is display enrichment. A failure must never hide the remote task list.
                globalActivities = Collections.emptyList();
            }
            for (ExternalActivity activity : globalActivities) {
                String key = hasText(activity.registrationId())
                        ? activity.registrationId() : "anonymous:" + anonymousIndex++;
                activities.put(key, activity);
            }

            Set<String> activeWorkItems = activities.values().stream()
                    .map(ExternalActivity::workItemId).filter(RemoteWorkItemService::hasText)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            for (String workItemId : activeWorkItems) {
                try {
                    for (ExternalActivity activity : provider.getWorkItemActivities(workItemId)) {
                        ExternalActivity globalActivity = activities.get(activity.registrationId());
                        if (globalActivity != null) {
                            activities.put(activity.registrationId(), new ExternalActivity(
                                    globalActivity.registrationId(), globalActivity.workItemId(),
                                    activity.accountId(), globalActivity.startedAt()));
                        }
                    }
                } catch (RuntimeException ignored) {
                    // The global feed still provides activity/LED state; unresolved users remain generic PM avatars.
                }
            }

            for (ExternalActivity activity : activities.values()) {
                if (hasText(activity.workItemId())) {
                    byWorkItem.computeIfAbsent(activity.workItemId(), ignored -> new ArrayList<>()).add(activity);
                }
            }
        }

        private List<ExternalActivity> forWorkItem(String workItemId) {
            return byWorkItem.getOrDefault(workItemId, Collections.emptyList());
        }

        private UserDto resolveUser(String provider, String accountId) {
            if (!hasText(accountId)) return null;
            String key = provider + "\u0000" + accountId;
            return userBindings.computeIfAbsent(key, ignored -> userBindingRepository
                            .findByProviderAndRemoteAccountIdAndActiveTrue(provider, accountId))
                    .map(binding -> binding.getUserEntity().toDto()).orElse(null);
        }
    }
}
