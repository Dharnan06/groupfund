const loginForm = document.getElementById("loginForm");

if (loginForm) {
    loginForm.addEventListener("submit", async function (e) {
        e.preventDefault();

        const data = {
            email: document.getElementById("loginEmail").value,
            password: document.getElementById("loginPassword").value
        };

        const message = document.getElementById("loginMessage");

        try {
            const response = await fetch(`${API_BASE_URL}/auth/login`, {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(data)
            });

            const result = await response.json();

            if (response.ok) {
                localStorage.setItem("token", result.data.token);
                localStorage.setItem("email", result.data.email);
                localStorage.setItem("userId", result.data.userId);
                localStorage.setItem("name", result.data.name);

                message.textContent = result.message;
                message.className = "message success";

                setTimeout(() => {
                    window.location.href = "dashboard.html";
                }, 800);
            } else {
                message.textContent = result.message || "Login failed";
                message.className = "message error";

                const forgotPasswordLink = document.getElementById("forgotPasswordLink");
                if (forgotPasswordLink) {
                    forgotPasswordLink.style.display = "block";
                }
            }

        } catch (error) {
            message.textContent = "Something went wrong. Please try again.";
            message.className = "message error";

            const forgotPasswordLink = document.getElementById("forgotPasswordLink");
            if (forgotPasswordLink) {
                forgotPasswordLink.style.display = "block";
            }
        }
    });
}