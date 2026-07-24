function initModalDismiss(modalIds = []) {
    window.addEventListener("click", function(e) {
        if (modalIds.includes(e.target.id)) {
            e.target.remove();
        }
    });

    window.addEventListener("keydown", function(e) {
        if (e.key !== "Escape") return;

        for (const modalId of modalIds) {
            document.getElementById(modalId)?.remove();
        }
    });
}

async function initForcedClockoutCheck() {
    const user = await getUser();
    if (!user?.forcedClockedOut) return;

    const html = await getComponent("timeRegistry");
    const popupHolder = document.getElementById("popupHolder");
    if (popupHolder) {
        popupHolder.innerHTML = html;
        document.getElementById("timeRegistryButton")?.addEventListener("click", () => {
            window.location.href = `timetable?id=${user.id}`;
        });
    }
}
