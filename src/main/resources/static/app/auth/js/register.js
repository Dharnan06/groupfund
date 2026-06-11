const registerForm = document.getElementById("registerForm");

if (registerForm) {
    registerForm.addEventListener("submit", async function (e) {
        e.preventDefault();

        const data = {
            name: document.getElementById("name").value,
            mobileNumber: document.getElementById("mobileNumber").value,
            email: document.getElementById("email").value,
            password: document.getElementById("password").value,
            confirmPassword: document.getElementById("confirmPassword").value
        };

        const message = document.getElementById("message");

        const registerBtn = document.querySelector("#registerForm button");
        registerBtn.disabled = true;
        registerBtn.textContent = "Sending OTP...";
        message.textContent = "Please wait. We are sending OTP to your email.";
        message.className = "message success";

        try {
            const response = await fetch(`${API_BASE_URL}/auth/register`, {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(data)
            });

            const result = await response.json();

            if (response.ok) {
                localStorage.setItem("registeredEmail", data.email);
                message.textContent = result.message;
                message.className = "message success";

                setTimeout(() => {
                    window.location.href = "otp.html";
                }, 1000);
            } else {
                message.textContent = result.message || "Registration failed";
                message.className = "message error";
                registerBtn.disabled = false;
                registerBtn.textContent = "Register";
            }
        } catch (error) {
            message.textContent = "Something went wrong. Please try again.";
            message.className = "message error";
            registerBtn.disabled = false;
            registerBtn.textContent = "Register";
        }
    });
}