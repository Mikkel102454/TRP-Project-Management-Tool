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
refreshPeriod();

async function refreshPeriod() {
    const params = new URLSearchParams(window.location.search);
    let refreshValue = params.get("refresh");

    if (refreshValue) {
        sessionStorage.setItem("refresh", refreshValue);
    } else {
        refreshValue = sessionStorage.getItem("refresh");
    }

    const seconds = parseInt(refreshValue, 10);
    if (isNaN(seconds) || seconds <= 0) return;

    setInterval(() => {
        try {
            loadProjects();
        } catch (e) {}
    }, seconds * 1000);
}