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

        return data.success;
    } catch (e) {
        log(e, Levels.SEVERE);
        return false;
    }
}
async function updateTask(taskId, title, isCompleted, deadline, estimatedTime, description){
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
                description: description
            })
        });

        const data = await response.json();

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
        const response = await fetch(`${API_ROOT}/task/time/start?taskId=${encodeURIComponent(id)}`, {
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

async function clockOut(id){
    try {
        const response = await fetch(`${API_ROOT}/task/time/stop?taskId=${encodeURIComponent(id)}`, {
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