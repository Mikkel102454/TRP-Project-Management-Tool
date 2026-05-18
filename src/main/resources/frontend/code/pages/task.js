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
load();