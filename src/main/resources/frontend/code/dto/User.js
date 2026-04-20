class User{
    id
    username
    initial
    isAdmin
    isEnabled
    constructor(id, username, initial, isAdmin, isEnabled) {
        this.id = id;
        this.username = username;
        this.initial = initial;
        this.isAdmin = isAdmin;
        this.isEnabled = isEnabled;
    }

    static fromJson(json){
        try {
            return new User(json.id, json.username, json.initial, json.admin, json.enabled)
        } catch (e){
            log(e, Levels.WARNING)
            return null;
        }
    }
}

