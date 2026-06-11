const BACKEND_URL = window.location.origin;
const API_BASE_URL = `${BACKEND_URL}/api/groups`;
const FRIENDS_API_BASE_URL = `${BACKEND_URL}/api/friends`;
const PAYMENTS_API_BASE_URL = `${BACKEND_URL}/api/payments`;

const token = localStorage.getItem("token");
const loggedInEmail = localStorage.getItem("email");

const sidebar = document.getElementById("sidebar");
const menuBtn = document.getElementById("menuBtn");

const urlParams = new URLSearchParams(window.location.search);
const groupId = urlParams.get("groupId");

let currentGroupMembers = [];
let currentGroup = null;
let paymentSummaryData = [];

if (!token) {
    window.location.href = "../auth/login.html";
}

if (!groupId) {
    alert("Group ID not found");
    window.location.href = "my-group.html";
}

menuBtn.addEventListener("click", () => {
    sidebar.classList.toggle("show");
});

document.addEventListener("click", (e) => {
    if (
        window.innerWidth <= 768 &&
        !sidebar.contains(e.target) &&
        !menuBtn.contains(e.target)
    ) {
        sidebar.classList.remove("show");
    }
});

function logout() {
    localStorage.removeItem("token");
    localStorage.removeItem("email");
    window.location.href = "../auth/login.html";
}

function goBackToGroups() {
    window.location.href = "my-group.html";
}

function formatGroupType(type) {
    if (type === "SAVINGS") return "Savings Group";
    if (type === "CUSTOM_LOAN") return "Custom Loan Group";
    return type || "-";
}

function formatGroupStatus(status) {
    if (status === "PENDING_MEMBERS") return "Waiting for Members";
    if (status === "READY_TO_ACTIVATE") return "Ready to Activate";
    if (status === "ACTIVATION_PENDING") return "Activation Voting";
    if (status === "ACTIVE") return "Active";
    if (status === "CLOSED") return "Closed";
    return status || "-";
}

function formatPaymentStatus(status) {
    if (status === "PAID") return "Paid";
    if (status === "PARTIAL") return "Partial";
    if (status === "PENDING") return "Pending";
    return status || "Pending";
}

function formatCurrency(amount) {
    return `₹${Number(amount || 0).toLocaleString("en-IN")}`;
}

function isCurrentUserLeader(group = currentGroup) {
    if (!group) return false;
    const loggedInUserId = Number(group.currentUserId || localStorage.getItem("userId") || 0);
    return group.leader === true
        || group.currentUserRole === "LEADER"
        || (group.leaderUserId && loggedInUserId && Number(group.leaderUserId) === loggedInUserId);
}

function getSelectedPaymentSummary() {
    const selectedUserId = Number(document.getElementById("paymentUser")?.value || 0);
    return paymentSummaryData.find(item => Number(item.userId) === selectedUserId);
}

function getAcceptedMemberCount(group = currentGroup) {
    const fromGroup = Number(group?.totalMembers || 0);
    const fromMembers = (currentGroupMembers || []).filter(member => member.status === "ACCEPTED").length;
    const fromSummary = (paymentSummaryData || []).length;
    return Math.max(fromGroup, fromMembers, fromSummary);
}

function syncPaymentUserDropdownFromSummary() {
    const paymentUser = document.getElementById("paymentUser");
    if (!paymentUser || !paymentSummaryData || paymentSummaryData.length === 0) return;

    const previousValue = paymentUser.value;
    const pendingMember = paymentSummaryData.find(item => Number(item.pendingAmount || 0) > 0);
    const defaultValue = previousValue || (pendingMember ? pendingMember.userId : paymentSummaryData[0].userId);

    paymentUser.innerHTML = paymentSummaryData.map(item => `
        <option value="${item.userId}">${item.fullName || "No Name"} (${item.role})</option>
    `).join("");

    paymentUser.value = String(defaultValue);
    if (!paymentUser.value && paymentSummaryData.length > 0) {
        paymentUser.value = String(paymentSummaryData[0].userId);
    }
}

