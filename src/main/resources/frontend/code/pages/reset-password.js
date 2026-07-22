const token = new URLSearchParams(window.location.search).get("token");

async function confirmReset() {
    const status = document.getElementById("resetStatus");
    const newPassword = document.getElementById("newPassword").value;

    status.classList.remove("hidden");

    if (!token) {
        status.innerText = "Reset link is missing a token.";
        return;
    }

    try {
        const response = await fetch(`${API_ROOT}/public/password-reset/confirm`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                token: token,
                newPassword: newPassword
            })
        });

        const data = await response.json();
        if (data.success) {
            status.innerText = "Password updated. You can now log in.";
        } else {
            status.innerText = data.error?.message || "Could not reset password.";
        }
    } catch (e) {
        status.innerText = "Could not reset password.";
    }
}
