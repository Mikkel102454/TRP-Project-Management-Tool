class User{
    id
    username
    initial
    email
    isAdmin
    isEnabled
    forcedClockedOut
    pmUserId
    pmProfileName

    constructor(id, username, initial, email, isAdmin, isEnabled, forcedClockedOut, pmUserId = null, pmProfileName = null) {
        this.id = id;
        this.username = username;
        this.initial = initial;
        this.email = email;
        this.isAdmin = isAdmin;
        this.isEnabled = isEnabled;
        this.forcedClockedOut = forcedClockedOut
        this.pmUserId = pmUserId
        this.pmProfileName = pmProfileName
    }

    static fromJson(json){
        try {
            return new User(
                json.id,
                json.username,
                json.initial,
                json.email,
                json.admin ?? json.isAdmin,
                json.enabled ?? json.isEnabled,
                json.forcedClockedOut,
                json.pmUserId,
                json.pmProfileName
            )
        } catch (e){
            log(e, Levels.WARNING)
            return null;
        }
    }
}
