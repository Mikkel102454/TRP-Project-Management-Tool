let openTask;

async function openTaskPopup(task) {
    try {
        if (task.readOnly) {
            task = await getRemoteTaskDetails(task.taskRef);
            if (!task) return;
        }
        const popupHolder = document.getElementById("popupHolder");
        popupHolder.innerHTML = "";
        await task.loadFull(popupHolder);
        if (!task.readOnly) {
            openTask = task.id;
            initUserPicker(document.getElementById("taskModal"), await getAllUsers(), task.scheduled);
        }
    } catch (error) {
        log(error, Levels.SEVERE);
    }
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

async function deleteTask(id){
    await removeTask(id)
    loadProject();
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

