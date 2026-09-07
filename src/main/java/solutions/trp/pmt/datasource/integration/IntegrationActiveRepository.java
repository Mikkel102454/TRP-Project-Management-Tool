package solutions.trp.pmt.datasource.integration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IntegrationActiveRepository extends JpaRepository<IntegrationActiveEntity, Long> {
    boolean existsByUserEntity_IdAndProvider(int userId, String provider);
    Optional<IntegrationActiveEntity> findByUserEntity_IdAndTaskRef(int userId, String taskRef);
    List<IntegrationActiveEntity> findAllByTaskRef(String taskRef);
    List<IntegrationActiveEntity> findAllByProjectBinding_Id(long bindingId);
    boolean existsByProjectBinding_Id(long bindingId);
}
