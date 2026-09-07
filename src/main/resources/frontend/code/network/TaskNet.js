async function createTask(projectId, title, isCompleted, deadline, estimatedTime, description){
    try {
        if(title === null || title === "") {
            log("Title cannot be empty", Levels.SEVERE);
            return;
        }
        const response = await fetch(`${API_ROOT}/task`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                projectId: projectId,
                title: title,
                isCompleted: isCompleted,
                deadline: deadline,
                estimatedTime: estimatedTime,
                description: description
            })
        });

        const data = await response.json();
        if (!data.success && data.error?.message) log(data.error.message, Levels.SEVERE);
        return data.success;
    } catch (e) {
        log(e, Levels.SEVERE);
        return false;
    }
}
async function updateTask(taskId, title, isCompleted, deadline, estimatedTime, description, status){
    try {
        const response = await fetch(`${API_ROOT}/task`, {
            method: "PATCH",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                taskId: taskId,
                title: title,
                isCompleted: isCompleted,
                deadline: deadline,
                estimatedTime: estimatedTime,
                description: description,
                status: status
            })
        });

        const data = await response.json();
        if (!data.success && data.error?.message) log(data.error.message, Levels.SEVERE);
        return data.success;
    } catch (e) {
        log(e, Levels.SEVERE);
        return false;
    }
}
async function removeTask(id){
    try {
        const response = await fetch(`${API_ROOT}/task?taskId=${encodeURIComponent(id)}`, {
            method: "DELETE",
            headers: {
                "Content-Type": "application/json"
            },
        });

        const data = await response.json();

        return data.success;
    } catch (e) {
        log(e, Levels.SEVERE);
        return false;
    }
}
async function scheduleUser(id, userId){
    try {
        const response = await fetch(`${API_ROOT}/task/schedule?taskId=${encodeURIComponent(id)}&userId=${encodeURIComponent(userId)}`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
        });

        const data = await response.json();

        return data.success;
    } catch (e) {
        log(e, Levels.SEVERE);
        return false;
    }
}
async function unscheduleUser(id, userId){
    try {
        const response = await fetch(`${API_ROOT}/task/schedule?taskId=${encodeURIComponent(id)}&userId=${encodeURIComponent(userId)}`, {
            method: "DELETE",
            headers: {
                "Content-Type": "application/json"
            },
        });

        const data = await response.json();

        return data.success;
    } catch (e) {
        log(e, Levels.SEVERE);
        return false;
    }
}

async function clockIn(id){
    try {
        const key = String(id).includes(":") ? "taskRef" : "taskId";
        const response = await fetch(`${API_ROOT}/task/time/start?${key}=${encodeURIComponent(id)}`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
        });

        const data = await response.json();
        if (!data.success && data.error?.message) log(data.error.message, Levels.SEVERE);
        return data.success;
    } catch (e) {
        log(e, Levels.SEVERE);
        return false;
    }
}

async function clockOut(id){
    try {
        const key = String(id).includes(":") ? "taskRef" : "taskId";
        const response = await fetch(`${API_ROOT}/task/time/stop?${key}=${encodeURIComponent(id)}`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
        });

        const data = await response.json();
        if (!data.success && data.error?.message) log(data.error.message, Levels.SEVERE);
        return data.success;
    } catch (e) {
        log(e, Levels.SEVERE);
        return false;
    }
}

async function getRemoteTaskDetails(taskRef) {
    const response = await fetch(`${API_ROOT}/task/details?taskRef=${encodeURIComponent(taskRef)}`);
    const data = await response.json();
    return data.success ? Task.fromJson(data.data) : null;
}

async function changeTaskPriority(id, priority){
    try {
        const response = await fetch(`${API_ROOT}/task/order?taskId=${encodeURIComponent(id)}&priority=${encodeURIComponent(priority)}`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
        });

        const data = await response.json();

        return data.success;
    } catch (e) {
        log(e, Levels.SEVERE);
        return false;
    }
}
