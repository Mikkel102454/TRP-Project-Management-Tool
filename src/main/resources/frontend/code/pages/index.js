let loadedProjects = [];
let selectedUserId = "all";
let showOwnAssignedOnly = false;
let currentDashboardUser = null;

async function loadProjects(apiKey) {
    try {
        let projects;
        if(apiKey) projects = await searchProjects("", apiKey);
        else projects = await searchProjects("");

        loadedProjects = Array.isArray(projects) ? projects.filter(project => project !== null) : [];
        await renderProjects();
    } catch (err) { return []; }

}

async function renderProjects() {
    const projectHolder = document.getElementById("projects");
    projectHolder.innerHTML = "";

    const projects = loadedProjects.filter(project => {
        if(project.archived) return false;
        if(showOwnAssignedOnly && !hasAssignedTaskForUser(project, currentDashboardUser?.id)) return false;
        if(selectedUserId !== "all" && !hasAssignedTaskForUser(project, Number(selectedUserId))) return false;
        return true;
    });

    if (projects.length === 0) {
        projectHolder.innerHTML = `
            <div class="bg-gray-200 border border-gray-500 px-4 py-3">
                No projects match this filter.
            </div>
        `;
        return;
    }

    for (let project of projects) {
        await project.loadPreview(projectHolder);
    }
}

function hasAssignedTaskForUser(project, userId) {
    if (!userId) return false;

    return project.task?.some(task =>
        task.status !== "CLOSED" && taskHasUser(task, userId)
    ) || false;
}

function taskHasUser(task, userId) {
    return hasUser(task.scheduled, userId) || hasUser(task.actives, userId);
}

function hasUser(users, userId) {
    return users?.some(user => user.id === userId) || false;
}

async function populateUserFilter() {
    const select = document.getElementById("userFilter");
    const users = await getAllUsers();

    if (!select || !Array.isArray(users)) return;

    for (const user of users) {
        const option = document.createElement("option");
        option.value = String(user.id);
        option.textContent = (user.initial ? `${user.initial} - ` : "") + user.username;
        select.appendChild(option);
    }
}

async function initializeDashboard() {
    currentDashboardUser = await getUser();
    await populateUserFilter();
    await loadProjects(getStoredApiKey());
    refreshPeriod();
}

async function openCreateModal(){
    let html = await getComponent("projectCreate")

    const popupHolder = document.getElementById("popupHolder");
    popupHolder.innerHTML = html;
}

initializeDashboard();

function getStoredApiKey() {
    const params = new URLSearchParams(window.location.search);
    let apiKey = params.get("key");

    if (apiKey) {
        sessionStorage.setItem("apiKey", apiKey);
    } else {
        apiKey = sessionStorage.getItem("apiKey");
    }

    return apiKey;
}

async function refreshPeriod() {
    const params = new URLSearchParams(window.location.search);
    let refreshValue = params.get("refresh");
    const apiKey = getStoredApiKey();

    if (refreshValue) {
        sessionStorage.setItem("refresh", refreshValue);
    } else {
        refreshValue = sessionStorage.getItem("refresh");
    }

    const seconds = parseInt(refreshValue, 10);
    if (isNaN(seconds) || seconds <= 0) return;

    setInterval(async () => {
        await loadProjects(apiKey);
    }, seconds * 1000);
}
