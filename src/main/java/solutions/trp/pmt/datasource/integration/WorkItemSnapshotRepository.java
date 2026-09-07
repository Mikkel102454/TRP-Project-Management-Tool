package solutions.trp.pmt.datasource.integration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkItemSnapshotRepository extends JpaRepository<WorkItemSnapshotEntity, Long> {
    Optional<WorkItemSnapshotEntity> findByProjectBinding_IdAndExternalId(long bindingId, String externalId);
    Optional<WorkItemSnapshotEntity> findByTaskRef(String taskRef);
    List<WorkItemSnapshotEntity> findAllByProjectBinding_Id(long bindingId);
    void deleteAllByProjectBinding_Id(long bindingId);
}
