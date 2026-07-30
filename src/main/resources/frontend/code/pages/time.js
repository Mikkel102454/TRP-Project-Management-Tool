let users = [];

function renderUsers() {
    const container = document.getElementById("userList");
    container.innerHTML = "";

    users.forEach(user => {
        const div = document.createElement("div");

        div.className = `
        grid grid-cols-12 items-center
        h-16 px-3
        bg-gradient-to-r from-gray-100 via-gray-200 to-gray-300
        border border-gray-500
        hover:bg-gray-300
        cursor-pointer
        `;

        div.innerHTML = `
        <div class="col-span-6 flex items-center gap-3">
            <div class="w-8 h-8 border border-gray-600 bg-gray-400 flex items-center justify-center text-xs font-bold">
                ${user.initial || user.username[0].toUpperCase()}
            </div>
            <div class="truncate text-gray-900 tracking-wide">
                ${user.username}
            </div>
        </div>

        <div class="col-span-3 text-xs text-gray-700">
            ${user.isAdmin ? "ADMIN" : "USER"}
        </div>
        `;

        div.onclick = () => window.location.href = `timetable?id=${user.id}`;
        container.appendChild(div);
    });
}

async function loadUsers() {
    users = await getAllUsers();
    renderUsers();
}

initModalDismiss(["userModal"]);
initForcedClockoutCheck();
loadUsers();
