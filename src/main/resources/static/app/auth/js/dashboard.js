const token = localStorage.getItem("token");
const API_ROOT = "http://localhost:8080";

const isInsideProfileFolder = window.location.pathname.includes("/profile/");

if (!token) {
    if (isInsideProfileFolder) {
        window.location.href = "../auth/login.html";
    } else {
        window.location.href = "login.html";
    }
}

const email = localStorage.getItem("email");
const welcomeText = document.getElementById("welcomeText");

if (welcomeText && email) {
    welcomeText.textContent = `Welcome back, ${email}`;
}

const menuBtn = document.getElementById("menuBtn");
const sidebar = document.querySelector(".sidebar");
const overlay = document.getElementById("sidebarOverlay");

if (menuBtn && sidebar && overlay) {
    menuBtn.addEventListener("click", () => {
        sidebar.classList.add("show");
        overlay.classList.add("show");
    });

    overlay.addEventListener("click", () => {
        sidebar.classList.remove("show");
        overlay.classList.remove("show");
    });
}

const logoutBtn = document.getElementById("logoutBtn");

if (logoutBtn) {
    logoutBtn.addEventListener("click", () => {
        localStorage.removeItem("token");
        localStorage.removeItem("email");
        localStorage.removeItem("registeredEmail");
        localStorage.removeItem("userId");
        localStorage.removeItem("name");

        if (isInsideProfileFolder) {
            window.location.href = "../auth/login.html";
        } else {
            window.location.href = "login.html";
        }
    });
}

function goToCreateGroup() {
    localStorage.setItem("openCreateGroupModal", "true");
    localStorage.setItem("cameFromDashboard", "true");
    window.location.href = "../group/my-group.html";
}

function formatCurrency(amount) {
    return `₹${Number(amount || 0).toLocaleString("en-IN")}`;
}

function formatDate(value) {
    if (!value) return "";
    const d = new Date(`${value}T00:00:00`);
    if (Number.isNaN(d.getTime())) return value;
    return d.toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" });
}

function formatMonth(value) {
    if (!value) return "";
    const d = new Date(`${value}T00:00:00`);
    if (Number.isNaN(d.getTime())) return value;
    return d.toLocaleDateString("en-IN", { month: "long", year: "numeric" });
}

async function api(url) {
    const response = await fetch(url, { headers: { "Authorization": `Bearer ${token}` }});
    const result = await response.json();
    if (!response.ok || result.success === false) throw new Error(result.message || "Request failed");
    return result.data;
}

async function loadDashboardStats() {
    try {
        const data = await api(`${API_ROOT}/api/groups/dashboard-stats`);
        if (data) {
            document.getElementById("totalGroups").innerText = data.totalGroups || 0;
            document.getElementById("monthlySavings").innerText = formatCurrency(data.monthlySavings);
            document.getElementById("activeLoan").innerText = formatCurrency(data.activeLoan);
            document.getElementById("pendingDues").innerText = formatCurrency(data.pendingDues);
        }
    } catch (error) {
        console.log("Failed to load dashboard stats", error);
    }
}

async function loadRecentActivities() {
    const box = document.getElementById("recentActivities");
    if (!box) return;
    try {
        const groups = await api(`${API_ROOT}/api/groups/my-groups`);
        const activities = [];

        for (const g of (groups || [])) {
            const typeLabel = g.groupType === "SAVINGS" ? "Savings group" : "Loan group";
            activities.push({
                date: g.startDate || "",
                title: `${typeLabel} created`,
                text: `${g.groupName} • ${g.totalMembers || 0}/${g.targetMembers || 20} members • ${g.groupStatus}`,
                amount: ""
            });

            try {
                const history = await api(`${API_ROOT}/api/payments/group/${g.groupId}/my-history`);
                (history || []).slice(0, 3).forEach(p => {
                    activities.push({
                        date: p.paidDate || p.paymentMonth || "",
                        title: `${g.groupType === "SAVINGS" ? "Savings" : "EMI"} paid`,
                        text: `${p.groupName || g.groupName} • ${formatMonth(p.paymentMonth)}${p.notes ? " • " + p.notes : ""}`,
                        amount: formatCurrency(p.amount)
                    });
                });
            } catch (_e) {}
        }

        activities.sort((a, b) => String(b.date).localeCompare(String(a.date)));
        const limited = activities.slice(0, 6);

        if (limited.length === 0) {
            box.innerHTML = `<p class="empty-text">No recent activities yet.</p>`;
            return;
        }

        box.innerHTML = limited.map(a => `
            <div class="activity-item">
                <div>
                    <strong>${a.title}</strong>
                    <span>${a.text}</span>
                    <small>${formatDate(a.date)}</small>
                </div>
                ${a.amount ? `<b>${a.amount}</b>` : ""}
            </div>
        `).join("");
    } catch (error) {
        box.innerHTML = `<p class="empty-text">Unable to load recent activities.</p>`;
    }
}

function goToInvitations() {
    localStorage.setItem("openInvitations", "true");
    window.location.href = "../group/my-group.html";
}

function goToSavings() {
    window.location.href = "../savings/savings.html";
}

function goToLoans() {
    window.location.href = "../loans/loans.html";
}

loadDashboardStats();
loadRecentActivities();
window.addEventListener("storage", event => {
    if (event.key === "groupFundLastPaymentUpdate") {
        loadDashboardStats();
        loadRecentActivities();
    }
});
document.addEventListener("visibilitychange", () => {
    if (!document.hidden) {
        loadDashboardStats();
        loadRecentActivities();
    }
});
setInterval(() => {
    loadDashboardStats();
    loadRecentActivities();
}, 8000);
