package solutions.trp.pmt.datasource.projects;

import jakarta.persistence.*;
import solutions.trp.pmt.datasource.leaders.LeaderEntity;
import solutions.trp.pmt.datasource.scheduled.ScheduledEntity;
import solutions.trp.pmt.datasource.tasks.TaskEntity;
import solutions.trp.pmt.datasource.users.UserEntity;
import solutions.trp.pmt.dto.ProjectDto;
import solutions.trp.pmt.service.TimeService;

import java.util.List;

@Entity(name = "project")
public class ProjectEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, name = "title")
    private String title;

    @Column(unique = true, nullable = false, name = "project_order")
    private int projectOrder;

    @Column(nullable = false, name = "archived")
    private boolean archived;

    @OneToMany(mappedBy = "projectEntity")
    private List<TaskEntity> tasks;

    @OneToMany(mappedBy = "projectEntity")
    private List<LeaderEntity> leaders;

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

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public List<TaskEntity> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskEntity> tasks) {
        this.tasks = tasks;
    }

    public List<LeaderEntity> getLeaders() {
        return leaders;
    }

    public void setLeaders(List<LeaderEntity> leaders) {
        this.leaders = leaders;
    }

    public ProjectDto toDto(TimeService timeService) {
        ProjectDto dto = new ProjectDto();
        dto.setId(id);
        dto.setTitle(title);
        dto.setProjectOrder(projectOrder);
        dto.setArchived(archived);
        dto.setScheduled(
                tasks.stream()
                        .map(TaskEntity::getScheduled)
                        .flatMap(List::stream)
                        .map(ScheduledEntity::getUserEntity)
                        .distinct()
                        .map(UserEntity::toDto)
                        .toList()
        );        dto.setLeader(leaders.stream().map(LeaderEntity::getUserEntity).map(UserEntity::toDto).toList());
        dto.setTasks(tasks.stream().map(task -> task.toDto(timeService)).toList());
        dto.setIsWorkedOn(
                tasks.stream()
                        .flatMap(task -> task.getActives().stream())
                        .findAny()
                        .isPresent()
        );
        return dto;
    }
}
