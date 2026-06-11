const API_BASE_URL = `${window.location.origin}/api/groups`;
const token = localStorage.getItem("token");

const sidebar = document.getElementById("sidebar");
const menuBtn = document.getElementById("menuBtn");

const myGroupsList = document.getElementById("myGroupsList");
const invitationList = document.getElementById("invitationList");
const groupCount = document.getElementById("groupCount");
const invitationCount = document.getElementById("invitationCount");

const groupDetailsModal = document.getElementById("groupDetailsModal");
const groupDetails = document.getElementById("groupDetails");

if (!token) {
    window.location.href = "../auth/login.html";
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
    window.location.href = "../auth/login.html";
}

function openGroup(groupId) {
    localStorage.setItem("currentGroupId", groupId);
    window.location.href = `group-details.html?groupId=${groupId}`;
}

function formatGroupType(type) {
    if (type === "SAVINGS") return "Savings Group";
    if (type === "CUSTOM_LOAN") return "Custom Loan Group";
    return type;
}

function formatGroupStatus(status) {
    if (status === "PENDING_MEMBERS") return "Waiting for Members";
    if (status === "READY_TO_ACTIVATE") return "Ready to Activate";
    if (status === "ACTIVATION_PENDING") return "Activation Voting";
    if (status === "ACTIVE") return "Active";
    if (status === "CLOSED") return "Closed";
    return status;
}

function closeGroupDetailsModal() {
    groupDetailsModal.style.display = "none";
}

function showCreateGroupModal() {
    document.getElementById("createGroupModal").style.display = "flex";
}

function closeCreateGroupModal() {
    document.getElementById("createGroupModal").style.display = "none";

    if (localStorage.getItem("cameFromDashboard") === "true") {
        localStorage.removeItem("cameFromDashboard");
        window.location.href = "../auth/dashboard.html";
    }
}

function toggleGroupFields() {
    const groupType = document.getElementById("groupType").value;

    document.getElementById("savingsFields").classList.add("hidden");
    document.getElementById("loanFields").classList.add("hidden");

    if (groupType === "SAVINGS") {
        document.getElementById("savingsFields").classList.remove("hidden");
    }

    if (groupType === "CUSTOM_LOAN") {
        document.getElementById("loanFields").classList.remove("hidden");
    }
}

async function deleteGroup(groupId) {
    const confirmDelete = confirm("Are you sure you want to delete this group?");
    if (!confirmDelete) return;

    try {
        const response = await fetch(`${API_BASE_URL}/${groupId}`, {
            method: "DELETE",
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        let result = null;

        try {
            result = await response.json();
        } catch (e) {
            result = {
                message: response.status === 403
                    ? "You are not allowed to delete this group"
                    : "Something went wrong"
            };
        }

        alert(result.message);

        if (response.ok) {
            await loadMyGroups();
            await loadInvitations();
        }

    } catch (error) {
        console.error("Delete group error:", error);
        alert("Something went wrong while deleting group.");
    }
}

async function createGroup() {
    const groupName = document.getElementById("groupName").value.trim();
    const groupType = document.getElementById("groupType").value;
    const startDate = document.getElementById("startDate").value;
    const targetMembers = Number(document.getElementById("targetMembers")?.value || 5);
    const message = document.getElementById("createGroupMessage");

    const payload = {
        groupName,
        groupType,
        startDate,
        targetMembers
    };

    if (groupType === "SAVINGS") {
        payload.monthlySavingsAmount =
            document.getElementById("monthlySavingsAmount").value;
    }

    if (groupType === "CUSTOM_LOAN") {
        payload.loanSource = document.getElementById("loanSource").value;
        payload.loanAmount = document.getElementById("loanAmount").value;
        payload.durationMonths = document.getElementById("durationMonths").value;
        payload.monthlyEmi = document.getElementById("monthlyEmi").value;
        payload.interestRate = document.getElementById("interestRate").value;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/create`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`
            },
            body: JSON.stringify(payload)
        });

        const result = await response.json();
        message.textContent = result.message;

        if (response.ok) {
            message.style.color = "#2e7d32";

            await loadMyGroups();

            setTimeout(() => {
                closeCreateGroupModal();
                clearCreateForm();
                message.textContent = "";
            }, 800);
        } else {
            message.style.color = "#e53935";
        }

    } catch (error) {
        message.textContent = "Failed to create group.";
        message.style.color = "#e53935";
    }
}

function clearCreateForm() {
    document.getElementById("groupName").value = "";
    document.getElementById("groupType").value = "";
    document.getElementById("startDate").value = "";
    if (document.getElementById("targetMembers")) document.getElementById("targetMembers").value = "5";
    document.getElementById("monthlySavingsAmount").value = "";
    document.getElementById("loanSource").value = "";
    document.getElementById("loanAmount").value = "";
    document.getElementById("durationMonths").value = "";
    document.getElementById("monthlyEmi").value = "";
    document.getElementById("interestRate").value = "";

    document.getElementById("savingsFields").classList.add("hidden");
    document.getElementById("loanFields").classList.add("hidden");
}

