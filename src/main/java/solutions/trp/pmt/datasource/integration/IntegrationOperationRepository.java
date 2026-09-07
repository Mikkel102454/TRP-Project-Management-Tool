package solutions.trp.pmt.datasource.integration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface IntegrationOperationRepository extends JpaRepository<IntegrationOperationEntity, String> {
    List<IntegrationOperationEntity> findAllByStateInAndUpdatedAtBefore(Collection<IntegrationOperationEntity.State> states, Instant updatedAt);
    boolean existsByProjectBinding_IdAndStateIn(long bindingId, Collection<IntegrationOperationEntity.State> states);
    boolean existsByUserEntity_IdAndProviderAndStateIn(int userId, String provider, Collection<IntegrationOperationEntity.State> states);
    void deleteAllByProjectBinding_Id(long bindingId);
}
