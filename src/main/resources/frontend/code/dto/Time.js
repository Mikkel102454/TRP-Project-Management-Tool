class Time{
    id
    taskId
    startTime
    endTime
    attention
    taskRef
    source
    provider
    remotelyRegistered
    readOnly
    projectId
    projectTitle
    taskTitle

    constructor(id, taskId, startTime, endTime, attention, integration = {}) {
        this.id = id;
        this.taskId = taskId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.attention = attention
        this.taskRef = integration.taskRef
        this.source = integration.source || "LOCAL"
        this.provider = integration.provider
        this.remotelyRegistered = integration.remotelyRegistered === true
        this.readOnly = integration.readOnly === true
        this.projectId = integration.projectId
        this.projectTitle = integration.projectTitle
        this.taskTitle = integration.taskTitle
    }

    static fromJson(json){
        try {
            return new Time(json.id, json.taskId, json.startTime, json.endTime, json.attention, json)
        } catch (e){
            log(e, Levels.WARNING)
            return null;
        }
    }
}
