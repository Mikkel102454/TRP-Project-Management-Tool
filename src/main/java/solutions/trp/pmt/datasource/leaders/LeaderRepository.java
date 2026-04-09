package solutions.trp.pmt.datasource.leaders;

import org.springframework.data.jpa.repository.JpaRepository;
import solutions.trp.pmt.datasource.actives.ActiveEntity;

import java.util.Optional;

public interface LeaderRepository extends JpaRepository<LeaderEntity, Integer> {
    boolean existsByUserIdAndProjectId(Integer userId, Integer projectId);
    Optional<ActiveEntity> findByUserIdAndProjectId(Integer userId, Integer projectId);
}
