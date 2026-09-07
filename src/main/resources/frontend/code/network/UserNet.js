async function createUser(username, password, email, isAdmin, isEnabled, pmUserId = null) {
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
        if (email != null) body.email = email;
        if (isAdmin != null) body.isAdmin = isAdmin;
        if (isEnabled != null) body.isEnabled = isEnabled;
        if (pmUserId != null) body.pmUserId = pmUserId;

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
        if (!data.success && data.error?.message) log(data.error.message, Levels.SEVERE);
        return data.success;

    } catch (e) {
        log(e, Levels.SEVERE);
        return false;
    }
}

async function updateUser(id, username, password, email, isAdmin, isEnabled, pmUserId = null) {
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
        if (email != null) body.email = email;
        if (isAdmin != null) body.isAdmin = isAdmin;
        if (isEnabled != null) body.isEnabled = isEnabled;
        if (pmUserId != null) body.pmUserId = pmUserId;

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
        if (!data.success && data.error?.message) log(data.error.message, Levels.SEVERE);
        return data.success;

    } catch (e) {
        log(e, Levels.SEVERE);
        return false;
    }
}

async function updatePassword(oldPassword, newPassword) {
    const data = await changeCurrentPassword(oldPassword, newPassword);
    return data?.success === true;
}

async function changeCurrentPassword(oldPassword, newPassword) {
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

        return await response.json();

    } catch (e) {
        log(e, Levels.SEVERE);
        return {
            success: false,
            error: {
                message: "Could not update password"
            }
        };
    }
}

async function updateCurrentUserEmail(email) {
    try {
        const response = await fetch(`${API_ROOT}/user/email`, {
            method: "PATCH",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                email: email
            })
        });

        return await response.json();

    } catch (e) {
        log(e, Levels.SEVERE);
        return {
            success: false,
            error: {
                message: "Could not update email"
            }
        };
    }
}

async function requestPasswordReset(email) {
    try {
        const response = await fetch(`${API_ROOT}/public/password-reset/request`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                email: email
            })
        });

        const data = await response.json();
        return data.success;

    } catch (e) {
        log(e, Levels.SEVERE);
        return false;
    }
}

async function confirmPasswordReset(token, newPassword) {
    try {
        const response = await fetch(`${API_ROOT}/public/password-reset/confirm`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                token: token,
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

async function getAdminUsers() {
    try {
        const response = await fetch(`${API_ROOT}/admin/user`);
        const data = await response.json();
        return data.success ? data.data.map(user => User.fromJson(user)) : [];
    } catch (e) {
        log(e, Levels.SEVERE);
        return [];
    }
}
