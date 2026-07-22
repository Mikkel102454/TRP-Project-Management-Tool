let project;
let openTask;
async function loadProject() {
    const params = new URLSearchParams(window.location.search);

    if (!params.has('id')) {
        return
    }
    const projectId = params.get('id');
    project = await getProject(projectId);

    document.getElementById("title").textContent = project.title

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

    for (let task of project.task) {
        let holder;

        if (task.status === "FINISHED") {
            holder = taskHolderFinished;
            hasFinishedTasks = true;

        } else if (task.status === "CLOSED") {
            holder = taskHolderClosed;
            hasClosedTasks = true;

        } else {
            holder = taskHolder;
        }

        await task.loadPreview(holder);
    }

    taskHolderFinishedHeader.classList.toggle(
        "hidden",
        !hasFinishedTasks
    );

    taskHolderClosedHeader.classList.toggle(
        "hidden",
        !hasClosedTasks
    );
}

async function openModal(id){
    for (let task of project.task) {
        if(task.id !== id) continue;

        const popupHolder = document.getElementById("popupHolder");
        popupHolder.innerHTML = "";
        await task.loadFull(popupHolder);

        const modal = document.getElementById('taskModal');

        initUserPicker(
            modal,
            await getAllUsers(),
            task.scheduled
        );
        openTask = task.id;
    }
}

async function openCreateModal(){
    let html = await getComponent("taskCreate")

    const popupHolder = document.getElementById("popupHolder");
    popupHolder.innerHTML = html;
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

async function startUpdateTask(title, estimate, deadline, description, status){
    await updateTask(openTask, title, false, deadline, timeLongerShort(estimate), description, status)

    loadProject();
}

async function addUserToProject(userId) {
    await scheduleUser(openTask, userId)
    loadProject();
}

async function removeUserToProject(userId) {
    await unscheduleUser(openTask, userId)
    loadProject();
}

async function deleteProject(){
    await removeProject(project.id)
}

async function deleteTask(id){
    await removeTask(id)
    loadProject();
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

function initUserPicker(modal, allUsers, preselectedUsers = []) {
    const selectedContainer = modal.querySelector(".selectedUsers");
    const dropdown = modal.querySelector(".userDropdown");
    const list = modal.querySelector(".userList");
    const search = modal.querySelector(".userSearch");

    const selected = new Map();
    preselectedUsers.forEach(u => selected.set(u.id, u));

    function togglePicker() {
        dropdown.classList.toggle("hidden");
        renderList(search.value);
    }

    function renderList(filter) {
        list.innerHTML = "";

        const searchValue = (filter || "").toLowerCase();
        const fragment = document.createDocumentFragment();

        allUsers
            .filter(u =>
                u.username.toLowerCase().includes(searchValue) &&
                !selected.has(u.id)
            )
            .forEach(user => {
                const div = document.createElement("div");
                div.className = "px-2 py-1 hover:bg-gray-100 cursor-pointer rounded flex justify-between items-center";
                div.innerHTML = `
        <span>${user.username}</span>
        <span class="text-xs text-gray-400">${user.initial}</span>
      `;
                div.onclick = () => addUser(user);
                fragment.appendChild(div);
            });

        list.appendChild(fragment);
    }

    function addUser(user) {
        selected.set(user.id, user);
        addUserToProject(user.id);
        renderSelected();
        renderList(search.value);
    }

    function renderSelected() {
        const initials = [...selected.values()].map(u => u.initial);
        selectedContainer.innerHTML = renderAvatars(initials);

        [...selectedContainer.children].forEach((el, index) => {
            const user = [...selected.values()][index];

            el.style.cursor = "pointer";
            el.onclick = () => {
                selected.delete(user.id);
                removeUserToProject(user.id);
                renderSelected();
                renderList(search.value);
            };
        });
    }

    modal.addEventListener("click", (e) => {
        if (e.target.closest(".userToggle")) {
            togglePicker();
        }
    });

    search.addEventListener("input", () => {
        renderList(search.value);
    });

    window.addEventListener("click", (e) => {
        if (!modal.contains(e.target)) {
            dropdown.classList.add("hidden");
        }
    });

    renderSelected();
}

initTaskDragAndDrop();
initModalDismiss(["taskModal", "passwordModal", "projectModal"]);
initForcedClockoutCheck();
load();
