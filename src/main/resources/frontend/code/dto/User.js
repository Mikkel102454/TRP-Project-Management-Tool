class User{
    id
    username
    initial
    email
    isAdmin
    isEnabled
    forcedClockedOut

    constructor(id, username, initial, email, isAdmin, isEnabled, forcedClockedOut) {
        this.id = id;
        this.username = username;
        this.initial = initial;
        this.email = email;
        this.isAdmin = isAdmin;
        this.isEnabled = isEnabled;
        this.forcedClockedOut = forcedClockedOut
    }

    static fromJson(json){
        try {
            return new User(json.id, json.username, json.initial, json.email, json.admin, json.enabled, json.forcedClockedOut)
        } catch (e){
            log(e, Levels.WARNING)
            return null;
        }
    }
}
