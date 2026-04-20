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
    spent

    constructor(id, title, projectId, isCompleted, taskOrder, deadline, estimatedTime, creator, description, actives, scheduled, spent) {
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
        this.spent = spent;
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

            return new Task(json.id, json.title, json.projectId, json.isCompleted, json.taskOrder, json.deadline ? new Date(json.deadline) : null, json.estimatedTime, creator, json.description, actives, scheduled, json.spent)
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

        const percent = this.estimatedTime > 0
            ? Math.min(this.spent / this.estimatedTime, 1)
            : 0;
        let progressOffset = percent * 100;

        if(progressOffset === 100) progressOffset = 0;
        const progressColor = this.spent >= this.estimatedTime
            ? "text-red-500"
            : "text-blue-500";
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
            "scheduled": scheduled,
            "progressOffset": progressOffset,
            "progressColor": progressColor
        })

        parent.innerHTML += html;
    }

    async loadFull(parent){
        let html = await getComponent("taskPopup")

        const scheduled = renderAvatars(this.scheduled.map(user => user.initial))
        const actives = renderAvatars(this.actives.map(user => user.initial))
        const creator = renderAvatars(this.creator?.initial ? [this.creator.initial] : []);
        const currentUser = await getUser();
        const isActive = this.actives.some(user => user.id === currentUser.id);

        let deadlineValue = "";

        if (this.deadline) {
            const d = new Date(this.deadline);

            const year = d.getFullYear();
            const month = String(d.getMonth() + 1).padStart(2, "0");
            const day = String(d.getDate()).padStart(2, "0");
            const hours = String(d.getHours()).padStart(2, "0");
            const minutes = String(d.getMinutes()).padStart(2, "0");

            deadlineValue = `${year}-${month}-${day}T${hours}:${minutes}`;
        }

        html = updateComponent(html, {
            "id": this.id,
            "title": this.title,
            "projectId": this.projectId,
            "isCompleted": this.isCompleted,
            "taskOrder": this.taskOrder,
            "priority": this.taskOrder < 3 ? "High" : "Normal",
            "deadline": deadlineValue,
            "estimatedTime": this.estimatedTime !== 0 ? timeShorterShort(this.estimatedTime) : "",
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