async function loadHeader() {
    const headerIcon = document.getElementById("headerIcon");

    const cachedInitial = localStorage.getItem("userInitial");
    if (cachedInitial) {
        headerIcon.innerText = cachedInitial;
    }

    const user = await getUser();
    if (!user) return;

    const initial = user.initial?.toUpperCase() || "";

    headerIcon.innerText = initial;
    localStorage.setItem("userInitial", initial);
}

loadHeader();

function toggleUserMenu() {
    const menu = document.getElementById("userDropdown");
    menu.classList.toggle("hidden");
}

document.addEventListener("click", function (e) {
    const menu = document.getElementById("userDropdown");
    const icon = document.getElementById("headerIcon");

    if (!menu || !icon) return;

    if (!menu.contains(e.target) && !icon.contains(e.target)) {
        menu.classList.add("hidden");
    }
});