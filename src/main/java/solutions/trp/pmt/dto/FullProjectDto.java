package solutions.trp.pmt.dto;

import solutions.trp.pmt.datasource.tasks.TaskEntity;

import java.util.List;

public class FullProjectDto {
    private int id;
    private String title;
    private int projectOrder;
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

    public List<TaskDto> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskDto> tasks) {
        this.tasks = tasks;
    }

    public void addTask(TaskDto task) {
        this.tasks.add(task);
    }
}
