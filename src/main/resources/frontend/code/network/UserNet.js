async function createUser(username, password, isAdmin, isEnabled) {
    try {
        if(username === null || username === "") {
            log("Username cannot be empty", Levels.SEVERE);
            return;
        }
        if(password === null || password === "") {
            log("Password cannot be empty", Levels.SEVERE);
            return;
        }
        if(password.length < 3) {
            log("Password must be 3 characters or more", Levels.SEVERE);
            return;
        }

        const body = {};

        if (username != null) body.username = username;
        if (password != null) body.password = password;
        if (isAdmin != null) body.isAdmin = isAdmin;
        if (isEnabled != null) body.isEnabled = isEnabled;

        // optional: generate initial from username if provided
        if (username != null) {
            body.initial = username.substring(0, 2).toUpperCase();
        }

        const response = await fetch(`${API_ROOT}/admin/user`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(body)
        });

        const data = await response.json();
        return data.success;

    } catch (e) {
        log(e, Levels.SEVERE);
        return false;
    }
}

async function updateUser(id, username, password, isAdmin, isEnabled) {
    try {
        if(username != null && username.length !== 0 && username === "") {
            log("Username cannot be empty", Levels.SEVERE);
            return;
        }
        if(password != null && password.length !== 0 && password === "") {
            log("Password cannot be empty", Levels.SEVERE);
            return;
        }
        if(password != null && password.length !== 0 && password.length < 3) {
            log("Password must be 3 characters or more", Levels.SEVERE);
            return;
        }
        const body = {};

        if (id != null) body.userId = id;
        if (username != null) body.username = username;
        if (password != null) body.password = password;
        if (isAdmin != null) body.isAdmin = isAdmin;
        if (isEnabled != null) body.isEnabled = isEnabled;

        if (username != null) {
            body.initial = username.substring(0, 2).toUpperCase();
        }

        const response = await fetch(`${API_ROOT}/admin/user`, {
            method: "PATCH",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(body)
        });

        const data = await response.json();
        return data.success;

    } catch (e) {
        log(e, Levels.SEVERE);
        return false;
    }
}

async function updatePassword(oldPassword, newPassword) {
    try {
        const response = await fetch(`${API_ROOT}/user/password`, {
            method: "PATCH",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                oldPassword: oldPassword,
                newPassword: newPassword
            })
        });

        const data = await response.json();
        return data.success;

    } catch (e) {
        log(e, Levels.SEVERE);
        return false;
    }
}

async function getAllUsers() {
    try {
        const response = await fetch(`${API_ROOT}/user`, {
            method: "GET",
            headers: {
                "Content-Type": "application/json"
            },
        });

        const data = await response.json();
        return data.data.map(user => User.fromJson(user));

    } catch (e) {
        log(e, Levels.SEVERE);
        return false;
    }
}