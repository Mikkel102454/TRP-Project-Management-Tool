async function getTimeEntries(userId){
    try {
        const response = await fetch(
            `${API_ROOT}/time/summary?userId=${userId}`
        );

        const data = await response.json();

        if (!data.success) return null;

        return data.data.map(time => Time.fromJson(time));
    } catch (e) {
        log(e, Levels.SEVERE)
    }
}

async function updateTimeEntry(id, startTime, endTime) {
    try {
        const response = await fetch(`${API_ROOT}/time/summary?timeId=${encodeURIComponent(id)}&startTime=${encodeURIComponent(startTime)}&endTime=${encodeURIComponent(endTime)}`, {
            method: "PATCH",
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

async function removeTimeEntry(id) {
    try {
        const response = await fetch(`${API_ROOT}/time/summary?timeId=${encodeURIComponent(id)}`, {
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