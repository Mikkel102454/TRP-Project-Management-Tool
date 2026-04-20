package solutions.trp.pmt.datasource.time_tables;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TimingRepository extends JpaRepository<TimingEntity, Integer> {
    @Query(value = """
SELECT COALESCE(SUM(t.time), 0)
FROM timing t
WHERE (:taskId IS NULL OR t.task_id = :taskId)
  AND (:projectId IS NULL OR t.task_id IN (
      SELECT id FROM task WHERE project_id = :projectId
  ))
  AND t.user_id IN (:userIds)
""", nativeQuery = true)
    int sumTime(Integer taskId, Integer projectId, List<Integer> userIds);

    @Query(value = """
SELECT COALESCE(SUM(t.time), 0)
FROM timing t
WHERE (:taskId IS NULL OR t.task_id = :taskId)
  AND (:projectId IS NULL OR t.task_id IN (
      SELECT id FROM task WHERE project_id = :projectId
  ))
""", nativeQuery = true)
    int sumTime(Integer taskId, Integer projectId);

    void deleteByTaskEntity_Id(Integer taskId);
}
