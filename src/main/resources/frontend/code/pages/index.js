let loadedProjects = [];
let selectedUserId = "all";
let showOwnAssignedOnly = false;
let currentDashboardUser = null;
let draggedProject = null;
let isProjectDragging = false;

const projectPlaceholder = document.createElement("div");
projectPlaceholder.className = "h-16 border border-gray-500 bg-gray-300 mx-0 my-1 opacity-60";

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

    updateProjectDragState();
}

function canReorderProjects() {
    return selectedUserId === "all" && !showOwnAssignedOnly;
}

function updateProjectDragState() {
    const canReorder = canReorderProjects();

    document.querySelectorAll(".project-item").forEach(item => {
        item.draggable = canReorder;
        item.classList.toggle("cursor-grab", canReorder);
        item.classList.toggle("cursor-pointer", !canReorder);
    });
}

function getProjectDragAfterElement(container, y) {
    const elements = [...container.querySelectorAll(".project-item:not(.opacity-40)")];

    return elements.reduce((closest, child) => {
        const box = child.getBoundingClientRect();
        const offset = y - box.top - box.height / 2;

        if (offset < 0 && offset > closest.offset) {
            return { offset: offset, element: child };
        } else {
            return closest;
        }
    }, { offset: Number.NEGATIVE_INFINITY }).element;
}

function initializeProjectDragAndDrop() {
    const projectHolder = document.getElementById("projects");
    if (!projectHolder) return;

    projectHolder.addEventListener("dragstart", (e) => {
        if (!canReorderProjects()) {
            e.preventDefault();
            return;
        }

        const item = e.target.closest(".project-item");
        if (!item) return;

        draggedProject = item;
        isProjectDragging = true;

        item.classList.add("opacity-40");
        projectPlaceholder.style.height = item.offsetHeight + "px";

        setTimeout(() => {
            item.style.display = "none";
        }, 0);
    });

    projectHolder.addEventListener("dragend", () => {
        if (!draggedProject) return;

        draggedProject.style.display = "";
        draggedProject.classList.remove("opacity-40");

        projectPlaceholder.remove();

        setTimeout(() => {
            isProjectDragging = false;
        }, 0);

        draggedProject = null;
    });

    projectHolder.addEventListener("dragover", (e) => {
        if (!draggedProject || !canReorderProjects()) return;

        e.preventDefault();

        const afterElement = getProjectDragAfterElement(projectHolder, e.clientY);

        if (afterElement == null) {
            projectHolder.appendChild(projectPlaceholder);
        } else {
            projectHolder.insertBefore(projectPlaceholder, afterElement);
        }
    });

    projectHolder.addEventListener("drop", async (e) => {
        if (!draggedProject || !canReorderProjects()) return;

        e.preventDefault();

        if (projectPlaceholder.parentNode) {
            projectPlaceholder.parentNode.insertBefore(draggedProject, projectPlaceholder);

            const items = [...projectHolder.querySelectorAll(".project-item")];
            const newIndex = items.indexOf(draggedProject) + 1;
            const projectId = draggedProject.dataset.id;

            await changeProjectPriority(projectId, newIndex);
            await loadProjects(getStoredApiKey());
        }
    });

    projectHolder.addEventListener("click", (e) => {
        if (isProjectDragging) {
            e.preventDefault();
            e.stopPropagation();
        }
    }, true);
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
    initializeProjectDragAndDrop();
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