function updatePaymentFormState() {
    const button = document.getElementById("recordPaymentBtn");
    const message = document.getElementById("paymentMessage");
    const amountInput = document.getElementById("paymentAmount");
    if (!button || !amountInput) return;

    const summary = getSelectedPaymentSummary();
    const expected = Number(summary?.expectedAmount || getExpectedMonthlyAmount() || 0);
    const pending = Number(summary?.pendingAmount ?? expected);

    amountInput.value = expected;

    if (currentGroup && currentGroup.groupStatus !== "ACTIVE") {
        button.disabled = true;
        button.textContent = "Group Not Active";
        if (message) {
            message.textContent = "Payments are locked until the group becomes active.";
            message.className = "payment-message error";
        }
    } else if (summary && pending <= 0) {
        button.disabled = true;
        button.textContent = "Already Paid";
        if (message) {
            message.textContent = "Payment already recorded for this member and month.";
            message.className = "payment-message success";
        }
    } else {
        button.disabled = false;
        button.textContent = "Record Payment";
        if (message && message.textContent.includes("already recorded")) {
            message.textContent = "";
            message.className = "payment-message";
        }
    }
}

function formatDate(date) {
    return date || "Not added";
}

function getActivationText(group) {
    if (!group) return "-";
    if (group.groupStatus === "ACTIVE") return "Active - payments are unlocked";
    if (group.groupStatus === "ACTIVATION_PENDING") return `${group.activationApprovedCount || 0}/${group.activationRequiredCount || group.totalMembers || 0} members approved`;
    if (group.groupStatus === "READY_TO_ACTIVATE") return "Ready - leader can request activation";
    return `Waiting for at least 5 accepted members`;
}

function renderActivationActions(group) {
    if (!group) return "";
    if (group.groupStatus === "ACTIVE") return `<div class="activation-box success">Group is active. Leader can now record payments.</div>`;
    if (group.groupStatus === "PENDING_MEMBERS") return `<div class="activation-box warning">Need at least 5 accepted members before activation. Current: ${group.totalMembers}/${group.targetMembers || 20}</div>`;
    if (group.groupStatus === "READY_TO_ACTIVATE" && isCurrentUserLeader(group)) return `<button class="invite-btn" onclick="requestActivation()">Request Group Activation</button>`;
    if (group.groupStatus === "READY_TO_ACTIVATE") return `<div class="activation-box warning">Waiting for leader to request activation.</div>`;
    if (group.groupStatus === "ACTIVATION_PENDING" && !group.currentUserActivationApproved) return `<button class="invite-btn" onclick="approveActivation()">Approve Activation</button>`;
    if (group.groupStatus === "ACTIVATION_PENDING") return `<div class="activation-box success">You approved activation. Waiting for other members.</div>`;
    return "";
}

async function requestActivation() {
    try {
        const response = await fetch(`${API_BASE_URL}/${groupId}/activation/request`, { method: "POST", headers: { "Authorization": `Bearer ${token}` } });
        const result = await response.json();
        alert(result.message);
        await loadGroupDetails();
    } catch (error) { alert("Failed to request activation."); }
}

async function approveActivation() {
    try {
        const response = await fetch(`${API_BASE_URL}/${groupId}/activation/approve`, { method: "POST", headers: { "Authorization": `Bearer ${token}` } });
        const result = await response.json();
        alert(result.message);
        await loadGroupDetails();
    } catch (error) { alert("Failed to approve activation."); }
}

function showTab(tabName) {
    document.querySelectorAll(".tab-content").forEach(tab => {
        tab.classList.add("hidden");
    });

    document.querySelectorAll(".tab-btn").forEach(btn => {
        btn.classList.remove("active");
    });

    document.getElementById(`${tabName}Tab`).classList.remove("hidden");

    const activeButton = Array.from(document.querySelectorAll(".tab-btn"))
        .find(btn => btn.getAttribute("onclick") === `showTab('${tabName}')`);

    if (activeButton) {
        activeButton.classList.add("active");
    }

    if (tabName === "payments") {
        loadPayments();
    }
}

