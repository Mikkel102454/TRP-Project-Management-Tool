package solutions.trp.pmt.datasource.actives;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ActiveRepository extends JpaRepository<ActiveEntity, Integer> {
    boolean existsByUserEntity_IdAndTaskEntity_Id(Integer userId, Integer taskId);
    Optional<ActiveEntity> findByUserEntity_IdAndTaskEntity_Id(Integer userId, Integer taskId);
}
