async function searchProjects(query){
    try {
        const response = await fetch(
            `${API_ROOT}/project?title=${encodeURIComponent(query)}`
        );

        const data = await response.json();

        if (!data.success) return [];

        return data.data.map(project => Project.fromJson(project));
    } catch (e) {
        log(e, Levels.SEVERE)
    }
}

async function getProject(id){
    try {
        const response = await fetch(
            `${API_ROOT}/project/${encodeURIComponent(id)}`
        );

        const data = await response.json();

        if (!data.success) return null;

        return FullProject.fromJson(data.data);
    } catch (e) {
        log(e, Levels.SEVERE)
    }
}

async function createProject(title) {
    try {
        const response = await fetch(`${API_ROOT}/project`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                title: title
            })
        });

        if (!response.ok) {
            log("Could not create new project.", Levels.SEVERE)
        }

        const data = await response.json();

        if (!data.success) return false;

        return true;
    } catch (e) {
        log(e, Levels.SEVERE);
        return false;
    }
}

async function removeProject(id){
    try {
        const response = await fetch(`${API_ROOT}/project?projectId=${encodeURIComponent(id)}`, {
            method: "DELETE",
            headers: {
                "Content-Type": "application/json"
            },
        });

        if (!response.ok) {
            log("Could not delete project.", Levels.SEVERE)
        }

        const data = await response.json();

        return data.success;
    } catch (e) {
        log(e, Levels.SEVERE);
        return false;
    }
}

async function addProjectLeader(id, userId){
    try {
        const response = await fetch(`${API_ROOT}/project/leader?projectId=${encodeURIComponent(id)}&userId=${encodeURIComponent(userId)}`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
        });

        if (!response.ok) {
            log("Could not add leader to project.", Levels.SEVERE)
        }

        const data = await response.json();

        return data.success;
    } catch (e) {
        log(e, Levels.SEVERE);
        return false;
    }
}

async function removeProjectLeader(id, userId){
    try {
        const response = await fetch(`${API_ROOT}/project/leader?projectId=${encodeURIComponent(id)}&userId=${encodeURIComponent(userId)}`, {
            method: "DELETE",
            headers: {
                "Content-Type": "application/json"
            },
        });

        if (!response.ok) {
            log("Could not remove leader from project.", Levels.SEVERE)
        }

        const data = await response.json();

        return data.success;
    } catch (e) {
        log(e, Levels.SEVERE);
        return false;
    }
}