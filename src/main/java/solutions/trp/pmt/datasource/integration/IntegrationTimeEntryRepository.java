package solutions.trp.pmt.datasource.integration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IntegrationTimeEntryRepository extends JpaRepository<IntegrationTimeEntryEntity, Long> {
    List<IntegrationTimeEntryEntity> findAllByUserEntity_Id(int userId);
    List<IntegrationTimeEntryEntity> findAllByTaskRef(String taskRef);
    boolean existsByRemoteRegistrationIdAndProvider(String remoteRegistrationId, String provider);
    Optional<IntegrationTimeEntryEntity> findByRemoteRegistrationIdAndProvider(String remoteRegistrationId, String provider);
    boolean existsByProjectBinding_Id(long bindingId);
}
