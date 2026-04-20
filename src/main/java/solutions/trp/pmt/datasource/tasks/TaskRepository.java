package solutions.trp.pmt.datasource.tasks;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TaskRepository extends JpaRepository<TaskEntity, Integer> {
    @Query("""
SELECT COALESCE(MAX(t.taskOrder), 0)
FROM task t
WHERE t.projectEntity.id = :projectId
""")
    int findMaxOrderByProjectId(int projectId);

    List<TaskEntity> findAllByProjectEntity_IdOrderByTaskOrder(int order);

    @Modifying
    @Query("""
UPDATE task t
SET t.taskOrder = t.taskOrder + 1000000
WHERE t.projectEntity.id = :projectId
AND t.taskOrder > :start AND t.taskOrder <= :end
""")
    void bumpRangeUp(int projectId, int start, int end);

    @Modifying
    @Query("""
UPDATE task t
SET t.taskOrder = t.taskOrder - 1000001
WHERE t.projectEntity.id = :projectId
AND t.taskOrder > :start + 1000000 AND t.taskOrder <= :end + 1000000
""")
    void shiftRangeDown(int projectId, int start, int end);

    @Modifying
    @Query("""
UPDATE task t
SET t.taskOrder = t.taskOrder - 1000000
WHERE t.projectEntity.id = :projectId
AND t.taskOrder >= :start AND t.taskOrder < :end
""")
    void bumpRangeDown(int projectId, int start, int end);

    @Modifying
    @Query("""
UPDATE task t
SET t.taskOrder = t.taskOrder + 1000001
WHERE t.projectEntity.id = :projectId
AND t.taskOrder >= :start - 1000000 AND t.taskOrder < :end - 1000000
""")
    void shiftRangeUp(int projectId, int start, int end);

    @Modifying
    @Query("""
    UPDATE task t
    SET t.taskOrder = t.taskOrder - 1
    WHERE t.projectEntity.id = :projectId
    AND t.taskOrder > :deletedPriority
""")
    void shiftAfterDelete(int projectId, int deletedPriority);
}
