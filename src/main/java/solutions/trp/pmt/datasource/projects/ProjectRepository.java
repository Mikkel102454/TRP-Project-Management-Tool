package solutions.trp.pmt.datasource.projects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Integer> {
    boolean existsByTitle(String title);

    @Query("SELECT COALESCE(MAX(p.projectOrder), 0) FROM project p")
    int findMaxOrder();

    @Query("""
    SELECT p FROM project p
    WHERE (:title IS NULL OR p.title LIKE %:title%)
""")
    Page<ProjectEntity> findProjects(
            @Param("title") String title,
            Pageable pageable
    );
}
