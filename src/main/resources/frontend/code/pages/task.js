let project;

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
loadProject();