package solutions.trp.pmt.service.integration;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import solutions.trp.pmt.controller.api.execption.ConflictException;
import solutions.trp.pmt.datasource.integration.*;
import solutions.trp.pmt.datasource.users.UserEntity;
import solutions.trp.pmt.integration.ExternalActivity;
import solutions.trp.pmt.integration.ExternalTimeUpdate;
import solutions.trp.pmt.integration.IntegrationException;
import solutions.trp.pmt.integration.IntegrationFailure;
import solutions.trp.pmt.integration.ProjectManagementProvider;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

@Service
public class IntegrationTimeCoordinator {
    private final RemoteWorkItemService workItemService;
    private final IntegrationBindingService bindingService;
    private final ProviderRegistry providerRegistry;
    private final IntegrationActiveRepository activeRepository;
    private final IntegrationOperationRepository operationRepository;
    private final IntegrationTimePersistenceService persistenceService;

    public IntegrationTimeCoordinator(RemoteWorkItemService workItemService,
                                      IntegrationBindingService bindingService,
                                      ProviderRegistry providerRegistry,
                                      IntegrationActiveRepository activeRepository,
                                      IntegrationOperationRepository operationRepository,
                                      IntegrationTimePersistenceService persistenceService) {
        this.workItemService = workItemService;
        this.bindingService = bindingService;
        this.providerRegistry = providerRegistry;
        this.activeRepository = activeRepository;
        this.operationRepository = operationRepository;
        this.persistenceService = persistenceService;
    }

    public synchronized void start(UserEntity user, String taskRef) {
        WorkItemSnapshotEntity snapshot = workItemService.requireSnapshot(taskRef);
        ProjectBindingEntity projectBinding = snapshot.getProjectBinding();
        UserBindingEntity userBinding = bindingService.activeUserBinding(user.getId(), projectBinding.getProvider())
                .orElseThrow(() -> new ConflictException("A validated PM user ID is required to clock PM tasks"));
        if (activeRepository.findByUserEntity_IdAndTaskRef(user.getId(), taskRef).isPresent()) {
            throw new ConflictException("User is already timed on this PM feature");
        }

        ProjectManagementProvider provider = providerRegistry.get(projectBinding.getProvider());
        if (provider.getActivities(userBinding.getRemoteAccountId()).stream()
                .anyMatch(activity -> snapshot.getExternalId().equals(activity.workItemId()))) {
            throw new ConflictException("The PM account is already timed on this feature");
        }
        IntegrationOperationEntity operation = operation(IntegrationOperationEntity.Type.START, user, userBinding, snapshot);
        operation.setTimerStart(Instant.now());
        operationRepository.saveAndFlush(operation);

        String registrationId = provider.startTime(userBinding.getRemoteAccountId(), snapshot.getExternalId());
        operation.setRemoteRegistrationId(registrationId);
        operation.setState(IntegrationOperationEntity.State.REMOTE_APPLIED);
        operationRepository.saveAndFlush(operation);
        try {
            persistenceService.createActive(operation);
            complete(operation);
        } catch (RuntimeException localFailure) {
            compensateStart(provider, operation, localFailure);
        }
    }

    public synchronized void stop(UserEntity user, String taskRef, boolean attention) {
        IntegrationActiveEntity active = activeRepository.findByUserEntity_IdAndTaskRef(user.getId(), taskRef)
                .orElseThrow(() -> new ConflictException("User is not timed on this PM task"));
        UserBindingEntity userBinding = bindingService.activeUserBinding(user.getId(), active.getProvider())
                .orElseThrow(() -> new ConflictException("A validated PM user ID is required to stop this PM timer"));
        ProjectManagementProvider provider = providerRegistry.get(active.getProvider());
        IntegrationOperationEntity operation = operation(IntegrationOperationEntity.Type.STOP, user, userBinding, active.getSnapshot());
        operation.setTimerStart(active.getStartTime()); operation.setRemoteRegistrationId(active.getRemoteRegistrationId());
        operationRepository.saveAndFlush(operation);

        stopIdempotently(provider, userBinding.getRemoteAccountId(), active.getSnapshot().getExternalId());
        operation.setState(IntegrationOperationEntity.State.REMOTE_APPLIED);
        operationRepository.saveAndFlush(operation);
        boolean retainedRemotely;
        try {
            retainedRemotely = provider.timeEntryExists(operation.getRemoteAccountId(),
                    operation.getSnapshot().getExternalId(), operation.getRemoteRegistrationId());
        } catch (IntegrationException failure) {
            operation.setState(IntegrationOperationEntity.State.RECONCILIATION_REQUIRED);
            operation.setLastError(failure.getMessage());
            operationRepository.save(operation);
            throw failure;
        }
        try {
            persistenceService.completeStop(operation, attention, retainedRemotely);
            complete(operation);
        } catch (RuntimeException localFailure) {
            compensateStop(provider, operation, active, localFailure);
        }
    }

