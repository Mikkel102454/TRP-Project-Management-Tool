class Task{
    id
    title
    status
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
    taskRef
    source
    provider
    externalId
    externalStatus
    readOnly
    module
    type
    developmentNotes
    workedOn
    unmappedScheduledCount
    unmappedActiveCount
    managedActiveUserIds

    constructor(id, title, status, projectId, isCompleted, taskOrder, deadline, estimatedTime, creator, description, actives, scheduled, spent, integration = {}) {
        this.id = id;
        this.title = title;
        this.status = status
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
        this.taskRef = integration.taskRef;
        this.source = integration.source || "LOCAL";
        this.provider = integration.provider;
        this.externalId = integration.externalId;
        this.externalStatus = integration.externalStatus;
        this.readOnly = integration.readOnly === true;
        this.module = integration.module;
        this.type = integration.type;
        this.developmentNotes = integration.developmentNotes;
        this.workedOn = integration.workedOn === true;
        this.unmappedScheduledCount = integration.unmappedScheduledCount || 0;
        this.unmappedActiveCount = integration.unmappedActiveCount || 0;
        this.managedActiveUserIds = integration.managedActiveUserIds || [];
    }

    static fromJson(json){
        try {
            const actives = [];

            for (const entry of (json.actives || [])) {
                actives.push(User.fromJson(entry));
            }

            const scheduled = [];

            for (const entry of (json.scheduled || [])) {
                scheduled.push(User.fromJson(entry));
            }

            const creator = json.creator ? User.fromJson(json.creator) : null;

            return new Task(json.id, json.title, json.status, json.projectId, json.isCompleted, json.taskOrder, json.deadline ? new Date(json.deadline) : null, json.estimatedTime, creator, json.description, actives, scheduled, json.spent, json)
        } catch (e){
            log(e, Levels.WARNING)
            return null;
        }
    }

    async loadHtml() {
        return `<span class="bg-blue-100 text-blue-700 px-2 py-0.5 rounded inline-block max-w-xs truncate">
        ${this.title}
    </span>`;
    }
    async loadPreview(parent){
        if (this.readOnly) {
            const scheduled = this.renderRemoteAvatars(this.scheduled, this.unmappedScheduledCount);
            const actives = this.renderRemoteAvatars(this.actives, this.unmappedActiveCount);
            const led = renderLed(this.workedOn ? "green" : "off");
            const module = this.module
                ? `<span title="Module: ${escapeHtmlAttr(this.module)}"
                         class="block w-full truncate text-left text-[11px] font-medium text-gray-700">
                       ${escapeHtmlAttr(this.module)}
                   </span>`
                : "";
            const type = this.type
                ? `<span title="Type: ${escapeHtmlAttr(this.type)}"
                         class="block w-full truncate text-left text-[11px] font-medium text-gray-700">
                       ${escapeHtmlAttr(this.type)}
                   </span>`
                : "";
            parent.innerHTML += `<div onclick="openModal('${escapeHtmlAttr(this.taskRef)}')"
                 data-id="${escapeHtmlAttr(this.taskRef)}"
                 class="task-item relative grid grid-cols-12 items-center h-14 px-4
                        bg-gradient-to-r from-gray-100 via-gray-200 to-gray-300
                        border border-gray-500 hover:bg-gray-300 cursor-pointer transition">
                <div class="col-span-5 grid items-center gap-2 overflow-hidden pr-5"
                     style="grid-template-columns: minmax(0, 1fr) 7rem 6rem">
                    <div class="min-w-0 truncate text-left text-gray-900 tracking-wide">${escapeHtmlAttr(this.title)}</div>
                    <div class="min-w-0">${module}</div>
                    <div class="min-w-0">${type}</div>
                </div>
                <div class="col-span-3 self-stretch flex items-center gap-1 overflow-hidden pl-5 pr-3">${scheduled}</div>
                <div class="col-span-3 self-stretch flex items-center gap-1 overflow-hidden pl-5 pr-3">${actives}</div>
                ${led}
            </div>`;
            return;
        }
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
            "title": escapeHtmlAttr(this.title),
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
            "progressColor": progressColor,
            "banner-color": getColorFromStatus(this.status),
            "status": this.status,
            "cursor-hover": this.status === "FINISHED" ? "" : "cursor-grab",
            "draggable": this.status === "FINISHED" ? "false" : "true"
        })

        parent.innerHTML += html;
    }

    async loadFull(parent){
        if (this.readOnly) {
            let html = await getComponent("remoteTaskPopup");
            const currentUser = await getUser();
            const isActive = this.actives.some(user => user.id === currentUser.id);
            const isManagedActive = this.managedActiveUserIds.includes(currentUser.id);
            const timingLabel = isManagedActive ? "CLOCK OUT" : isActive ? "ACTIVE IN PM" : "CLOCK IN";
            const timingAction = isManagedActive ? `clockOut('${this.taskRef}')` : `clockIn('${this.taskRef}')`;
            html = updateComponent(html, {
                title: escapeHtmlAttr(this.title), status: escapeHtmlAttr(this.externalStatus || this.status || "UNKNOWN"),
                module: escapeHtmlAttr(this.module || ""), type: escapeHtmlAttr(this.type || ""),
                scheduled: this.renderRemoteAvatars(this.scheduled, this.unmappedScheduledCount),
                actives: this.renderRemoteAvatars(this.actives, this.unmappedActiveCount),
                description: escapeHtmlAttr(this.description || ""), developmentNotes: escapeHtmlAttr(this.developmentNotes || ""),
                isTimed: timingLabel,
                isTimedAction: timingAction,
                timingDisabled: isActive && !isManagedActive ? "disabled" : ""
            });
            parent.innerHTML += html;
            return;
        }
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
            "title": escapeHtmlAttr(this.title),
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
            "status": this.status,
            "isTimed": isActive ? "CLOCK OUT" : "CLOCK IN",
            "isTimedAction": isActive ? `clockOut('${this.taskRef}')` : `clockIn('${this.taskRef}')`,
            "statusOption": await getDropdownOptions(this.status),
            "isAdmin": currentUser.isAdmin
        })

        parent.innerHTML += html;
    }

    renderRemoteAvatars(users, unmappedCount) {
        const initials = users.map(user => user.initial);
        for (let i = 0; i < unmappedCount; i++) initials.push("PM");
        return renderAvatars(initials);
    }
}
