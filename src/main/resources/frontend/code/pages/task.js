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
    taskHolder.innerHTML = "";

    for (let task of project.task) {
        await task.loadPreview(taskHolder);
    }
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

async function startCreateTask(title, estimate, deadline, description){
    await createTask(project.id, title, false, deadline, timeLongerShort(estimate), description)

    loadProject();
}

async function startUpdateTask(title, estimate, deadline, description){
    await updateTask(openTask, title, false, deadline, timeLongerShort(estimate), description)

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
    loadProject();
}

async function deleteTask(id){
    await removeTask(id)
    loadProject();
}
loadProject();