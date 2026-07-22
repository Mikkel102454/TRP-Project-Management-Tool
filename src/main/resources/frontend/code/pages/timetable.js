const isAdmin = window.__BOOTSTRAP__?.isAdmin === true;

let timeEntries = [];
let projects = [];
let selectedProject = "all";
let selectedUserId = null;
let minimumGapMinutes = 30;

const minuteMs = 60 * 1000;
const hourMs = 60 * minuteMs;
const dayMs = 24 * hourMs;

function toDatetimeLocalValue(isoString) {
    const d = new Date(isoString);
    const pad = (n) => String(n).padStart(2, "0");

    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
        + `T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

function renderEntries() {
    const container = document.getElementById("userList");
    container.innerHTML = "";

    const visibleEntries = getVisibleEntries();
    renderGapGraph(visibleEntries);

    visibleEntries.forEach(entry => {
        let foundTask = null;
        let foundProject = null;

        for (const project of projects) {
            const task = project.task?.find(t => t.id === entry.taskId);
            if (task) {
                foundTask = task;
                foundProject = project;
                break;
            }
        }

        const div = document.createElement("div");
        div.className = `
grid grid-cols-12 items-center
h-16 px-3
gap-x-2
${entry?.attention ? "bg-gradient-to-r from-yellow-100 via-yellow-200 to-yellow-300"  : "bg-gradient-to-r from-gray-100 via-gray-200 to-gray-300"}
border border-gray-500
`;

        const deleteButton = isAdmin ? `
    <button class="px-2 py-1 border border-red-700
                   bg-red-500 text-white text-xs
                   hover:bg-red-600"
        onclick="deleteEntry(${entry.id})">
        DELETE
    </button>
` : "";

        div.innerHTML = `
<div class="col-span-3 text-gray-900 truncate">
    ${foundProject?.title || "UNKNOWN"}
</div>

<div class="col-span-3 text-gray-700 truncate">
    ${foundTask?.title || "UNKNOWN"}
</div>

<div class="col-span-2 flex justify-start">
    <input type="datetime-local"
            step="1"
           value="${toDatetimeLocalValue(entry.startTime)}"
           class="w-[95%] border border-gray-500 bg-gray-100 text-xs"
           onchange="updateEntry(${entry.id}, this.value, null, this)">
</div>

<div class="col-span-2 flex justify-start">
    <input type="datetime-local"
           step="1"
           value="${toDatetimeLocalValue(entry.endTime)}"
           class="w-[95%] border border-gray-500 bg-gray-100 text-xs"
           onchange="updateEntry(${entry.id}, null, this.value, this)">
</div>

<div class="col-span-2 flex justify-end gap-2">
    <button class="px-2 py-1 border border-blue-700 hidden
                   bg-blue-500 text-white text-xs
                   hover:bg-blue-600 update-button"
        onclick="update(${entry.id}, this)">
        UPDATE
    </button>

    ${deleteButton}
</div>
`;

        container.appendChild(div);
    });

    if (visibleEntries.length === 0) {
        container.innerHTML = `
                <div class="border border-gray-500 bg-gray-100 px-3 py-6 text-center text-xs text-gray-600">
                    NO TIME ENTRIES FOR THIS FILTER
                </div>
            `;
    }
}

function getVisibleEntries() {
    const openProjectIds = new Set(
        projects
            .filter(project => hasOpenTaskForUser(project, selectedUserId))
            .map(project => project.id)
    );

    return timeEntries.filter(entry => {
        if (selectedProject === "all") return true;

        const project = findProjectForEntry(entry);
        if (!project) return false;

        if (selectedProject === "open") return openProjectIds.has(project.id);
        return project.id === Number(selectedProject);
    });
}

function renderGapGraph(entries) {
    const graph = document.getElementById("gapGraph");
    const count = document.getElementById("gapCount");
    const total = document.getElementById("gapTotal");
    const max = document.getElementById("gapMax");
    const gaps = getTimeGaps(entries)
        .filter(gap => gap.durationMs >= minimumGapMinutes * minuteMs);
    const weeks = getWeeklyGapSummary(gaps);

    graph.innerHTML = "";

    const totalGapMs = gaps.reduce((sum, gap) => sum + gap.durationMs, 0);
    const maxWeekMs = Math.max(0, ...weeks.map(week => week.totalMs));

    count.textContent = `${weeks.length} ${weeks.length === 1 ? "WEEK" : "WEEKS"}`;
    total.textContent = `${formatDuration(totalGapMs)} TOTAL`;
    max.textContent = `${formatDuration(maxWeekMs)} MAX WEEK`;

    if (entries.length < 2) {
        graph.innerHTML = `
                <div class="time-gap-empty">
                    NEED AT LEAST TWO TIME ENTRIES TO SHOW GAPS
                </div>
            `;
        return;
    }

    if (gaps.length === 0) {
        graph.innerHTML = `
                <div class="time-gap-empty">
                    NO GAPS MATCH THIS FILTER
                </div>
            `;
        return;
    }

    graph.appendChild(createExampleCard("1", "TOTAL GAP BARS", renderWeeklyBars(weeks, maxWeekMs)));
    graph.appendChild(createExampleCard("2", "DAY STACKED WEEKS", renderStackedWeeks(weeks)));
    graph.appendChild(createExampleCard("3", "WEEK HEATMAP", renderWeeklyHeatmap(weeks)));
    graph.appendChild(createExampleCard("4", "LARGEST GAP RANKING", renderLargestGapRanking(weeks)));
    graph.appendChild(createExampleCard("5", "DAILY TIMELINE BLOCKS", renderDailyTimeline(weeks), true));
}

function getTimeGaps(entries) {
    const sortedEntries = entries
        .filter(entry => entry.startTime && entry.endTime)
        .slice()
        .sort((a, b) => new Date(a.startTime) - new Date(b.startTime));

    const gaps = [];
    for (let i = 0; i < sortedEntries.length - 1; i++) {
        const currentEnd = new Date(sortedEntries[i].endTime);
        const nextStart = new Date(sortedEntries[i + 1].startTime);
        const durationMs = nextStart - currentEnd;

        if (durationMs <= 0) continue;

        gaps.push({
            start: currentEnd,
            end: nextStart,
            durationMs
        });
    }

    return gaps.sort((a, b) => b.durationMs - a.durationMs);
}

function getWeeklyGapSummary(gaps) {
    const weeksByKey = new Map();

    gaps
        .slice()
        .sort((a, b) => a.start - b.start)
        .forEach(gap => {
            const weekStart = getWeekStart(gap.start);
            const key = weekStart.toISOString().slice(0, 10);

            if (!weeksByKey.has(key)) {
                weeksByKey.set(key, {
                    key,
                    start: weekStart,
                    end: new Date(weekStart.getTime() + 6 * dayMs),
                    totalMs: 0,
                    largestGapMs: 0,
                    gapCount: 0,
                    days: Array.from({length: 7}, () => ({totalMs: 0, gapCount: 0}))
                });
            }

            const week = weeksByKey.get(key);
            const day = getWeekdayIndex(gap.start);
            week.totalMs += gap.durationMs;
            week.largestGapMs = Math.max(week.largestGapMs, gap.durationMs);
            week.gapCount++;
            week.days[day].totalMs += gap.durationMs;
            week.days[day].gapCount++;
        });

    return [...weeksByKey.values()].sort((a, b) => b.start - a.start);
}

function createExampleCard(number, title, content, wide = false) {
    const card = document.createElement("div");
    card.className = `time-gap-example${wide ? " time-gap-example-wide" : ""}`;
    card.innerHTML = `
            <div class="time-gap-example-title">
                <span>${number}. ${title}</span>
                <span>PICK ${number}</span>
            </div>
            <div class="time-gap-example-body">${content}</div>
        `;
    return card;
}

function renderWeeklyBars(weeks, maxWeekMs) {
    return weeks.map(week => {
        const width = getPercent(week.totalMs, maxWeekMs, 6);
        return `
                <div class="week-bar-row">
                    <div class="week-label">${formatWeekRange(week)}</div>
                    <div class="week-track">
                        <div class="week-bar" style="width: ${width}%"></div>
                    </div>
                    <div class="week-value">${formatDuration(week.totalMs)}</div>
                </div>
            `;
    }).join("");
}

function renderStackedWeeks(weeks) {
    const maxDayMs = Math.max(0, ...weeks.flatMap(week => week.days.map(day => day.totalMs)));

    return weeks.map(week => `
            <div class="week-bar-row">
                <div class="week-label">${formatWeekRange(week)}</div>
                <div class="week-stacked-track">
                    ${week.days.map(day => {
                        const opacity = getOpacity(day.totalMs, maxDayMs);
                        return `<div class="week-stacked-day" title="${formatDuration(day.totalMs)}" style="background: rgba(59, 130, 246, ${opacity})"></div>`;
                    }).join("")}
                </div>
                <div class="week-value">${week.gapCount}G</div>
            </div>
        `).join("");
}

function renderWeeklyHeatmap(weeks) {
    const maxDayMs = Math.max(0, ...weeks.flatMap(week => week.days.map(day => day.totalMs)));
    const dayLabels = ["M", "T", "W", "T", "F", "S", "S"];

    return `
            <div class="week-heatmap">
                <div></div>
                ${dayLabels.map(label => `<div class="week-value">${label}</div>`).join("")}
                <div></div>
            </div>
            ${weeks.map(week => `
                <div class="week-heatmap">
                    <div class="week-label">${formatShortDate(week.start)}</div>
                    ${week.days.map(day => {
                        const opacity = getOpacity(day.totalMs, maxDayMs);
                        return `<div class="week-heat-cell" title="${formatDuration(day.totalMs)}" style="background: rgba(29, 78, 216, ${opacity})"></div>`;
                    }).join("")}
                    <div class="week-value">${formatDuration(week.totalMs)}</div>
                </div>
            `).join("")}
        `;
}

function renderLargestGapRanking(weeks) {
    return weeks
        .slice()
        .sort((a, b) => b.largestGapMs - a.largestGapMs)
        .map((week, index) => `
                <div class="week-rank-row">
                    <div class="week-rank-number">${index + 1}</div>
                    <div class="week-label">${formatWeekRange(week)}</div>
                    <div class="week-value">${formatDuration(week.largestGapMs)}</div>
                </div>
            `).join("");
}

function renderDailyTimeline(weeks) {
    const maxDayMs = Math.max(0, ...weeks.flatMap(week => week.days.map(day => day.totalMs)));
    const labels = ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"];

    return weeks.map(week => `
            <div class="week-label">${formatWeekRange(week)} / ${formatDuration(week.totalMs)}</div>
            <div class="week-timeline">
                ${week.days.map((day, index) => {
                    const height = getPercent(day.totalMs, maxDayMs, 0);
                    return `
                        <div class="week-timeline-day">
                            <div class="week-timeline-fill" style="height: ${height}%"></div>
                            <div class="week-timeline-label">${labels[index]}<br>${formatDuration(day.totalMs)}</div>
                        </div>
                    `;
                }).join("")}
            </div>
        `).join("");
}

function getPercent(value, maxValue, minimum) {
    if (value <= 0 || maxValue <= 0) return 0;
    return Math.max(minimum, Math.round((value / maxValue) * 100));
}

function getOpacity(value, maxValue) {
    if (value <= 0 || maxValue <= 0) return 0.12;
    return Math.max(0.25, Math.min(0.95, value / maxValue));
}

function getWeekStart(date) {
    const weekStart = new Date(date);
    weekStart.setHours(0, 0, 0, 0);
    const day = weekStart.getDay() || 7;
    weekStart.setDate(weekStart.getDate() - day + 1);
    return weekStart;
}

function getWeekdayIndex(date) {
    return (date.getDay() + 6) % 7;
}

function formatDuration(ms) {
    if (ms <= 0) return "0H";

    const days = Math.floor(ms / dayMs);
    const hours = Math.floor((ms % dayMs) / hourMs);
    const minutes = Math.floor((ms % hourMs) / minuteMs);

    if (days > 0) return `${days}D ${hours}H`;
    if (hours > 0) return `${hours}H ${minutes}M`;
    return `${minutes}M`;
}

function formatShortDate(date) {
    const pad = (n) => String(n).padStart(2, "0");
    return `${pad(date.getDate())}/${pad(date.getMonth() + 1)}`;
}

function formatWeekRange(week) {
    return `${formatShortDate(week.start)} - ${formatShortDate(week.end)}`;
}

function findProjectForEntry(entry) {
    return projects.find(project => project.task?.some(task => task.id === entry.taskId));
}

function hasOpenTaskForUser(project, userId) {
    return project.task?.some(task =>
        task.status !== "CLOSED" &&
        [...(task.scheduled || []), ...(task.actives || [])].some(user => user.id === userId)
    );
}

function populateProjectFilter() {
    const select = document.getElementById("projectFilter");
    const previousValue = selectedProject;
    const projectIdsWithEntries = new Set(
        timeEntries.map(entry => findProjectForEntry(entry)?.id).filter(id => id != null)
    );
    const entryProjects = projects
        .filter(project => projectIdsWithEntries.has(project.id))
        .sort((a, b) => a.title.localeCompare(b.title));

    select.innerHTML = "";

    const allOption = document.createElement("option");
    allOption.value = "all";
    allOption.textContent = "ALL PROJECTS";
    select.appendChild(allOption);

    if (entryProjects.some(project => hasOpenTaskForUser(project, selectedUserId))) {
        const openOption = document.createElement("option");
        openOption.value = "open";
        openOption.textContent = "ALL OPEN PROJECTS";
        select.appendChild(openOption);
    }

    entryProjects.forEach(project => {
        const option = document.createElement("option");
        option.value = String(project.id);
        option.textContent = project.title +
            (hasOpenTaskForUser(project, selectedUserId) ? " (OPEN TASK)" : "");
        select.appendChild(option);
    });

    if ([...select.options].some(option => option.value === previousValue)) {
        select.value = previousValue;
    } else {
        selectedProject = "all";
        select.value = selectedProject;
    }
}

async function updateEntry(id, startTime, endTime, input) {
    const entry = timeEntries.find(e => e.id === id);
    if (startTime != null) entry.startTime = startTime;
    if (endTime != null) entry.endTime = endTime;
    renderGapGraph(getVisibleEntries());

    const row = input.closest(".grid");
    const updateBtn = row.querySelector(".update-button");
    if (updateBtn) {
        updateBtn.classList.remove("hidden");
    }
}

async function update(id, input) {
    const entry = timeEntries.find(e => e.id === id);
    const updated = await updateTimeEntry(entry.id, new Date(entry.startTime).toISOString(), new Date(entry.endTime).toISOString());
    if (!updated) return;

    entry.attention = false;
    input.classList.add("hidden");

    const row = input.closest(".grid");
    if (row.classList.contains("from-yellow-100")) {
        row.classList.remove("from-yellow-100", "via-yellow-200", "to-yellow-300");
        row.classList.add("from-gray-100", "via-gray-200", "to-gray-300");
    }

    renderEntries();
}

async function deleteEntry(id) {
    if (!confirm("Delete?")) return;

    const deleted = await removeTimeEntry(id);
    if (!deleted) return;

    timeEntries = timeEntries.filter(entry => entry.id !== id);
    populateProjectFilter();
    renderEntries();
}

async function loadEntries() {
    const params = new URLSearchParams(window.location.search);
    if (!params.has("id")) return;

    selectedUserId = Number(params.get("id"));
    timeEntries = await getTimeEntries(selectedUserId) || [];
    timeEntries.sort((a, b) => b.id - a.id);
    projects = await getAllProjects();

    populateProjectFilter();
    renderEntries();
}

initModalDismiss(["userModal", "passwordModal"]);
loadEntries();