function openInviteFriendModal() {
    document.getElementById("inviteFriendModal").style.display = "flex";
    loadFriendsForInvite();
}

function closeInviteFriendModal() {
    document.getElementById("inviteFriendModal").style.display = "none";
}

async function loadFriendsForInvite() {
    const friendsInviteList = document.getElementById("friendsInviteList");

    try {
        const response = await fetch(`${FRIENDS_API_BASE_URL}/list`, {
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        const result = await response.json();
        friendsInviteList.innerHTML = "";

        if (!result.data || result.data.length === 0) {
            friendsInviteList.innerHTML = `<p class="empty-text">No friends available to invite.</p>`;
            return;
        }

        const availableFriends = result.data.filter(friend =>
            !currentGroupMembers.some(member => member.userId === friend.userId)
        );

        if (currentGroup && currentGroup.groupStatus === "ACTIVATION_PENDING") {
            friendsInviteList.innerHTML = `<p class="empty-text">Activation voting is in progress. Complete activation before inviting more members.</p>`;
            return;
        }

        const limit = currentGroup?.targetMembers || 20;
        const usedSlots = currentGroupMembers.filter(member => member.status === "ACCEPTED" || member.status === "PENDING").length;
        if (usedSlots >= limit) {
            friendsInviteList.innerHTML = `<p class="empty-text">This group reached its member limit of ${limit}.</p>`;
            return;
        }

        if (availableFriends.length === 0) {
            friendsInviteList.innerHTML = `<p class="empty-text">All your friends are already in this group.</p>`;
            return;
        }

        friendsInviteList.innerHTML = availableFriends.map(friend => `
            <div class="invite-friend-row">
                <div class="invite-friend-info">
                    <h4>${friend.fullName || "No Name"}</h4>
                    <p>Mobile: ${friend.mobileNumber || ""}</p>
                    <p>District: ${friend.district || "District not added"}</p>
                </div>

                <button class="send-invite-btn" onclick="sendGroupInvite(${friend.userId})">
                    Invite
                </button>
            </div>
        `).join("");

    } catch (error) {
        friendsInviteList.innerHTML = `<p class="empty-text">Failed to load friends.</p>`;
    }
}

async function sendGroupInvite(userId) {
    try {
        const response = await fetch(`${API_BASE_URL}/invite`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`
            },
            body: JSON.stringify({
                groupId: Number(groupId),
                userId: userId
            })
        });

        const result = await response.json();
        alert(result.message);

        await loadGroupDetails();
        await loadFriendsForInvite();

    } catch (error) {
        alert("Failed to send group invitation.");
    }
}

function getExpectedMonthlyAmount() {
    if (!currentGroup) return 0;

    if (currentGroup.groupType === "SAVINGS" && currentGroup.savingsDetails) {
        return currentGroup.savingsDetails.monthlySavingsAmount || 0;
    }

    if (currentGroup.groupType === "CUSTOM_LOAN" && currentGroup.loanDetails) {
        return currentGroup.loanDetails.monthlyEmi || 0;
    }

    return 0;
}

function renderFinanceDetails(group) {
    const financeContent = document.getElementById("financeContent");

    if (group.groupType === "SAVINGS" && group.savingsDetails) {
        financeContent.innerHTML = `
            <div class="finance-grid">
                <div class="finance-item">
                    <span>Monthly Savings</span>
                    <strong>${formatCurrency(group.savingsDetails.monthlySavingsAmount)}</strong>
                </div>
                <div class="finance-item">
                    <span>Savings Start Date</span>
                    <strong>${formatDate(group.savingsDetails.savingsStartDate)}</strong>
                </div>
                <div class="finance-item">
                    <span>Accepted Members</span>
                    <strong>${getAcceptedMemberCount(group)}</strong>
                </div>
                <div class="finance-item">
                    <span>Monthly Group Collection</span>
                    <strong>${formatCurrency((group.savingsDetails.monthlySavingsAmount || 0) * group.totalMembers)}</strong>
                </div>
            </div>
        `;
        return;
    }

    if (group.groupType === "CUSTOM_LOAN" && group.loanDetails) {
        financeContent.innerHTML = `
            <div class="finance-grid">
                <div class="finance-item">
                    <span>Loan Source</span>
                    <strong>${group.loanDetails.loanSource || "Not added"}</strong>
                </div>
                <div class="finance-item">
                    <span>Loan Amount</span>
                    <strong>${formatCurrency(group.loanDetails.loanAmount)}</strong>
                </div>
                <div class="finance-item">
                    <span>Monthly EMI</span>
                    <strong>${formatCurrency(group.loanDetails.monthlyEmi)}</strong>
                </div>
                <div class="finance-item">
                    <span>Duration</span>
                    <strong>${group.loanDetails.durationMonths || 0} months</strong>
                </div>
                <div class="finance-item">
                    <span>Interest Rate</span>
                    <strong>${group.loanDetails.interestRate || 0}%</strong>
                </div>
                <div class="finance-item">
                    <span>Loan Start Date</span>
                    <strong>${formatDate(group.loanDetails.loanStartDate)}</strong>
                </div>
            </div>
        `;
        return;
    }

    financeContent.innerHTML = `<p class="empty-text">Finance details are not available for this group.</p>`;
}

function populatePaymentMembers() {
    const paymentUser = document.getElementById("paymentUser");
    if (!paymentUser) return;

    const acceptedMembers = currentGroupMembers.filter(member => member.status === "ACCEPTED");

    paymentUser.innerHTML = acceptedMembers.map(member => `
        <option value="${member.userId}">${member.fullName || "No Name"} (${member.role})</option>
    `).join("");

    if (!document.getElementById("paymentMonth").value) {
        const today = new Date();
        document.getElementById("paymentMonth").value =
            `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}`;
    }

    setDefaultPaymentAmount();
}

function setDefaultPaymentAmount() {
    updatePaymentFormState();
}

function getSelectedPaymentMonthDate() {
    const monthValue = document.getElementById("paymentMonth").value;
    return monthValue ? `${monthValue}-01` : null;
}

async function loadPaymentSummary() {
    const paymentSummary = document.getElementById("paymentSummary");
    if (!paymentSummary) return;

    try {
        const paymentMonth = getSelectedPaymentMonthDate();
        const url = paymentMonth
            ? `${PAYMENTS_API_BASE_URL}/group/${groupId}/summary?paymentMonth=${paymentMonth}`
            : `${PAYMENTS_API_BASE_URL}/group/${groupId}/summary`;

        const response = await fetch(url, {
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        const result = await response.json();

        if (!response.ok || !result.success) {
            paymentSummary.innerHTML = `<p class="empty-text">${result.message || "Failed to load payment summary."}</p>`;
            return;
        }

        if (!result.data || result.data.length === 0) {
            paymentSummaryData = [];
            paymentSummary.innerHTML = `<p class="empty-text">No accepted members found for payment tracking.</p>`;
            updatePaymentFormState();
            return;
        }

        paymentSummaryData = result.data || [];
        syncPaymentUserDropdownFromSummary();
        paymentSummary.innerHTML = paymentSummaryData.map(item => `
            <div class="payment-summary-row">
                <div>
                    <h4>${item.fullName || "No Name"}</h4>
                    <p>${item.role}</p>
                </div>
                <div><span>Expected</span><strong>${formatCurrency(item.expectedAmount)}</strong></div>
                <div><span>Paid</span><strong>${formatCurrency(item.paidAmount)}</strong></div>
                <div><span>Pending</span><strong>${formatCurrency(item.pendingAmount)}</strong></div>
                <span class="status-pill ${String(item.status || "PENDING").toLowerCase()}">
                    ${formatPaymentStatus(item.status)}
                </span>
            </div>
        `).join("");
        updatePaymentFormState();
    } catch (error) {
        paymentSummaryData = [];
        paymentSummary.innerHTML = `<p class="empty-text">Failed to load payment summary.</p>`;
        updatePaymentFormState();
    }
}

async function loadPayments() {
    const paymentsList = document.getElementById("paymentsList");

    if (!paymentsList) return;

    await loadPaymentSummary();

    try {
        const paymentMonth = getSelectedPaymentMonthDate();
        const url = paymentMonth
            ? `${PAYMENTS_API_BASE_URL}/group/${groupId}?paymentMonth=${paymentMonth}`
            : `${PAYMENTS_API_BASE_URL}/group/${groupId}`;

        const response = await fetch(url, {
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        const result = await response.json();

        if (!response.ok || !result.success) {
            paymentsList.innerHTML = `<p class="empty-text">${result.message || "Failed to load payments."}</p>`;
            return;
        }

        if (!result.data || result.data.length === 0) {
            paymentsList.innerHTML = `<p class="empty-text">No payment history recorded for this month.</p>`;
            return;
        }

        paymentsList.innerHTML = result.data.map(payment => `
            <div class="payment-row">
                <div>
                    <h4>${payment.fullName || "No Name"}</h4>
                    <p>Month: ${payment.paymentMonth}</p>
                    <p>Paid Date: ${payment.paidDate}</p>
                    ${payment.notes ? `<p>Notes: ${payment.notes}</p>` : ""}
                </div>
                <div class="payment-amount">
                    <strong>${formatCurrency(payment.amount)}</strong>
                    <span>${formatPaymentStatus(payment.status)}</span>
                </div>
            </div>
        `).join("");
    } catch (error) {
        paymentsList.innerHTML = `<p class="empty-text">Failed to load payments.</p>`;
    }
}

async function recordPayment() {
    const message = document.getElementById("paymentMessage");
    const userId = document.getElementById("paymentUser").value;
    const amount = document.getElementById("paymentAmount").value;
    const paymentMonth = getSelectedPaymentMonthDate();
    const notes = document.getElementById("paymentNotes").value;

    message.textContent = "";

    if (!userId || !amount || Number(amount) <= 0 || !paymentMonth) {
        message.textContent = "Choose a member, month, and valid amount.";
        message.className = "payment-message error";
        return;
    }

    const summary = getSelectedPaymentSummary();
    if (summary && Number(summary.pendingAmount || 0) <= 0) {
        message.textContent = "Payment is already recorded for this member and month.";
        message.className = "payment-message error";
        updatePaymentFormState();
        return;
    }

    const expected = Number(summary?.expectedAmount || getExpectedMonthlyAmount() || 0);
    if (expected > 0 && Number(amount) !== expected) {
        message.textContent = `Amount must be exactly ${formatCurrency(expected)} for this month.`;
        message.className = "payment-message error";
        return;
    }

    try {
        const response = await fetch(`${PAYMENTS_API_BASE_URL}/record`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`
            },
            body: JSON.stringify({
                groupId: Number(groupId),
                userId: Number(userId),
                amount: Number(amount),
                paymentMonth,
                notes
            })
        });

        const result = await response.json();

        if (response.ok && result.success) {
            message.textContent = result.message;
            message.className = "payment-message success";
            document.getElementById("paymentNotes").value = "";
            await loadPayments();
            renderFinanceDetails(currentGroup);
            updatePaymentFormState();
            localStorage.setItem("groupFundLastPaymentUpdate", String(Date.now()));
        } else {
            message.textContent = result.message || "Payment could not be recorded.";
            message.className = "payment-message error";
        }
    } catch (error) {
        message.textContent = "Failed to record payment.";
        message.className = "payment-message error";
    }
}