async function loadMyGroups() {
    try {
        const response = await fetch(`${API_BASE_URL}/my-groups`, {
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        const result = await response.json();

        myGroupsList.innerHTML = "";

        if (!result.data || result.data.length === 0) {
            groupCount.innerText = "(0)";
            myGroupsList.innerHTML = `<p class="empty-text">No groups found.</p>`;
            return;
        }

        groupCount.innerText = `(${result.data.length})`;

        result.data.forEach(group => {
            myGroupsList.innerHTML += `
                <div class="group-item">
                    <div class="group-info">
                        <h3>👤👤 ${group.groupName}</h3>
                        <p>Type: ${formatGroupType(group.groupType)}</p>
                        <p>Leader: ${group.leaderName}</p>

                        <div class="group-meta-row">
                            <span class="member-badge">👤👤 ${group.totalMembers} / ${group.targetMembers || 20} Members</span>
                            <span class="status-badge ${group.groupStatus === "ACTIVE" ? "active" : "pending"}">
                                ${formatGroupStatus(group.groupStatus)}
                            </span>
                        </div>
                    </div>

                    <div class="group-actions">
                        <button class="open-btn" onclick="openGroup(${group.groupId})">
                            Open Group
                        </button>

                        <button class="view-btn" onclick="viewGroupDetails(${group.groupId})">
                            View Details
                        </button>

                        ${
                            group.leader
                            ? `<button class="delete-group-btn" onclick="deleteGroup(${group.groupId})">
                                    Delete Group
                               </button>`
                            : ""
                        }
                    </div>
                </div>
            `;
        });

    } catch (error) {
        myGroupsList.innerHTML = `<p class="empty-text">Failed to load groups.</p>`;
    }
}

async function loadInvitations() {
    try {
        const response = await fetch(`${API_BASE_URL}/invitations`, {
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        const result = await response.json();

        invitationList.innerHTML = "";

        if (!result.data || result.data.length === 0) {
            invitationCount.innerText = "(0)";
            invitationList.innerHTML = `<p class="empty-text">No pending invitations.</p>`;
            return;
        }

        invitationCount.innerText = `(${result.data.length})`;

        result.data.forEach(invite => {
            invitationList.innerHTML += `
                <div class="group-item">
                    <div class="group-info">
                        <h3>${invite.groupName}</h3>
                        <p>Type: ${invite.groupType}</p>
                        <p>Leader: ${invite.leaderName}</p>
                    </div>

                    <div class="group-actions">
                        <button class="accept-btn" onclick="acceptInvitation(${invite.invitationId})">
                            Accept
                        </button>
                        <button class="reject-btn" onclick="rejectInvitation(${invite.invitationId})">
                            Reject
                        </button>
                    </div>
                </div>
            `;
        });

    } catch (error) {
        invitationList.innerHTML = `<p class="empty-text">Failed to load invitations.</p>`;
    }
}

async function acceptInvitation(invitationId) {
    try {
        const response = await fetch(`${API_BASE_URL}/invitations/accept/${invitationId}`, {
            method: "POST",
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        const result = await response.json();
        alert(result.message);

        await loadInvitations();
        await loadMyGroups();

    } catch (error) {
        alert("Failed to accept invitation.");
    }
}

async function rejectInvitation(invitationId) {
    try {
        const response = await fetch(`${API_BASE_URL}/invitations/reject/${invitationId}`, {
            method: "POST",
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        const result = await response.json();
        alert(result.message);

        await loadInvitations();

    } catch (error) {
        alert("Failed to reject invitation.");
    }
}

async function viewGroupDetails(groupId) {
    try {
        const response = await fetch(`${API_BASE_URL}/${groupId}`, {
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        const result = await response.json();

        if (!response.ok) {
            alert(result.message);
            return;
        }

        const group = result.data;

        groupDetailsModal.style.display = "flex";

        groupDetails.innerHTML = `
            <div class="group-info">
                <h3>${group.groupName}</h3>
                <p>Type: ${group.groupType}</p>
                <p>Status: ${group.groupStatus}</p>
                <p>Total Members: ${group.totalMembers} / ${group.targetMembers || 20}</p>
                <p>Leader: ${group.leaderName}</p>
                <p>Start Date: ${group.startDate}</p>
            </div>

            <div class="member-list">
                <h3>Members</h3>
                ${group.members.map(member => `
                    <div class="member-row">
                        <h4>${member.fullName}</h4>
                        <p>Mobile: ${member.mobileNumber}</p>
                        <p>Role: ${member.role}</p>
                        <p>Status: ${member.status}</p>
                    </div>
                `).join("")}
            </div>
        `;


    } catch (error) {
        alert("Failed to load group details.");
    }
}

loadMyGroups();
loadInvitations();

if (localStorage.getItem("openCreateGroupModal") === "true") {
    localStorage.removeItem("openCreateGroupModal");
    showCreateGroupModal();
}
// Close popups when user clicks outside the popup card.
document.querySelectorAll(".modal-overlay").forEach(modal => {
    modal.addEventListener("click", (event) => {
        if (event.target === modal) {
            modal.style.display = "none";
            if (modal.id === "createGroupModal" && localStorage.getItem("cameFromDashboard") === "true") {
                localStorage.removeItem("cameFromDashboard");
                window.location.href = "../auth/dashboard.html";
            }
        }
    });
});

if (localStorage.getItem("openInvitations") === "true") {
    localStorage.removeItem("openInvitations");
    setTimeout(() => {
        const invitationSection = document.getElementById("invitationList");
        if (invitationSection) invitationSection.scrollIntoView({ behavior: "smooth", block: "center" });
    }, 500);
}
