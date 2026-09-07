package solutions.trp.pmt.service.integration;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import solutions.trp.pmt.controller.api.execption.NotFoundException;
import solutions.trp.pmt.datasource.integration.*;

import java.time.Instant;

@Service
public class IntegrationTimePersistenceService {
    private final IntegrationActiveRepository activeRepository;
    private final IntegrationTimeEntryRepository entryRepository;

    public IntegrationTimePersistenceService(IntegrationActiveRepository activeRepository,
                                             IntegrationTimeEntryRepository entryRepository) {
        this.activeRepository = activeRepository;
        this.entryRepository = entryRepository;
    }

    @Transactional
    public IntegrationActiveEntity createActive(IntegrationOperationEntity operation) {
        return activeRepository.findByUserEntity_IdAndTaskRef(operation.getUserEntity().getId(), operation.getTaskRef())
                .orElseGet(() -> {
                    IntegrationActiveEntity active = new IntegrationActiveEntity();
                    active.setProvider(operation.getProvider()); active.setProjectBinding(operation.getProjectBinding());
                    active.setSnapshot(operation.getSnapshot()); active.setUserEntity(operation.getUserEntity());
                    active.setTaskRef(operation.getTaskRef()); active.setRemoteRegistrationId(operation.getRemoteRegistrationId());
                    active.setStartTime(operation.getTimerStart());
                    return activeRepository.save(active);
                });
    }

    @Transactional
    public void completeStop(IntegrationOperationEntity operation, boolean attention, boolean retainedRemotely) {
        IntegrationActiveEntity active = activeRepository
                .findByUserEntity_IdAndTaskRef(operation.getUserEntity().getId(), operation.getTaskRef())
                .orElse(null);
        String registrationId = operation.getRemoteRegistrationId();
        if (retainedRemotely && registrationId != null
                && !entryRepository.existsByRemoteRegistrationIdAndProvider(registrationId, operation.getProvider())) {
            IntegrationTimeEntryEntity entry = new IntegrationTimeEntryEntity();
            entry.setProvider(operation.getProvider()); entry.setProjectBinding(operation.getProjectBinding());
            entry.setSnapshot(operation.getSnapshot()); entry.setUserEntity(operation.getUserEntity());
            entry.setTaskRef(operation.getTaskRef()); entry.setRemoteRegistrationId(registrationId);
            entry.setRemoteAccountId(operation.getRemoteAccountId());
            entry.setStartTime(operation.getTimerStart() == null ? Instant.now() : operation.getTimerStart());
            entry.setEndTime(Instant.now()); entry.setAttention(attention);
            entryRepository.save(entry);
        }
        if (active != null) activeRepository.delete(active);
    }

    @Transactional
    public void updateEntry(IntegrationOperationEntity operation, Instant start, Instant end) {
        IntegrationTimeEntryEntity entry = entryRepository
                .findByRemoteRegistrationIdAndProvider(operation.getRemoteRegistrationId(), operation.getProvider())
                .orElseThrow(() -> new NotFoundException("Could not find PM time entry"));
        entry.setStartTime(start);
        entry.setEndTime(end);
        entry.setAttention(false);
        entryRepository.save(entry);
    }
}