    public synchronized void update(IntegrationTimeEntryEntity entry, Instant start, Instant end) {
        if (entry.getRemoteAccountId() == null || entry.getRemoteAccountId().isBlank()) {
            throw new ConflictException("The PM account for this time entry is unavailable");
        }

        ProjectManagementProvider provider = providerRegistry.get(entry.getProvider());
        IntegrationOperationEntity operation = updateOperation(entry, start, end);
        operationRepository.saveAndFlush(operation);

        ExternalTimeUpdate update;
        try {
            update = provider.updateTime(operation.getRemoteAccountId(), entry.getSnapshot().getExternalId(),
                    operation.getRemoteRegistrationId(), start, end);
        } catch (IntegrationException failure) {
            recordUpdateFailure(operation, failure);
            throw failure;
        }

        operation.setPreviousTimerStart(update.previousStart());
        operation.setPreviousTimerEnd(update.previousEnd());
        operation.setState(IntegrationOperationEntity.State.REMOTE_APPLIED);
        operationRepository.saveAndFlush(operation);
        try {
            persistenceService.updateEntry(operation, update.start(), update.end());
        } catch (RuntimeException localFailure) {
            compensateUpdate(provider, operation, localFailure);
        }
        complete(operation);
    }

    private void stopIdempotently(ProjectManagementProvider provider, String accountId, String workItemId) {
        try {
            provider.stopTime(accountId, workItemId);
        } catch (IntegrationException failure) {
            boolean stillActive;
            try {
                stillActive = provider.getActivities(accountId).stream().anyMatch(activity -> workItemId.equals(activity.workItemId()));
            } catch (IntegrationException activityFailure) {
                throw failure;
            }
            if (stillActive) throw failure;
        }
    }

    private void compensateStart(ProjectManagementProvider provider, IntegrationOperationEntity operation, RuntimeException failure) {
        try {
            stopIdempotently(provider, operation.getRemoteAccountId(), operation.getSnapshot().getExternalId());
            operation.setState(IntegrationOperationEntity.State.COMPENSATED);
        } catch (RuntimeException compensationFailure) {
            operation.setState(IntegrationOperationEntity.State.RECONCILIATION_REQUIRED);
            operation.setLastError(compensationFailure.getMessage());
        }
        operationRepository.save(operation);
        throw new IntegrationException(IntegrationFailure.UNAVAILABLE, "PM timer start could not be saved locally", failure);
    }

    private void compensateStop(ProjectManagementProvider provider, IntegrationOperationEntity operation,
                                IntegrationActiveEntity active, RuntimeException failure) {
        try {
            String replacementId = provider.startTime(operation.getRemoteAccountId(), operation.getSnapshot().getExternalId());
            active.setRemoteRegistrationId(replacementId);
            activeRepository.save(active);
            operation.setState(IntegrationOperationEntity.State.COMPENSATED);
        } catch (RuntimeException compensationFailure) {
            operation.setState(IntegrationOperationEntity.State.RECONCILIATION_REQUIRED);
            operation.setLastError(compensationFailure.getMessage());
        }
        operationRepository.save(operation);
        throw new IntegrationException(IntegrationFailure.UNAVAILABLE, "PM timer stop could not be saved locally", failure);
    }

    private void compensateUpdate(ProjectManagementProvider provider, IntegrationOperationEntity operation,
                                  RuntimeException failure) {
        try {
            provider.updateTime(operation.getRemoteAccountId(), operation.getSnapshot().getExternalId(),
                    operation.getRemoteRegistrationId(), operation.getPreviousTimerStart(),
                    operation.getPreviousTimerEnd());
            operation.setState(IntegrationOperationEntity.State.COMPENSATED);
        } catch (RuntimeException compensationFailure) {
            operation.setState(IntegrationOperationEntity.State.RECONCILIATION_REQUIRED);
            operation.setLastError(compensationFailure.getMessage());
        }
        operationRepository.save(operation);
        throw new IntegrationException(IntegrationFailure.UNAVAILABLE, "PM time update could not be saved locally", failure);
    }

    private void recordUpdateFailure(IntegrationOperationEntity operation, IntegrationException failure) {
        operation.setState(switch (failure.getFailure()) {
            case BAD_REQUEST, NOT_FOUND, INVALID_ACCOUNT, AUTHENTICATION -> IntegrationOperationEntity.State.FAILED;
            case TIMEOUT, UNAVAILABLE -> IntegrationOperationEntity.State.RECONCILIATION_REQUIRED;
        });
        operation.setLastError(failure.getMessage());
        operationRepository.save(operation);
    }

