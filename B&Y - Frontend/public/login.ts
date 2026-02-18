document.addEventListener("DOMContentLoaded", () => {
    const loginForm = document.getElementById("login-form") as HTMLFormElement;
    loginForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        const username = (document.getElementById("username") as HTMLInputElement).value;
        const password = (document.getElementById("password") as HTMLInputElement).value;
        try {
            const response = await fetch(BACKEND_URL + "/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ username, password }),
            });
            if (response.ok) {
                window.location.href = "/albums";
            } else {
                showToast("Invalid credentials");
            }
        } catch (error) {
            showToast("Network error");
        }
    });
});

function showToast(message: string) {
    const toastMessage = document.getElementById("toastMessage");
    if (toastMessage) toastMessage.textContent = message;
    // @ts-ignore
    const toast = new bootstrap.Toast(document.getElementById("loginToast"));
    toast.show();
}