async function loadGroupDetails() {
    try {
        const response = await fetch(`${API_BASE_URL}/${groupId}`, {
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        const result = await response.json();

        if (!response.ok) {
            alert(result.message);
            window.location.href = "my-group.html";
            return;
        }

        const group = result.data;
        currentGroup = group;
        currentGroupMembers = group.members || [];

        document.getElementById("groupTitle").innerText = group.groupName;
        document.getElementById("groupSubtitle").innerText =
            `${formatGroupType(group.groupType)} - ${formatGroupStatus(group.groupStatus)}`;

        document.getElementById("groupType").innerText = formatGroupType(group.groupType);
        document.getElementById("groupStatus").innerText = formatGroupStatus(group.groupStatus);
        document.getElementById("memberCount").innerText = `${group.totalMembers} / ${group.targetMembers || 20}`;
        document.getElementById("leaderName").innerText = group.leaderName;

        document.getElementById("overviewContent").innerHTML = `
            <div class="overview-box">
                <h3>${group.groupName}</h3>
                <p><strong>Type:</strong> ${formatGroupType(group.groupType)}</p>
                <p><strong>Status:</strong> ${formatGroupStatus(group.groupStatus)}</p>
                <p><strong>Leader:</strong> ${group.leaderName}</p>
                <p><strong>Members:</strong> ${group.totalMembers} / ${group.targetMembers || 20}</p>
                <p><strong>Start Date:</strong> ${formatDate(group.startDate)}</p>
                <p><strong>Activation:</strong> ${getActivationText(group)}</p>
                ${renderActivationActions(group)}
                <p><strong>Expected monthly amount per member:</strong> ${formatCurrency(getExpectedMonthlyAmount())}</p>
            </div>
        `;

        document.getElementById("membersList").innerHTML =
            currentGroupMembers.map(member => `
                <div class="member-row">
                    <h4>${member.role === "LEADER" ? "Leader" : "Member"} - ${member.fullName}</h4>
                    <p>Mobile: ${member.mobileNumber}</p>
                    <p>Role: ${member.role}</p>
                    <p>Status: ${member.status}</p>
                </div>
            `).join("");

        renderFinanceDetails(group);
        populatePaymentMembers();

        const paymentForm = document.getElementById("paymentForm");
        const paymentLeaderNote = document.getElementById("paymentLeaderNote");
        if (paymentForm && paymentLeaderNote) {
            if (group.groupStatus !== "ACTIVE") {
                paymentForm.style.display = "none";
                paymentLeaderNote.style.display = "block";
                paymentLeaderNote.textContent = "Payments are locked. Activate the group first after 5+ members join and all accepted members approve.";
            } else if (isCurrentUserLeader(group)) {
                paymentForm.style.display = "grid";
                paymentLeaderNote.style.display = "none";
            } else {
                paymentForm.style.display = "none";
                paymentLeaderNote.style.display = "block";
                paymentLeaderNote.textContent = "Only the group leader can record payments. You can view payment status and history.";
            }
        }

        await loadPayments();

    } catch (error) {
        alert("Failed to load group details.");
    }
}

const paymentMonthInput = document.getElementById("paymentMonth");
if (paymentMonthInput) {
    paymentMonthInput.addEventListener("change", loadPayments);
}

const paymentUserInput = document.getElementById("paymentUser");
if (paymentUserInput) {
    paymentUserInput.addEventListener("change", updatePaymentFormState);
}

loadGroupDetails();

// Refresh payment status instantly when another GroupFund tab records a payment.
window.addEventListener("storage", (event) => {
    if (event.key === "groupFundLastPaymentUpdate") {
        loadGroupDetails();
    }
});

document.addEventListener("visibilitychange", () => {
    if (!document.hidden) {
        loadGroupDetails();
    }
});

setInterval(() => {
    const paymentsTab = document.getElementById("paymentsTab");
    if (paymentsTab && !paymentsTab.classList.contains("hidden")) {
        loadPayments();
    }
}, 3000);

// Close popups when user clicks outside the popup card.
document.querySelectorAll(".modal-overlay").forEach(modal => {
    modal.addEventListener("click", (event) => {
        if (event.target === modal) {
            modal.style.display = "none";
        }
    });
});
