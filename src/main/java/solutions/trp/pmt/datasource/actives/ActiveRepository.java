package solutions.trp.pmt.datasource.actives;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ActiveRepository extends JpaRepository<ActiveEntity, Integer> {
    boolean existsByUserIdAndTaskId(Integer userId, Integer taskId);
    Optional<ActiveEntity> findByUserIdAndTaskId(Integer userId, Integer taskId);
}
