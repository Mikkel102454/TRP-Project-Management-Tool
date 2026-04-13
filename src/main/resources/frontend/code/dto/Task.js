class Task{
    id
    title
    projectId
    isCompleted
    taskOrder
    deadline
    estimatedTime
    creator
    description
    actives
    scheduled

    constructor(id, title, projectId, isCompleted, taskOrder, deadline, estimatedTime, creator, description, actives, scheduled) {
        this.id = id;
        this.title = title;
        this.projectId = projectId;
        this.isCompleted = isCompleted;
        this.taskOrder = taskOrder;
        this.deadline = deadline;
        this.estimatedTime = estimatedTime;
        this.creator = creator;
        this.description = description;
        this.actives = actives;
        this.scheduled = scheduled;
    }

    static fromJson(json){
        try {
            const actives = [];

            for (const entry of json.actives) {
                actives.push(User.fromJson(entry));
            }

            const scheduled = [];

            for (const entry of json.scheduled) {
                actives.push(User.fromJson(entry));
            }

            const creator = User.fromJson(json.creator);

            return new Task(json.id, json.projectId, json.isCompleted, json.taskOrder, json.deadline ? new Date(json.deadline) : null, json.estimatedTime, creator, json.description, actives, scheduled)
        } catch (e){
            log(e, Levels.WARNING)
            return null;
        }
    }
}