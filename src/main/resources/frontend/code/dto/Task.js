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
                scheduled.push(User.fromJson(entry));
            }

            const creator = User.fromJson(json.creator);

            return new Task(json.id, json.title, json.projectId, json.isCompleted, json.taskOrder, json.deadline ? new Date(json.deadline) : null, json.estimatedTime, creator, json.description, actives, scheduled)
        } catch (e){
            log(e, Levels.WARNING)
            return null;
        }
    }

    async loadHtml(){
        return "<span class=\"bg-blue-100 text-blue-700 px-2 py-0.5 rounded\">" + this.title + "</span>"
    }

    async loadPreview(parent){
        let html = await getComponent("task")

        const scheduled = renderAvatars(this.scheduled.map(user => user.initial))
        const actives = renderAvatars(this.actives.map(user => user.initial))
        const creator = renderAvatars(this.creator?.initial ? [this.creator.initial] : []);
        html = updateComponent(html, {
            "id": this.id,
            "title": this.title,
            "projectId": this.projectId,
            "isCompleted": this.isCompleted,
            "taskOrder": this.taskOrder,
            "deadline": this.deadline,
            "estimatedTime": timeShorter(this.estimatedTime),
            "creator": creator,
            "description": this.description,
            "actives": actives,
            "scheduled": scheduled
        })

        parent.innerHTML += html;
    }

    async loadFull(parent){
        let html = await getComponent("taskPopup")

        const scheduled = renderAvatars(this.scheduled.map(user => user.initial))
        const actives = renderAvatars(this.actives.map(user => user.initial))
        const creator = renderAvatars(this.creator?.initial ? [this.creator.initial] : []);
        const isActive = this.actives.some(async user => user.id === await getUser().id);

        let formatted = null;
        if(this.deadline != null){
            const date = new Date(this.deadline);

            formatted =
                date.getDate() + "/" +
                (date.getMonth() + 1) + "/" +
                date.getFullYear() + " " +
                String(date.getHours()).padStart(2, "0") + "." +
                String(date.getMinutes()).padStart(2, "0");
        }

        html = updateComponent(html, {
            "id": this.id,
            "title": this.title,
            "projectId": this.projectId,
            "isCompleted": this.isCompleted,
            "taskOrder": this.taskOrder,
            "priority": this.taskOrder < 3 ? "High" : "Normal",
            "deadline": formatted !== null ? formatted : "No Deadline",
            "estimatedTime": this.estimatedTime !== 0 ? timeShorterLong(this.estimatedTime) : "No Estimate",
            "creator": creator,
            "description": this.description,
            "actives": actives,
            "scheduled": scheduled,
            "isTimed": isActive ? "Clock out" : "Clock in",
            "isTimedAction": isActive ? `clockOut(${this.id})` : `clockIn(${this.id})`
        })

        parent.innerHTML += html;
    }
}