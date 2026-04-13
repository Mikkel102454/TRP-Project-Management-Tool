class Project{
    id
    title
    projectOrder
    isWorkedOn
    scheduled
    leader


    constructor(id, title, projectOrder, isWorkedOn, scheduled, leader) {
        this.id = id;
        this.title = title;
        this.projectOrder = projectOrder;
        this.isWorkedOn = isWorkedOn;
        this.scheduled = scheduled;
        this.leader = leader;
    }

    static fromJson(json){
        try {
            const scheduled = [];

            for (const entry of json.scheduled) {
                actives.push(User.fromJson(entry));
            }

            const leaders = [];

            for (const entry of json.leader) {
                leaders.push(User.fromJson(entry));
            }

            return new Project(json.id, json.title, json.projectOrder, json.isWorkedOn, json.scheduled, leaders)
        } catch (e){
            log(e, Levels.WARING)
            return null;
        }
    }
}