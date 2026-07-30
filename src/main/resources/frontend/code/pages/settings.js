function setSettingsStatus(elementId, message, isError = false) {
    const status = document.getElementById(elementId);
    status.textContent = message;
    status.style.color = isError ? "#991b1b" : "#166534";
}

function setButtonBusy(buttonId, busy) {
    const button = document.getElementById(buttonId);
    button.disabled = busy;
    button.style.opacity = busy ? "0.6" : "1";
}

async function initializeSettings() {
    const user = await getUser();
    if (!user) return;

    document.getElementById("emailInput").value = user.email || "";
}

async function saveEmail(event) {
    event.preventDefault();

    const emailInput = document.getElementById("emailInput");
    const email = emailInput.value.trim();

    if (email !== "" && !emailInput.checkValidity()) {
        setSettingsStatus("emailStatus", "Enter a valid email address.", true);
        return;
    }

    setButtonBusy("emailSaveButton", true);
    setSettingsStatus("emailStatus", "");

    const result = await updateCurrentUserEmail(email);

    if (result.success) {
        const updatedUser = User.fromJson(result.data);
        UserDto = updatedUser;
        window.__BOOTSTRAP__.currentUser = result.data;
        emailInput.value = updatedUser.email || "";
        setSettingsStatus(
            "emailStatus",
            updatedUser.email ? "Email saved." : "Email removed."
        );
    } else {
        setSettingsStatus(
            "emailStatus",
            result.error?.message || "Could not update email.",
            true
        );
    }

    setButtonBusy("emailSaveButton", false);
}

async function savePassword(event) {
    event.preventDefault();

    const oldPasswordInput = document.getElementById("oldPasswordInput");
    const newPasswordInput = document.getElementById("newPasswordInput");
    const confirmPasswordInput = document.getElementById("confirmPasswordInput");
    const oldPassword = oldPasswordInput.value;
    const newPassword = newPasswordInput.value;

    if (!oldPassword) {
        setSettingsStatus("passwordStatus", "Enter your current password.", true);
        return;
    }

    if (newPassword.length < 3) {
        setSettingsStatus("passwordStatus", "New password must be at least 3 characters.", true);
        return;
    }

    if (newPassword !== confirmPasswordInput.value) {
        setSettingsStatus("passwordStatus", "New passwords do not match.", true);
        return;
    }

    setButtonBusy("passwordSaveButton", true);
    setSettingsStatus("passwordStatus", "");

    const result = await changeCurrentPassword(oldPassword, newPassword);

    if (result.success) {
        oldPasswordInput.value = "";
        newPasswordInput.value = "";
        confirmPasswordInput.value = "";
        setSettingsStatus("passwordStatus", "Password changed.");
    } else {
        setSettingsStatus(
            "passwordStatus",
            result.error?.message || "Could not update password.",
            true
        );
    }

    setButtonBusy("passwordSaveButton", false);
}

initializeSettings();
initForcedClockoutCheck();
