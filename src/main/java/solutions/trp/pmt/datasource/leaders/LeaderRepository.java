package solutions.trp.pmt.datasource.leaders;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaderRepository extends JpaRepository<LeaderEntity, Integer> {
    boolean existsByUserEntity_IdAndProjectEntity_Id(Integer userId, Integer projectId);
    Optional<LeaderEntity> findByUserEntity_IdAndProjectEntity_Id(Integer userId, Integer projectId);
    List<LeaderEntity> findAllByProjectEntity_Id(Integer projectId);
}
