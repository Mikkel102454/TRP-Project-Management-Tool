package solutions.trp.pmt.datasource.tasks;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TaskRepository extends JpaRepository<TaskEntity, Integer> {
    @Query("SELECT COALESCE(MAX(t.taskOrder), 0) FROM tasks t")
    int findMaxOrder();

    List<TaskEntity> findAllByProjectId(int order);
}
