const DISPLAY_REFRESH_INTERVAL_MS = 5_000;

let displayHasLoadedProjects = false;
let displayAuthenticationFailed = false;

function readDisplayToken() {
    return new URL(window.location.href).searchParams.get("token");
}

function showDisplayStatus(message, authenticationError = false) {
    const status = document.getElementById("displayStatus");
    status.textContent = message;
    status.classList.remove("hidden");
    status.classList.toggle("bg-red-100", authenticationError);
    status.classList.toggle("border-red-600", authenticationError);
    status.classList.toggle("text-red-900", authenticationError);
    status.classList.toggle("bg-gray-200", !authenticationError);
    status.classList.toggle("border-gray-500", !authenticationError);
    status.classList.toggle("text-gray-900", !authenticationError);
}

function hideDisplayStatus() {
    document.getElementById("displayStatus").classList.add("hidden");
}

async function renderDisplayProjects(projects) {
    const projectHolder = document.getElementById("projects");
    projectHolder.innerHTML = "";

    if (projects.length === 0) {
        projectHolder.innerHTML = `
            <div class="bg-gray-200 border border-gray-500 px-4 py-3">
                No active projects.
            </div>
        `;
        return;
    }

    for (const project of projects) {
        await project.loadPreview(projectHolder);
        const projectElement = projectHolder.lastElementChild;
        projectElement?.removeAttribute("onclick");
        projectElement?.classList.remove("cursor-pointer");
    }
}

async function loadDisplayProjects(token) {
    try {
        const response = await fetch(`${API_ROOT}/display/projects`, {
            headers: {
                "X-Display-Token": token
            }
        });

        if (response.status === 401 || response.status === 403) {
            displayAuthenticationFailed = true;
            document.getElementById("projects").innerHTML = "";
            showDisplayStatus("Display authentication failed. Reopen this page with its setup URL.", true);
            return;
        }

        if (!response.ok) throw new Error(`Display request failed with status ${response.status}`);

        const body = await response.json();
        if (!body.success || !Array.isArray(body.data)) throw new Error("Display response was invalid");

        const projects = body.data.map(project => Project.fromJson(project)).filter(project => project !== null);
        await renderDisplayProjects(projects);
        displayHasLoadedProjects = true;
        hideDisplayStatus();
    } catch (e) {
        log(e, Levels.WARNING);
        if (displayHasLoadedProjects) {
            showDisplayStatus("Unable to refresh projects. Showing the last available data.");
        } else {
            document.getElementById("projects").innerHTML = "";
            showDisplayStatus("Unable to load projects. The display will retry automatically.");
        }
    }
}

async function initializeDisplay() {
    const token = readDisplayToken();

    if (!token) {
        document.getElementById("projects").innerHTML = "";
        showDisplayStatus("A display token is required. Open this page with its setup URL.", true);
        return;
    }

    await loadDisplayProjects(token);
    if (displayAuthenticationFailed) return;

    setInterval(() => loadDisplayProjects(token), DISPLAY_REFRESH_INTERVAL_MS);
}

initializeDisplay();
