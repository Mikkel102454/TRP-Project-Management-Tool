class Time{
    id
    taskId
    startTime
    endTime
    attention

    constructor(id, taskId, startTime, endTime, attention) {
        this.id = id;
        this.taskId = taskId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.attention = attention
    }

    static fromJson(json){
        try {
            return new Time(json.id, json.taskId, json.startTime, json.endTime, json.attention)
        } catch (e){
            log(e, Levels.WARNING)
            return null;
        }
    }
}

