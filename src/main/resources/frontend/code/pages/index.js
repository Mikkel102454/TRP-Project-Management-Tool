async function loadProjects(apiKey) {
    try {
        let projects;
        if(apiKey) projects = await searchProjects("", apiKey);
        else projects = await searchProjects("");

        const projectHolder = document.getElementById("projects");
        projectHolder.innerHTML = "";

        for (let project of projects) {
            await project.loadPreview(projectHolder);
        }
    } catch (err) { return []; }

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
    let apiKey = params.get("key");

    if (refreshValue) {
        sessionStorage.setItem("refresh", refreshValue);
    } else {
        refreshValue = sessionStorage.getItem("refresh");
    }
    if (apiKey) {
        sessionStorage.setItem("apiKey", apiKey);
    } else {
        apiKey = sessionStorage.getItem("apiKey");
    }

    const seconds = parseInt(refreshValue, 10);
    if (isNaN(seconds) || seconds <= 0) return;

    setInterval(async () => {
        await loadProjects(apiKey);
    }, seconds * 1000);
}