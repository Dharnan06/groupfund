const forgotPasswordLink = document.getElementById("forgotPasswordLink");
const openForgotModal = document.getElementById("openForgotModal");
const forgotModal = document.getElementById("forgotModal");
const closeForgotModal = document.getElementById("closeForgotModal");

const forgotEmail = document.getElementById("forgotEmail");
const sendForgotOtpBtn = document.getElementById("sendForgotOtpBtn");
const forgotOtpSection = document.getElementById("forgotOtpSection");
const verifyForgotOtpBtn = document.getElementById("verifyForgotOtpBtn");

const resetPasswordSection = document.getElementById("resetPasswordSection");
const resetPasswordBtn = document.getElementById("resetPasswordBtn");
const forgotMessage = document.getElementById("forgotMessage");

const forgotOtpInputs = document.querySelectorAll("#forgotOtpSection .otp-inputs input");

if (openForgotModal) {
    openForgotModal.addEventListener("click", function (e) {
        e.preventDefault();

        forgotEmail.value = document.getElementById("loginEmail").value;
        forgotModal.style.display = "flex";
    });
}

if (closeForgotModal) {
    closeForgotModal.addEventListener("click", function () {
        forgotModal.style.display = "none";
    });
}

forgotOtpInputs.forEach((input, index) => {
    input.addEventListener("input", () => {
        if (input.value.length === 1 && index < forgotOtpInputs.length - 1) {
            forgotOtpInputs[index + 1].focus();
        }
    });
});

if (sendForgotOtpBtn) {
    sendForgotOtpBtn.addEventListener("click", async function () {
        try {
            const response = await fetch(`${API_BASE_URL}/auth/forgot-password/send-otp`, {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({
                    email: forgotEmail.value
                })
            });

            const result = await response.json();

            if (response.ok) {
                forgotMessage.textContent = result.message;
                forgotMessage.className = "message success";
                forgotOtpSection.style.display = "block";
            } else {
                forgotMessage.textContent = result.message || "Failed to send OTP";
                forgotMessage.className = "message error";
            }
        } catch (error) {
            forgotMessage.textContent = "Something went wrong. Please try again.";
            forgotMessage.className = "message error";
        }
    });
}

if (verifyForgotOtpBtn) {
    verifyForgotOtpBtn.addEventListener("click", async function () {
        const otp = Array.from(forgotOtpInputs).map(input => input.value).join("");

        try {
            const response = await fetch(`${API_BASE_URL}/auth/forgot-password/verify-otp`, {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({
                    email: forgotEmail.value,
                    otp: Number(otp)
                })
            });

            const result = await response.json();

            if (response.ok) {
                forgotMessage.textContent = result.message;
                forgotMessage.className = "message success";

                forgotOtpSection.style.display = "none";
                sendForgotOtpBtn.style.display = "none";
                forgotEmail.disabled = true;
                resetPasswordSection.style.display = "block";
            } else {
                forgotMessage.textContent = result.message || "Invalid OTP";
                forgotMessage.className = "message error";
            }
        } catch (error) {
            forgotMessage.textContent = "Something went wrong. Please try again.";
            forgotMessage.className = "message error";
        }
    });
}

if (resetPasswordBtn) {
    resetPasswordBtn.addEventListener("click", async function () {
        try {
            const response = await fetch(`${API_BASE_URL}/auth/forgot-password/reset`, {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({
                    email: forgotEmail.value,
                    newPassword: document.getElementById("newPassword").value,
                    confirmPassword: document.getElementById("confirmNewPassword").value
                })
            });

            const result = await response.json();

            if (response.ok) {
                forgotMessage.textContent = result.message;
                forgotMessage.className = "message success";

                setTimeout(() => {
                    forgotModal.style.display = "none";
                    document.getElementById("loginPassword").value = "";
                }, 1200);
            } else {
                forgotMessage.textContent = result.message || "Password reset failed";
                forgotMessage.className = "message error";
            }
        } catch (error) {
            forgotMessage.textContent = "Something went wrong. Please try again.";
            forgotMessage.className = "message error";
        }
    });
}
// Close forgot-password popup when user clicks outside the popup card.
document.querySelectorAll(".modal-overlay").forEach(modal => {
    modal.addEventListener("click", (event) => {
        if (event.target === modal) {
            modal.style.display = "none";
        }
    });
});
