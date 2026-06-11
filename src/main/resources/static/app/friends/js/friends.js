const API_BASE_URL = "http://localhost:8080/api/friends";
const BACKEND_URL = "http://localhost:8080";

const token = localStorage.getItem("token");

const sidebar = document.getElementById("sidebar");
const menuBtn = document.getElementById("menuBtn");

const searchInput = document.getElementById("searchInput");
const searchBtn = document.getElementById("searchBtn");
const searchResults = document.getElementById("searchResults");

const notificationBtn = document.getElementById("notificationBtn");
const requestDropdown = document.getElementById("requestDropdown");
const requestList = document.getElementById("requestList");
const requestCount = document.getElementById("requestCount");
const pendingCountText = document.getElementById("pendingCountText");
const friendsList = document.getElementById("friendsList");
const friendsCount = document.getElementById("friendsCount");

const defaultImage = "https://cdn-icons-png.flaticon.com/512/149/149071.png";

let currentFriends = [];

function getProfileImage(url) {
    if (!url) return defaultImage;
    if (url.startsWith("http")) return url;
    return BACKEND_URL + url;
}

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

notificationBtn.addEventListener("click", () => {
    requestDropdown.classList.toggle("show");
});

searchBtn.addEventListener("click", searchUsers);

searchInput.addEventListener("keyup", (e) => {
    if (e.key === "Enter") searchUsers();
});

function isAlreadyFriend(userId) {
    return currentFriends.some(friend => friend.userId === userId);
}

async function searchUsers() {
    const keyword = searchInput.value.trim();

    if (keyword === "") {
        searchResults.innerHTML = `<p class="empty-text">Enter name or mobile number to search.</p>`;
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/search?keyword=${encodeURIComponent(keyword)}`, {
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        const result = await response.json();
        searchResults.innerHTML = "";

        if (!result.data || result.data.length === 0) {
            searchResults.innerHTML = `<p class="empty-text">No users found.</p>`;
            return;
        }

        result.data.forEach(user => {
            const alreadyFriend = isAlreadyFriend(user.userId);

            searchResults.innerHTML += `
                <div class="person-card">
                    <img src="${getProfileImage(user.profileImageUrl)}" alt="Profile">

                    <div class="person-info">
                        <h3>${user.fullName || "No Name"}</h3>
                        <p>Mobile: ${user.mobileNumber || ""}</p>
                        <p>District: ${user.district || "District not added"}</p>
                    </div>

                    <div class="person-action">
                        ${
                            alreadyFriend
                            ? `<button class="already-btn" disabled>Already in your friends list</button>`
                            : `<button class="action-btn" onclick="sendRequest(${user.userId})">Send Request</button>`
                        }
                    </div>
                </div>
            `;
        });

    } catch (error) {
        searchResults.innerHTML = `<p class="empty-text">Something went wrong while searching.</p>`;
    }
}

async function sendRequest(receiverId) {
    try {
        const response = await fetch(`${API_BASE_URL}/request/send/${receiverId}`, {
            method: "POST",
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        const result = await response.json();
        alert(result.message);

        await loadFriends();
        await loadRequests();
        await searchUsers();

    } catch (error) {
        alert("Failed to send request.");
    }
}

async function loadRequests() {
    try {
        const response = await fetch(`${API_BASE_URL}/requests/received`, {
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        const result = await response.json();

        requestList.innerHTML = "";

        if (!result.data || result.data.length === 0) {
            requestCount.innerText = "0";
            pendingCountText.innerText = "(0)";
            requestList.innerHTML = `<p class="empty-text">No pending requests.</p>`;
            return;
        }

        requestCount.innerText = result.data.length;
        pendingCountText.innerText = `(${result.data.length})`;

        result.data.forEach(req => {
            requestList.innerHTML += `
                <div class="request-item">
                    <img src="${getProfileImage(req.profileImageUrl)}" alt="Profile">

                    <div class="request-info">
                        <h4>${req.fullName || "No Name"}</h4>
                        <p>District: ${req.district || "District not added"}</p>
                    </div>

                    <div class="request-actions">
                        <button class="accept-btn" onclick="acceptRequest(${req.requestId})">Accept</button>
                        <button class="reject-btn" onclick="rejectRequest(${req.requestId})">Reject</button>
                    </div>
                </div>
            `;
        });

    } catch (error) {
        requestList.innerHTML = `<p class="empty-text">Failed to load requests.</p>`;
    }
}

async function acceptRequest(requestId) {
    const response = await fetch(`${API_BASE_URL}/request/accept/${requestId}`, {
        method: "POST",
        headers: {
            "Authorization": `Bearer ${token}`
        }
    });

    const result = await response.json();
    alert(result.message);

    await loadRequests();
    await loadFriends();
    await searchUsers();
}

async function rejectRequest(requestId) {
    const response = await fetch(`${API_BASE_URL}/request/reject/${requestId}`, {
        method: "POST",
        headers: {
            "Authorization": `Bearer ${token}`
        }
    });

    const result = await response.json();
    alert(result.message);

    await loadRequests();
    await searchUsers();
}

async function loadFriends() {
    try {
        const response = await fetch(`${API_BASE_URL}/list`, {
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        const result = await response.json();
        console.log("Friends API result:", result);

        friendsList.innerHTML = "";

        if (!result.data || result.data.length === 0) {
            currentFriends = [];
            friendsCount.innerText = "(0)";
            friendsList.innerHTML = `
                <div class="empty-friends">
                    <div class="empty-icon">Users</div>
                    <h3>No Friends Yet</h3>
                    <p>Search and connect with members from your Kuzhu.</p>
                </div>
            `;
            return;
        }

        currentFriends = result.data;
        friendsCount.innerText = `(${result.data.length})`;

        result.data.forEach(friend => {
            friendsList.innerHTML += `
                <div class="person-card">
                    <img src="${getProfileImage(friend.profileImageUrl)}" alt="Profile">

                    <div class="person-info">
                        <h3>${friend.fullName || "No Name"}</h3>
                        <p>Mobile: ${friend.mobileNumber || ""}</p>
                        <p>District: ${friend.district || "District not added"}</p>
                    </div>

                    <div class="person-action">
                        <button class="delete-btn" onclick="deleteFriend(${friend.requestId})">
                            Delete Friend
                        </button>
                    </div>
                </div>
            `;
        });

    } catch (error) {
          console.error("Load friends error:", error);
          friendsList.innerHTML = `<p class="empty-text">Failed to load friends.</p>`;
      }
}

async function deleteFriend(requestId) {
    const confirmDelete = confirm("Are you sure you want to remove this friend?");

    if (!confirmDelete) return;

    try {
        const response = await fetch(`${API_BASE_URL}/delete/${requestId}`, {
            method: "DELETE",
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        const result = await response.json();
        alert(result.message);

        await loadFriends();
        await loadRequests();
        await searchUsers();

    } catch (error) {
        alert("Failed to delete friend.");
    }
}

loadFriends();
loadRequests();

function logout() {
    localStorage.removeItem("token");
    window.location.href = "../auth/login.html";
}