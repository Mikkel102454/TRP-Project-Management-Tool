let project;
let selectedTaskUserId = "all";
let taskUserFilterStorageKey = null;
async function loadProject() {
    const params = new URLSearchParams(window.location.search);

    if (!params.has('id')) {
        return
    }
    const projectId = params.get('id');
    project = await getProject(projectId);

    if (!project) return;

    document.getElementById("title").textContent = project.title

    const integrated = !!project.integration;
    const currentUser = await getUser();
    taskUserFilterStorageKey = currentUser
        ? `taskUserFilter:${currentUser.id}:${project.id}`
        : `taskUserFilter:${project.id}`;
    populateTaskUserFilter(currentUser, integrated);

    document.getElementById("new-task-btn").classList.toggle("hidden", integrated);
    document.getElementById("module-column-label").classList.toggle("hidden", !integrated);
    document.getElementById("type-column-label").classList.toggle("hidden", !integrated);
    await renderProjectTasks();
}

function populateTaskUserFilter(currentUser, integrated) {
    const select = document.getElementById("task-user-filter");
    const assignedUsers = new Map();

    for (const task of project.task) {
        for (const user of task.scheduled || []) assignedUsers.set(user.id, user);
    }

    if (integrated && currentUser) assignedUsers.set(currentUser.id, currentUser);

    select.innerHTML = '<option value="all">ALL USERS</option>';
    [...assignedUsers.values()]
        .sort((left, right) => left.username.localeCompare(right.username))
        .forEach(user => {
            const option = document.createElement("option");
            option.value = String(user.id);
            option.textContent = user.username;
            select.appendChild(option);
        });

    const defaultUserId = integrated && currentUser ? String(currentUser.id) : "all";
    const savedUserId = taskUserFilterStorageKey
        ? localStorage.getItem(taskUserFilterStorageKey)
        : null;
    selectedTaskUserId = [...select.options].some(option => option.value === savedUserId)
        ? savedUserId
        : defaultUserId;
    select.value = selectedTaskUserId;
}

function changeTaskUserFilter(userId) {
    selectedTaskUserId = userId;
    if (taskUserFilterStorageKey) localStorage.setItem(taskUserFilterStorageKey, userId);
    renderProjectTasks();
}

async function renderProjectTasks() {
    if (!project) return;

    const taskHolder = document.getElementById("tasks");
    const taskHolderFinished = document.getElementById("tasks-finished");
    const taskHolderFinishedHeader = document.getElementById("tasks-finished-header");
    let hasFinishedTasks = false;
    const taskHolderClosed = document.getElementById("tasks-closed");
    const taskHolderClosedHeader = document.getElementById("tasks-closed-header");
    let hasClosedTasks = false;
    taskHolder.innerHTML = "";
    taskHolderFinished.innerHTML = "";
    taskHolderClosed.innerHTML = "";

    const integrated = !!project.integration;
    if (integrated && project.integration.available === false) {
        taskHolder.innerHTML = `<div class="border border-red-500 bg-red-100 text-red-800 px-3 py-4">PM TASKS UNAVAILABLE</div>`;
    }

    for (let task of project.task) {
        if (selectedTaskUserId !== "all" && !taskHasAssignedUser(task, Number(selectedTaskUserId))) continue;
        let holder;

        if (!integrated && task.status === "FINISHED") {
            holder = taskHolderFinished;
            hasFinishedTasks = true;

        } else if (!integrated && task.status === "CLOSED") {
            holder = taskHolderClosed;
            hasClosedTasks = true;

        } else {
            holder = taskHolder;
        }

        await task.loadPreview(holder);
    }

    taskHolderFinishedHeader.classList.toggle(
        "hidden",
        integrated || !hasFinishedTasks
    );

    taskHolderClosedHeader.classList.toggle(
        "hidden",
        integrated || !hasClosedTasks
    );
}

function taskHasAssignedUser(task, userId) {
    return task.scheduled?.some(user => user.id === userId) || false;
}

async function openModal(id) {
    const task = project.task.find(task => task.id === id || task.taskRef === id);
    if (task) await openTaskPopup(task);
}

async function openCreateModal(){
    if (project.integration) return;
    let html = await getComponent("taskCreate")

    const popupHolder = document.getElementById("popupHolder");
    popupHolder.innerHTML = html;
    document.getElementById("taskTitleInput")?.focus();
}

async function openRenameModal(){
    let html = await getComponent("projectRename")
    html = updateComponent(html, {
        "id": project.id,
        "title": escapeHtmlAttr(project.title),
    })

    const popupHolder = document.getElementById("popupHolder");
    popupHolder.innerHTML = html;
}

async function archiveProject(){
    if(project.archived) {
        await unarchiveTask(project.id)
    } else await archiveTask(project.id);

    loadProject();
}

async function startCreateTask(title, estimate, deadline, description){
    await createTask(project.id, title, false, deadline, timeLongerShort(estimate), description)

    loadProject();
}




async function deleteProject(){
    return await removeProject(project.id)
}


async function load(){
    await loadProject();
    document.getElementById("archive-btn").innerHTML = project.archived ? "UNARCHIVE" : "ARCHIVE";

    if(project.archived){
        document.getElementById("title-header").href += "archives";
    }

}

function initTaskDragAndDrop() {
    const list = document.getElementById("tasks");
    if (!list) return;

    let dragged = null;
    let isDragging = false;

    const placeholder = document.createElement("div");
    placeholder.className = "h-14 border border-gray-500 bg-gray-300 mx-0 my-1 opacity-60";

    list.addEventListener("dragstart", (e) => {
        if (project?.integration) return;
        if (!e.target.closest("#tasks")) return;

        const item = e.target.closest(".task-item");
        if (!item) return;

        dragged = item;
        isDragging = true;

        item.classList.add("opacity-40");
        placeholder.style.height = item.offsetHeight + "px";

        setTimeout(() => {
            item.style.display = "none";
        }, 0);
    });

    list.addEventListener("dragend", () => {
        if (!dragged) return;

        dragged.style.display = "";
        dragged.classList.remove("opacity-40");
        placeholder.remove();

        setTimeout(() => {
            isDragging = false;
        }, 0);

        dragged = null;
    });

    list.addEventListener("dragover", (e) => {
        e.preventDefault();

        const afterElement = getDragAfterElement(list, e.clientY);

        if (afterElement == null) {
            list.appendChild(placeholder);
        } else {
            list.insertBefore(placeholder, afterElement);
        }
    });

    list.addEventListener("drop", async (e) => {
        e.preventDefault();

        if (dragged && placeholder.parentNode) {
            placeholder.parentNode.insertBefore(dragged, placeholder);

            const items = [...list.querySelectorAll(".task-item")];
            const newIndex = items.indexOf(dragged) + 1;
            const taskId = dragged.dataset.id;

            await changeTaskPriority(taskId, newIndex);
            loadProject();
        }
    });

    list.addEventListener("click", (e) => {
        if (!isDragging) return;

        e.preventDefault();
        e.stopPropagation();
    }, true);
}

function getDragAfterElement(container, y) {
    const elements = [...container.querySelectorAll(".task-item:not(.opacity-40)")];

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


initTaskDragAndDrop();
initModalDismiss(["taskModal", "projectModal"]);
initForcedClockoutCheck();
load();
