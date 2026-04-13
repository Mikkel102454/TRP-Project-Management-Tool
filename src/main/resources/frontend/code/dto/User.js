class User{
    id
    username
    initial
    constructor(id, username, initial) {
        this.id = id;
        this.username = username;
        this.initial = initial;
    }

    static fromJson(json){
        try {
            return new User(json.id, json.username, json.initial)
        } catch (e){
            log(e, Levels.WARING)
            return null;
        }
    }
}