    private IntegrationOperationEntity operation(IntegrationOperationEntity.Type type, UserEntity user,
                                                 UserBindingEntity userBinding, WorkItemSnapshotEntity snapshot) {
        IntegrationOperationEntity operation = new IntegrationOperationEntity();
        operation.setProvider(snapshot.getProjectBinding().getProvider()); operation.setType(type);
        operation.setState(IntegrationOperationEntity.State.PENDING_REMOTE);
        operation.setProjectBinding(snapshot.getProjectBinding()); operation.setSnapshot(snapshot); operation.setUserEntity(user);
        operation.setRemoteAccountId(userBinding.getRemoteAccountId()); operation.setTaskRef(snapshot.getTaskRef());
        return operation;
    }

    private IntegrationOperationEntity updateOperation(IntegrationTimeEntryEntity entry, Instant start, Instant end) {
        IntegrationOperationEntity operation = new IntegrationOperationEntity();
        operation.setProvider(entry.getProvider());
        operation.setType(IntegrationOperationEntity.Type.UPDATE);
        operation.setState(IntegrationOperationEntity.State.PENDING_REMOTE);
        operation.setProjectBinding(entry.getProjectBinding());
        operation.setSnapshot(entry.getSnapshot());
        operation.setUserEntity(entry.getUserEntity());
        operation.setRemoteAccountId(entry.getRemoteAccountId());
        operation.setTaskRef(entry.getTaskRef());
        operation.setRemoteRegistrationId(entry.getRemoteRegistrationId());
        operation.setTimerStart(start);
        operation.setTimerEnd(end);
        operation.setPreviousTimerStart(entry.getStartTime());
        operation.setPreviousTimerEnd(entry.getEndTime());
        return operation;
    }

    private void complete(IntegrationOperationEntity operation) {
        operation.setState(IntegrationOperationEntity.State.COMPLETED);
        operation.setLastError(null);
        operationRepository.save(operation);
    }

    @Scheduled(fixedDelay = 60_000)
    public void reconcileInterruptedOperations() {
        List<IntegrationOperationEntity> operations = operationRepository.findAllByStateInAndUpdatedAtBefore(
                EnumSet.of(IntegrationOperationEntity.State.PENDING_REMOTE,
                        IntegrationOperationEntity.State.REMOTE_APPLIED,
                        IntegrationOperationEntity.State.RECONCILIATION_REQUIRED),
                Instant.now().minus(Duration.ofMinutes(1)));
        for (IntegrationOperationEntity operation : operations) {
            try { reconcile(operation); }
            catch (RuntimeException failure) {
                operation.setState(IntegrationOperationEntity.State.RECONCILIATION_REQUIRED);
                operation.setLastError(failure.getMessage());
                operationRepository.save(operation);
            }
        }
    }

    private void reconcile(IntegrationOperationEntity operation) {
        ProjectManagementProvider provider = providerRegistry.get(operation.getProvider());
        if (operation.getType() == IntegrationOperationEntity.Type.UPDATE) {
            ExternalTimeUpdate update = provider.updateTime(operation.getRemoteAccountId(),
                    operation.getSnapshot().getExternalId(), operation.getRemoteRegistrationId(),
                    operation.getTimerStart(), operation.getTimerEnd());
            persistenceService.updateEntry(operation, update.start(), update.end());
            complete(operation);
            return;
        }

        List<ExternalActivity> activities = provider.getActivities(operation.getRemoteAccountId());
        ExternalActivity matching = activities.stream()
                .filter(activity -> operation.getSnapshot().getExternalId().equals(activity.workItemId())).findFirst().orElse(null);
        if (operation.getType() == IntegrationOperationEntity.Type.START) {
            if (matching == null) {
                operation.setState(IntegrationOperationEntity.State.FAILED);
                operationRepository.save(operation);
                return;
            }
            operation.setRemoteRegistrationId(matching.registrationId());
            if (operation.getTimerStart() == null) operation.setTimerStart(matching.startedAt() == null ? Instant.now() : matching.startedAt().atZone(java.time.ZoneId.systemDefault()).toInstant());
            persistenceService.createActive(operation);
        } else if (operation.getType() == IntegrationOperationEntity.Type.STOP) {
            if (matching != null) stopIdempotently(provider, operation.getRemoteAccountId(), operation.getSnapshot().getExternalId());
            boolean retainedRemotely = provider.timeEntryExists(operation.getRemoteAccountId(),
                    operation.getSnapshot().getExternalId(), operation.getRemoteRegistrationId());
            persistenceService.completeStop(operation, false, retainedRemotely);
        }
        complete(operation);
    }
}
