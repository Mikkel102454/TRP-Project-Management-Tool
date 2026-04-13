package solutions.trp.pmt.dto;

import java.util.List;

public class ProjectDto {
    private int id;
    private String title;
    private int projectOrder;
    private boolean isWorkedOn;
    List<UserDto> scheduled;
    List<UserDto> leader;
    List<TaskDto> tasks;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public boolean getIsWorkedOn() {
        return isWorkedOn;
    }

    public void setIsWorkedOn(boolean workedOn) {
        isWorkedOn = workedOn;
    }

    public List<UserDto> getScheduled() {
        return scheduled;
    }

    public void setScheduled(List<UserDto> scheduled) {
        this.scheduled = scheduled;
    }

    public List<UserDto> getLeader() {
        return leader;
    }

    public void setLeader(List<UserDto> leader) {
        this.leader = leader;
    }

    public List<TaskDto> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskDto> tasks) {
        this.tasks = tasks;
    }
}
