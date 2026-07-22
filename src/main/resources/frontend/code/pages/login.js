function toggleResetForm() {
    document.getElementById("resetForm").classList.toggle("hidden");
}

async function requestReset() {
    const status = document.getElementById("resetStatus");
    status.classList.remove("hidden");
    status.innerText = "If that email exists, a reset link has been sent.";

    try {
        await fetch(`${API_ROOT}/public/password-reset/request`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                email: document.getElementById("resetEmail").value
            })
        });
    } catch (e) {
        status.innerText = "If that email exists, a reset link has been sent.";
    }
}
