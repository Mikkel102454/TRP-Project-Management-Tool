async function loadProjects() {
    const projects = await searchProjects("");

    const projectHolder = document.getElementById("projects");
    projectHolder.innerHTML = "";

    for (let project of projects) {
        await project.loadPreview(projectHolder);
    }
}

async function openCreateModal(){
    let html = await getComponent("projectCreate")

    const popupHolder = document.getElementById("popupHolder");
    popupHolder.innerHTML = html;
}

loadProjects();