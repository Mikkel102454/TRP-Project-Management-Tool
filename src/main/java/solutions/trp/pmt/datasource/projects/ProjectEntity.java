package solutions.trp.pmt.datasource.projects;

import jakarta.persistence.*;
import solutions.trp.pmt.dto.FullProjectDto;
import solutions.trp.pmt.dto.ProjectDto;

@Entity(name = "project")
public class ProjectEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(unique = false, nullable = false, name = "title")
    private String title;

    @Column(unique = true, nullable = false, name = "project_order")
    private int projectOrder;

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getProjectOrder() {
        return projectOrder;
    }

    public void setProjectOrder(int projectOrder) {
        this.projectOrder = projectOrder;
    }

    public ProjectDto toDto() {
        ProjectDto dto = new ProjectDto();
        dto.setId(id);
        dto.setTitle(title);
        dto.setProjectOrder(projectOrder);

        return dto;
    }

    public FullProjectDto toFullDto() {
        FullProjectDto dto = new FullProjectDto();
        dto.setId(id);
        dto.setTitle(title);
        dto.setProjectOrder(projectOrder);

        return dto;
    }
}
