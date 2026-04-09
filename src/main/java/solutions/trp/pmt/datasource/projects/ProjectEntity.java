package solutions.trp.pmt.datasource.projects;

import jakarta.persistence.*;

@Entity(name = "projects")
public class ProjectEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(unique = false, nullable = false, name = "title")
    private String title;

    @Column(unique = false, nullable = false, name = "project_order")
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
}
