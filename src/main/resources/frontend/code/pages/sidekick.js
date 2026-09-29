let sidekickRefreshTimer;

async function sidekickGet(endpoint) {
    const response = await fetch(`${CONTEXT_PATH}${endpoint}`, {cache: "no-store"});
    if (!response.ok || response.redirected) throw new Error("Could not load Sidekick");
    const result = await response.json();
    if (!result.success) throw new Error("Could not load Sidekick");
    return result.data;
}

async function renderSidekicks(holder, tasks) {
    const list = document.createElement("div");
    list.className = "flex flex-col gap-2";
    holder.appendChild(list);
    for (const data of tasks) {
        const task = Task.fromJson(data);
        if (!task) continue;
        const row = document.createElement("div");
        await task.loadPreview(row);
        const preview = row.firstElementChild;
        preview.removeAttribute("onclick");
        preview.draggable = false;
        preview.classList.remove("cursor-grab");
        preview.classList.add("cursor-pointer");
        preview.setAttribute("role", "button");
        preview.tabIndex = 0;
        preview.setAttribute("aria-label", task.title);
        preview.addEventListener("click", () => openTaskPopup(task));
        preview.addEventListener("keydown", event => {
            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                openTaskPopup(task);
            }
        });
        list.appendChild(preview);
    }
}

async function refreshSidekickDisplay() {
    clearTimeout(sidekickRefreshTimer);
    try {
        const data = await sidekickGet("/gadget/sidekick/data");
        const projects = document.createElement("div");
        for (const project of data.projects) {
            const section = document.createElement("section");
            const title = document.createElement("h2");
            title.className = "font-semibold mb-2";
            title.textContent = project.title;
            section.appendChild(title);
            await renderSidekicks(section, project.tasks);
            projects.appendChild(section);
        }
        if (!data.projects.length) projects.textContent = "No unfinished tasks assigned to you.";
        const recent = document.createElement("div");
        await renderSidekicks(recent, data.recentTasks);
        if (!data.recentTasks.length) recent.textContent = "No recent unfinished tasks.";
        document.getElementById("projects").replaceChildren(...projects.childNodes);
        document.getElementById("recentTasks").replaceChildren(recent);
        document.getElementById("loadError").hidden = true;
    } catch (error) {
        document.getElementById("loadError").hidden = false;
        log(error, Levels.WARNING);
    } finally {
        clearTimeout(sidekickRefreshTimer);
        sidekickRefreshTimer = setTimeout(refreshSidekickDisplay, 3000);
    }
}

// Shared task popup actions refresh the current page through this hook.
async function loadProject() {
    await refreshSidekickDisplay();
}

initModalDismiss(["taskModal"]);
refreshSidekickDisplay();
