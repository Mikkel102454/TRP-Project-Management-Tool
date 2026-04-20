package solutions.trp.pmt.datasource.scheduled;

import org.springframework.data.jpa.repository.JpaRepository;
import solutions.trp.pmt.datasource.actives.ActiveEntity;

import java.util.List;
import java.util.Optional;

public interface ScheduledRepository extends JpaRepository<ScheduledEntity, Integer> {
    boolean existsByUserEntity_IdAndTaskEntity_Id(Integer userId, Integer taskId);
    Optional<ScheduledEntity> findByUserEntity_IdAndTaskEntity_Id(Integer userId, Integer taskId);
    List<ScheduledEntity> findAllByTaskEntity_Id(Integer taskId);

    void deleteByTaskEntity_Id(Integer taskId);
}
