package solutions.trp.pmt.datasource.integration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectBindingRepository extends JpaRepository<ProjectBindingEntity, Long> {
    Optional<ProjectBindingEntity> findByProjectEntity_IdAndActiveTrue(int projectId);
    Optional<ProjectBindingEntity> findByProjectEntity_Id(int projectId);
}
