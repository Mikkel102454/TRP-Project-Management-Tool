class Time{
    id
    taskId
    startTime
    endTime

    constructor(id, taskId, startTime, endTime) {
        this.id = id;
        this.taskId = taskId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    static fromJson(json){
        try {
            return new Time(json.id, json.taskId, json.startTime, json.endTime)
        } catch (e){
            log(e, Levels.WARNING)
            return null;
        }
    }
}

