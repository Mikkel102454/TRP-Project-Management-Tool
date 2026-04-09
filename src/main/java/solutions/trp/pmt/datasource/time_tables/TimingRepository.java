package solutions.trp.pmt.datasource.time_tables;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TimingRepository extends JpaRepository<TimingEntity, Integer> {
    @Query(value = """
    SELECT COALESCE(SUM(TIMESTAMPDIFF(SECOND, start_stamp, end_stamp)), 0)
    FROM time_tables t
    WHERE (:taskId IS NULL OR t.task_id = :taskId)
      AND (:projectId IS NULL OR t.task_id IN (
          SELECT id FROM tasks WHERE project_id = :projectId
      ))
      AND (:userIds IS NULL OR t.user_id IN (:userIds))
    """, nativeQuery = true)
    long sumTime(
            @Param("taskId") Integer taskId,
            @Param("projectId") Integer projectId,
            @Param("userIds") List<Integer> userIds
    );
}
