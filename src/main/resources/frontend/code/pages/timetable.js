const isAdmin = window.__BOOTSTRAP__?.isAdmin === true;

let timeEntries = [];
let projects = [];
let selectedProject = "all";
let selectedUserId = null;
const minimumGapMinutes = 15;
let selectedWeekStartKey = getDateKey(getWeekStart(new Date()));
let selectedGapDayIndex = null;

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
    const title = document.getElementById("gapWeekTitle");
    const count = document.getElementById("gapCount");
    const total = document.getElementById("gapTotal");
    const max = document.getElementById("gapMax");
    const gaps = getTimeGaps(entries)
        .filter(gap => gap.durationMs >= minimumGapMinutes * minuteMs);
    const weeks = getWeeklyGapSummary(gaps);
    const selectedPeriod = getSelectedWeek(weeks);

    graph.innerHTML = "";
    populateWeekFilter(weeks);

    title.textContent = `WEEKLY TIME GAP GRAPH / ${formatWeekLabel(selectedPeriod)}`;
    count.textContent = `${selectedPeriod.gapCount} ${selectedPeriod.gapCount === 1 ? "GAP" : "GAPS"}`;
    total.textContent = `${formatDuration(selectedPeriod.totalMs)} TOTAL`;
    max.textContent = `${formatDuration(selectedPeriod.largestGapMs)} MAX GAP`;

    graph.appendChild(createWeeklySummary(selectedPeriod));
    graph.appendChild(createWeeklyDayGrid(selectedPeriod));
    graph.appendChild(createGapDetail(selectedPeriod));
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
            id: `${sortedEntries[i].id}-${sortedEntries[i + 1].id}`,
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
            getGapDaySegments(gap).forEach(segment => {
                const weekStart = getWeekStart(segment.start);
                const key = getDateKey(weekStart);

                if (!weeksByKey.has(key)) {
                    weeksByKey.set(key, createEmptyWeek(weekStart));
                }

                addGapSegmentToPeriod(weeksByKey.get(key), gap, segment);
            });
        });

    return [...weeksByKey.values()].sort((a, b) => b.start - a.start);
}

function getSelectedWeek(weeks) {
    const selectedWeek = weeks.find(week => week.key === selectedWeekStartKey);
    if (selectedWeek) return selectedWeek;

    const start = parseDateKey(selectedWeekStartKey);
    return createEmptyWeek(start);
}

function createEmptyWeek(start) {
    return {
        key: getDateKey(start),
        start,
        end: new Date(start.getTime() + 6 * dayMs),
        totalMs: 0,
        largestGapMs: 0,
        gapCount: 0,
        gapIds: new Set(),
        days: Array.from({length: 7}, () => ({totalMs: 0, gapCount: 0, gapIds: new Set(), gaps: []}))
    };
}

function addGapSegmentToPeriod(period, gap, segment) {
    const day = getWeekdayIndex(segment.start);

    period.totalMs += segment.durationMs;
    period.largestGapMs = Math.max(period.largestGapMs, gap.durationMs);

    if (!period.gapIds.has(gap.id)) {
        period.gapIds.add(gap.id);
        period.gapCount++;
    }

    period.days[day].totalMs += segment.durationMs;
    period.days[day].gaps.push(segment);

    if (!period.days[day].gapIds.has(gap.id)) {
        period.days[day].gapIds.add(gap.id);
        period.days[day].gapCount++;
    }
}

function getGapDaySegments(gap) {
    const segments = [];
    let segmentStart = new Date(gap.start);
    const gapEnd = new Date(gap.end);

    while (segmentStart < gapEnd) {
        const nextDayStart = new Date(segmentStart);
        nextDayStart.setHours(24, 0, 0, 0);

        const segmentEnd = nextDayStart < gapEnd ? nextDayStart : gapEnd;
        segments.push({
            id: gap.id,
            start: new Date(segmentStart),
            end: new Date(segmentEnd),
            originalStart: gap.start,
            originalEnd: gap.end,
            durationMs: segmentEnd - segmentStart
        });

        segmentStart = segmentEnd;
    }

    return segments;
}

function populateWeekFilter(weeks) {
    const select = document.getElementById("weekFilter");
    if (!select) return;

    const weekOptions = new Map();
    weekOptions.set(selectedWeekStartKey, createEmptyWeek(parseDateKey(selectedWeekStartKey)));
    weeks.forEach(week => weekOptions.set(week.key, week));

    const sortedWeeks = [...weekOptions.values()].sort((a, b) => b.start - a.start);
    select.innerHTML = "";

    sortedWeeks.forEach(week => {
        const option = document.createElement("option");
        option.value = week.key;
        option.textContent = `${formatWeekLabel(week)} (${formatDuration(week.totalMs)})`;
        select.appendChild(option);
    });

    select.value = selectedWeekStartKey;
}

