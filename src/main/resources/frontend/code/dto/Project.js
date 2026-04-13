class Project{
    id
    title
    projectOrder
    isWorkedOn
    scheduled
    leader
    task

    constructor(id, title, projectOrder, isWorkedOn, scheduled, leader, task) {
        this.id = id;
        this.title = title;
        this.projectOrder = projectOrder;
        this.isWorkedOn = isWorkedOn;
        this.scheduled = scheduled;
        this.leader = leader;
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


            return new Project(json.id, json.title, json.projectOrder, json.isWorkedOn, scheduled, leaders, tasks)
        } catch (e){
            log(e, Levels.WARNING)
            return null;
        }
    }

    async loadHtml(){

    }

    async loadPreview(parent) {
        let html = getComponent("project")

        const users = renderAvatars(this.scheduled.map(user => user.initial))
        const leaders = renderAvatars(this.leader.map(user => user.initial))
        let tasks;
        for (const task of this.task) {
            tasks += await task.loadHtml();
        }
        html = updateComponent(html, {
            "id": this.id,
            "title": this.title,
            "projectOrder": this.projectOrder,
            "isWorkedOn": this.isWorkedOn,
            "scheduled": users,
            "leader": leaders,
            "tasks": tasks
        })

        parent.append(html)
    }
}