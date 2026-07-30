let users = [];

function openCreateModal() {
    openModal();

    document.getElementById("modalTitle").innerText = "Create User";
    document.getElementById("userId").value = "";
    document.getElementById("usernameInput").value = "";
    document.getElementById("passwordInput").value = "";
    document.getElementById("emailInput").value = "";
    document.getElementById("passwordInput").placeholder = "At least 3 characters or more";
    document.getElementById("roleInput").value = "USER";
    document.getElementById("enabledInput").checked = true;
}

function openEditModal(user) {
    openModal();

    document.getElementById("modalTitle").innerText = "Edit User";
    document.getElementById("userId").value = user.id;
    document.getElementById("usernameInput").value = user.username;
    document.getElementById("passwordInput").value = "";
    document.getElementById("emailInput").value = user.email || "";
    document.getElementById("passwordInput").placeholder = "Leave empty to keep current";
    document.getElementById("roleInput").value = user.isAdmin ? "ADMIN" : "USER";
    document.getElementById("enabledInput").checked = user.isEnabled;
}

function openModal() {
    const modal = document.getElementById("userModal");
    modal.classList.remove("hidden");
    modal.classList.add("flex");
}

function closeModal() {
    const modal = document.getElementById("userModal");
    modal.classList.add("hidden");
    modal.classList.remove("flex");
}

async function saveUser() {
    const id = document.getElementById("userId").value;
    const username = document.getElementById("usernameInput").value;
    const password = document.getElementById("passwordInput").value;
    const email = document.getElementById("emailInput").value;
    const role = document.getElementById("roleInput").value;
    const isEnabled = document.getElementById("enabledInput").checked;

    if (id) {
        await updateUser(id, username, password, email, role === "ADMIN", isEnabled);
    } else {
        await createUser(username, password, email, role === "ADMIN", isEnabled);
    }

    closeModal();
    loadUsers();
}

async function deleteUser() {
    const id = document.getElementById("userId").value;
    if (!id) return;

    const confirmDelete = confirm("Are you sure you want to delete this user?");
    if (!confirmDelete) return;

    closeModal();
    loadUsers();
}

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
        <div class="col-span-6 flex items-center gap-3 min-w-0">
            <div class="w-8 h-8 border border-gray-600 bg-gray-400 flex items-center justify-center text-xs font-bold">
                ${user.initial || user.username[0].toUpperCase()}
            </div>
            <div class="min-w-0">
                <div class="truncate text-gray-900 tracking-wide">
                    ${user.username}
                </div>
                <div class="truncate text-[11px] text-gray-600">
                    ${user.email || ""}
                </div>
            </div>
        </div>

        <div class="col-span-3 text-xs text-gray-700">
            ${user.isAdmin ? "ADMIN" : "USER"}
        </div>

        <div class="col-span-2 flex items-center">
            ${user.isEnabled ? renderLed("green") : renderLed("off")}
        </div>

        <div class="col-span-1 text-right text-gray-600 text-xs">
            EDIT
        </div>
        `;

        div.onclick = () => openEditModal(user);
        container.appendChild(div);
    });
}

async function loadUsers() {
    users = await getAllUsers();
    renderUsers();
}

initModalDismiss(["userModal"]);
window.addEventListener("keydown", function(e) {
    if (e.key === "Escape") closeModal();
});
initForcedClockoutCheck();
loadUsers();