function createWeeklySummary(week) {
    const summary = document.createElement("div");
    summary.className = "weekly-gap-summary";
    summary.innerHTML = `
        <span>${formatWeekLabel(week)}</span>
        <span>${formatWeekRange(week)}</span>
        <span>${week.gapCount} ${week.gapCount === 1 ? "GAP" : "GAPS"}</span>
        <span>${formatDuration(week.totalMs)} TOTAL GAP TIME</span>
        <span>${formatDuration(week.largestGapMs)} LARGEST GAP</span>
    `;
    return summary;
}

function createWeeklyDayGrid(week) {
    if (selectedGapDayIndex == null) {
        selectedGapDayIndex = getDefaultDayIndex(week);
    }

    const maxDayMs = Math.max(0, ...week.days.map(day => day.totalMs));
    const labels = ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"];
    const grid = document.createElement("div");
    grid.className = "weekly-day-grid";

    week.days.forEach((day, index) => {
        const date = new Date(week.start.getTime() + index * dayMs);
        const height = getPercent(day.totalMs, maxDayMs, 0);
        const button = document.createElement("button");
        button.type = "button";
        button.className = `weekly-day-button${index === selectedGapDayIndex ? " weekly-day-selected" : ""}`;
        button.onclick = () => selectGapDay(index);
        button.innerHTML = `
            <div class="weekly-day-fill" style="height: ${height}%"></div>
            <div class="weekly-day-content">
                <span class="weekly-day-name">${labels[index]} ${formatShortDate(date)}</span>
                <span class="weekly-day-total">${formatDuration(day.totalMs)}</span>
                <span class="weekly-day-count">${day.gapCount} ${day.gapCount === 1 ? "GAP" : "GAPS"}</span>
            </div>
        `;
        grid.appendChild(button);
    });

    return grid;
}

function createGapDetail(week) {
    const selectedDay = week.days[selectedGapDayIndex] || week.days[0];
    const selectedDate = new Date(week.start.getTime() + selectedGapDayIndex * dayMs);
    const detailTitle = `${formatDayName(selectedDate)} ${formatShortDate(selectedDate)}`;
    const detail = document.createElement("div");
    detail.className = "weekly-gap-detail";

    detail.innerHTML = `
        <div class="weekly-gap-detail-header">
            <div>${detailTitle}</div>
            <div class="weekly-gap-right">TIME GAP</div>
            <div class="weekly-gap-right">DURATION</div>
        </div>
        ${selectedDay.gaps.length === 0
        ? `<div class="weekly-gap-empty-detail">NO GAPS FOR THIS DAY</div>`
        : selectedDay.gaps
            .slice()
            .sort((a, b) => a.start - b.start)
            .map(gap => `
                <div class="weekly-gap-row">
                    <div class="weekly-gap-range">${formatGapRange(gap)}</div>
                    <div class="weekly-gap-right weekly-gap-muted">${formatTime(gap.start)} - ${formatTime(gap.end)}</div>
                    <div class="weekly-gap-right">${formatDuration(gap.durationMs)}</div>
                </div>
            `).join("")}
    `;

    return detail;
}

function getDefaultDayIndex(week) {
    const today = new Date();
    if (getDateKey(getWeekStart(today)) === week.key) return getWeekdayIndex(today);

    const firstGapDay = week.days.findIndex(day => day.gapCount > 0);
    return firstGapDay >= 0 ? firstGapDay : 0;
}

function selectGapDay(index) {
    selectedGapDayIndex = index;
    renderGapGraph(getVisibleEntries());
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

function getDateKey(date) {
    const pad = (n) => String(n).padStart(2, "0");
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function parseDateKey(key) {
    const [year, month, day] = key.split("-").map(Number);
    return new Date(year, month - 1, day);
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

function formatTime(date) {
    const pad = (n) => String(n).padStart(2, "0");
    return `${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function formatDayName(date) {
    return ["SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"][date.getDay()];
}

function formatGapRange(gap, alwaysShowDate = false) {
    const sameDay = getDateKey(gap.start) === getDateKey(gap.end);
    if (sameDay && alwaysShowDate) return `${formatShortDate(gap.start)} ${formatTime(gap.start)} - ${formatTime(gap.end)}`;
    if (sameDay) return `${formatTime(gap.start)} - ${formatTime(gap.end)}`;
    return `${formatShortDate(gap.start)} ${formatTime(gap.start)} - ${formatShortDate(gap.end)} ${formatTime(gap.end)}`;
}

function formatWeekRange(week) {
    return `${formatShortDate(week.start)} - ${formatShortDate(week.end)}`;
}

function formatWeekLabel(week) {
    const isoWeek = getIsoWeek(week.start);
    return `WEEK ${isoWeek.week}, ${isoWeek.year}`;
}

function getIsoWeek(date) {
    const target = new Date(date);
    target.setHours(0, 0, 0, 0);
    target.setDate(target.getDate() + 3 - ((target.getDay() + 6) % 7));

    const weekYear = target.getFullYear();
    const firstThursday = new Date(weekYear, 0, 4);
    firstThursday.setDate(firstThursday.getDate() + 3 - ((firstThursday.getDay() + 6) % 7));

    const week = 1 + Math.round((target - firstThursday) / (7 * dayMs));
    return {week, year: weekYear};
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
