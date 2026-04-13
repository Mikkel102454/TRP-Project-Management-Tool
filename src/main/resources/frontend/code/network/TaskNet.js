async function createTask(projectId, title, isCompleted, deadline, estimatedTime, description){
    try {
        const response = await fetch(`${API_ROOT}/project`, {
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
async function removeTask(id){
    try {
        const response = await fetch(`${API_ROOT}/project/task?taskId=${encodeURIComponent(id)}&userId=${encodeURIComponent(userId)}`, {
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