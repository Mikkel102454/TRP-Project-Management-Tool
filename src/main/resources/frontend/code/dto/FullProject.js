class FullProject extends Project{
    task

    constructor(id, title, projectOrder, isWorkedOn, scheduled, leader, task) {
        super(id, title, projectOrder, isWorkedOn, scheduled, leader);
        this.task = task;
    }

    static fromJson(json){
        try {
            const scheduled = [];

            for (const entry of json.scheduled) {
                scheduled.push(User.fromJson(entry));
            }

            const leaders = [];

            for (const entry of json.leader) {
                leaders.push(User.fromJson(entry));
            }

            const tasks = [];

            for (const entry of json.tasks) {
                tasks.push(Task.fromJson(entry));
            }

            return new FullProject(json.id, json.title, json.projectOrder, json.isWorkedOn, json.scheduled, leaders, tasks)
        } catch (e){
            log(e, Levels.WARING)
            return null;
        }
    }
}