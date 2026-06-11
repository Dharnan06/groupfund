const otpInputs = document.querySelectorAll(".otp-inputs input");
const verifyOtpBtn = document.getElementById("verifyOtpBtn");
const backBtn = document.getElementById("backBtn");
const otpEmailText = document.getElementById("otpEmailText");

const email = localStorage.getItem("registeredEmail");

if (!email) {
    window.location.href = "register.html";
}

if (otpEmailText) {
    otpEmailText.textContent = email;
}

otpInputs.forEach((input, index) => {
    input.addEventListener("input", () => {
        if (input.value.length === 1 && index < otpInputs.length - 1) {
            otpInputs[index + 1].focus();
        }
    });
});

if (backBtn) {
    backBtn.addEventListener("click", () => {
        window.location.href = "register.html";
    });
}

if (verifyOtpBtn) {
    verifyOtpBtn.addEventListener("click", async () => {
        const otp = Array.from(otpInputs).map(input => input.value).join("");
        const otpMessage = document.getElementById("otpMessage");

        try {
            const response = await fetch(`${API_BASE_URL}/auth/verify-otp`, {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({
                    email: email,
                    otp: Number(otp)
                })
            });

            const result = await response.json();

            if (response.ok) {
                localStorage.setItem("token", result.data.token);
                localStorage.setItem("email", result.data.email);
                localStorage.setItem("userId", result.data.userId);
                localStorage.setItem("name", result.data.name);
                window.location.href = "dashboard.html";
            } else {
                otpMessage.textContent = result.message || "Invalid OTP";
                otpMessage.className = "message error";
            }
        } catch (error) {
            otpMessage.textContent = "Something went wrong. Please try again.";
            otpMessage.className = "message error";
        }
    });
}