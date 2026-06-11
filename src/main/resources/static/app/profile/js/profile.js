const profileToken = localStorage.getItem("token");

if (!profileToken) {
    window.location.href = "../auth/login.html";
}

const API_IMAGE_BASE_URL = window.location.origin;
const DEFAULT_PROFILE_IMAGE = "https://cdn-icons-png.flaticon.com/512/149/149071.png";
let currentProfile = null;
let currentSection = "personal";

async function loadProfile() {
    try {
        const response = await fetch(`${API_BASE_URL}/profile/me`, {
            method: "GET",
            headers: {
                Authorization: `Bearer ${profileToken}`
            }
        });

        const result = await response.json();

        if (response.ok) {
            currentProfile = result.data;

            updateTopProfile(currentProfile);

            currentSection = "personal";
            showProfileSection("personal");
        } else {
            console.log(result.message);
        }

    } catch (error) {
        console.log("Failed to fetch profile", error);
    }
}

function updateTopProfile(p) {
    document.getElementById("profileName").textContent = p.fullName || "Your Name";
    document.getElementById("profileOccupation").textContent = p.occupation || "Occupation not added";
    document.getElementById("profileLocation").textContent = p.district || "Location not added";

    document.getElementById("profileGender").textContent = p.gender || "Gender";
    document.getElementById("profileDob").textContent = p.dateOfBirth || "DOB";
    document.getElementById("profileMobile").textContent = p.mobileNumber || "Mobile";
    document.getElementById("profileEmail").textContent = p.email || "Email";

    document.getElementById("profilePercent").textContent =
        `${p.profileCompletionPercentage || 0}%`;

    const profileImg = document.getElementById("profileImage");

    if (p.profileImageUrl) {
        profileImg.src = API_IMAGE_BASE_URL + p.profileImageUrl;
    } else {
        profileImg.src = DEFAULT_PROFILE_IMAGE;
    }

    document.getElementById("photoAchievement").textContent =
        p.profileImageUrl ? "Photo Uploaded" : "Photo Not Uploaded";

    document.getElementById("groupEligibility").textContent =
        (p.profileCompletionPercentage || 0) >= 80
            ? "Eligible to Create Group"
            : "Complete profile to create group";
}

function showProfileSection(section) {
    currentSection = section;

    document.querySelectorAll(".quick-link").forEach(link => {
        link.classList.remove("active-link");
    });

    const activeLink = document.getElementById(section + "Link");
    if (activeLink) {
        activeLink.classList.add("active-link");
    }

    const title = document.getElementById("sectionTitle");
    const content = document.getElementById("sectionContent");
    const p = currentProfile || {};

    if (section === "personal") {
        title.textContent = "Personal Details";
        content.innerHTML = `
            <div><label>Full Name</label><p>${p.fullName || "Not added"}</p></div>
            <div><label>Date of Birth</label><p>${p.dateOfBirth || "Not added"}</p></div>
            <div><label>Gender</label><p>${p.gender || "Not added"}</p></div>
            <div><label>Marital Status</label><p>${p.maritalStatus || "Not added"}</p></div>
            <div><label>Address</label><p>${p.address || "Not added"}</p></div>
            <div><label>District</label><p>${p.district || "Not added"}</p></div>
            <div><label>State</label><p>${p.state || "Not added"}</p></div>
            <div><label>Pincode</label><p>${p.pincode || "Not added"}</p></div>
        `;
    }

    if (section === "family") {
        title.textContent = "Family Details";
        content.innerHTML = `
            <div><label>Father Name</label><p>${p.fatherName || "Not added"}</p></div>
            <div><label>Mother Name</label><p>${p.motherName || "Not added"}</p></div>
        `;
    }

    if (section === "occupation") {
        title.textContent = "Occupation";
        content.innerHTML = `
            <div><label>Occupation</label><p>${p.occupation || "Not added"}</p></div>
            <div><label>Monthly Income</label><p>${p.monthlyIncome ? "\u20B9" + p.monthlyIncome : "Not added"}</p></div>
        `;
    }

    if (section === "verification") {
        title.textContent = "Verification";
        content.innerHTML = `
            <div><label>Email</label><p>${p.email || "Not added"}</p></div>
            <div><label>Mobile Number</label><p>${p.mobileNumber || "Not added"}</p></div>
            <div><label>Aadhaar Status</label><p>${p.aadhaarNumberVerified ? "Verified" : "Not verified"}</p></div>
        `;
    }
}

const sectionEditBtn = document.getElementById("sectionEditBtn");
const editSectionModal = document.getElementById("editSectionModal");
const closeSectionEdit = document.getElementById("closeSectionEdit");
const editModalTitle = document.getElementById("editModalTitle");
const editSectionFields = document.getElementById("editSectionFields");
const saveSectionBtn = document.getElementById("saveSectionBtn");
const sectionEditMessage = document.getElementById("sectionEditMessage");

if (sectionEditBtn) {
    sectionEditBtn.addEventListener("click", () => {
        openSectionEditModal();
    });
}

function openSectionEditModal() {
    const p = currentProfile || {};
    editSectionFields.innerHTML = "";

    if (currentSection === "personal") {
        editModalTitle.textContent = "Edit Personal Details";
        editSectionFields.innerHTML = `
            <div class="edit-field">
                <label>Full Name</label>
                <input type="text" id="editFullName" value="${p.fullName || ""}">
            </div>

            <div class="edit-field">
                <label>Date of Birth</label>
                <input type="date" id="editDob" value="${p.dateOfBirth || ""}">
            </div>

            <div class="edit-field">
                <label>Gender</label>
                <input type="text" id="editGender" value="${p.gender || ""}">
            </div>

            <div class="edit-field">
                <label>Marital Status</label>
                <input type="text" id="editMaritalStatus" value="${p.maritalStatus || ""}">
            </div>

            <div class="edit-field">
                <label>Address</label>
                <input type="text" id="editAddress" value="${p.address || ""}">
            </div>

            <div class="edit-field">
                <label>District</label>
                <input type="text" id="editDistrict" value="${p.district || ""}">
            </div>

            <div class="edit-field">
                <label>State</label>
                <input type="text" id="editState" value="${p.state || ""}">
            </div>

            <div class="edit-field">
                <label>Pincode</label>
                <input type="text" id="editPincode" value="${p.pincode || ""}">
            </div>
        `;
    }

    if (currentSection === "family") {
        editModalTitle.textContent = "Edit Family Details";
        editSectionFields.innerHTML = `
            <div class="edit-field">
                <label>Father Name</label>
                <input type="text" id="editFatherName" value="${p.fatherName || ""}">
            </div>

            <div class="edit-field">
                <label>Mother Name</label>
                <input type="text" id="editMotherName" value="${p.motherName || ""}">
            </div>
        `;
    }

    if (currentSection === "occupation") {
        editModalTitle.textContent = "Edit Occupation Details";
        editSectionFields.innerHTML = `
            <div class="edit-field">
                <label>Occupation</label>
                <input type="text" id="editOccupation" value="${p.occupation || ""}">
            </div>

            <div class="edit-field">
                <label>Monthly Income</label>
                <input type="number" id="editMonthlyIncome" value="${p.monthlyIncome || ""}">
            </div>
        `;
    }

    if (currentSection === "verification") {
        editModalTitle.textContent = "Verification Details";
        editSectionFields.innerHTML = `
            <div class="edit-field">
                <label>Email</label>
                <input type="text" value="${p.email || ""}" disabled>
            </div>

            <div class="edit-field">
                <label>Mobile Number</label>
                <input type="text" value="${p.mobileNumber || ""}" disabled>
            </div>

            <div class="edit-field">
                <label>Aadhaar Number</label>
                <input type="text" id="editAadhaarNumber" value="${p.aadhaarNumber || ""}">
            </div>
        `;
    }

    editSectionModal.style.display = "flex";
}

if (closeSectionEdit) {
    closeSectionEdit.addEventListener("click", () => {
        editSectionModal.style.display = "none";
    });
}

if (saveSectionBtn) {
    saveSectionBtn.addEventListener("click", async () => {
        let data = {};

        if (currentSection === "personal") {
            data = {
                fullName: document.getElementById("editFullName").value,
                dateOfBirth: document.getElementById("editDob").value,
                gender: document.getElementById("editGender").value,
                maritalStatus: document.getElementById("editMaritalStatus").value,
                address: document.getElementById("editAddress").value,
                district: document.getElementById("editDistrict").value,
                state: document.getElementById("editState").value,
                pincode: document.getElementById("editPincode").value
            };
        }

        if (currentSection === "family") {
            data = {
                fatherName: document.getElementById("editFatherName").value,
                motherName: document.getElementById("editMotherName").value
            };
        }

        if (currentSection === "occupation") {
            data = {
                occupation: document.getElementById("editOccupation").value,
                monthlyIncome: document.getElementById("editMonthlyIncome").value
            };
        }

        if (currentSection === "verification") {
            data = {
                aadhaarNumber: document.getElementById("editAadhaarNumber").value
            };
        }

        try {
            const response = await fetch(`${API_BASE_URL}/profile/save`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${profileToken}`
                },
                body: JSON.stringify(data)
            });

            const result = await response.json();

            if (response.ok) {
                sectionEditMessage.textContent = result.message;
                sectionEditMessage.className = "message success";

                await loadProfile();

                setTimeout(() => {
                    editSectionModal.style.display = "none";
                    sectionEditMessage.textContent = "";
                }, 800);
            } else {
                sectionEditMessage.textContent = result.message || "Update failed";
                sectionEditMessage.className = "message error";
            }
        } catch (error) {
            sectionEditMessage.textContent = "Something went wrong";
            sectionEditMessage.className = "message error";
        }
    });
}

const profileImageBox = document.getElementById("profileImageBox");
const topProfileModal = document.getElementById("topProfileModal");
const closeTopProfile = document.getElementById("closeTopProfile");
const saveTopProfileBtn = document.getElementById("saveTopProfileBtn");
const topProfileMessage = document.getElementById("topProfileMessage");

if (profileImageBox) {
    profileImageBox.addEventListener("click", () => {
        const currentImage = document.getElementById("profileImage").src;
        document.getElementById("photoPreview").src =
            currentImage && currentImage !== window.location.href
                ? currentImage
                : DEFAULT_PROFILE_IMAGE;
        topProfileModal.style.display = "flex";
    });
}

if (closeTopProfile) {
    closeTopProfile.addEventListener("click", () => {
        topProfileModal.style.display = "none";
    });
}

if (saveTopProfileBtn) {
    saveTopProfileBtn.addEventListener("click", async () => {
        try {
            const imageFile = document.getElementById("editProfileImage").files[0];

            if (!imageFile) {
                topProfileMessage.textContent = "Please choose an image";
                topProfileMessage.className = "message error";
                return;
            }

            const formData = new FormData();
            formData.append("file", imageFile);

            const response = await fetch(`${API_BASE_URL}/profile/upload-image`, {
                method: "POST",
                headers: {
                    Authorization: `Bearer ${profileToken}`
                },
                body: formData
            });

            const result = await response.json();

            if (response.ok) {
                topProfileMessage.textContent = result.message;
                topProfileMessage.className = "message success";

                await loadProfile();

                setTimeout(() => {
                    topProfileModal.style.display = "none";
                    topProfileMessage.textContent = "";
                    document.getElementById("editProfileImage").value = "";
                }, 800);
            } else {
                topProfileMessage.textContent = result.message || "Image upload failed";
                topProfileMessage.className = "message error";
            }

        } catch (error) {
            topProfileMessage.textContent = "Something went wrong";
            topProfileMessage.className = "message error";
        }
    });
}

const editProfileImage = document.getElementById("editProfileImage");
const selectedFileName = document.getElementById("selectedFileName");

if (editProfileImage && selectedFileName) {
    editProfileImage.addEventListener("change", () => {
        const file = editProfileImage.files[0];

        if (file) {
            selectedFileName.textContent = file.name;

            const previewUrl = URL.createObjectURL(file);
            document.getElementById("photoPreview").src = previewUrl;
        } else {
            selectedFileName.textContent = "No file selected";
            document.getElementById("photoPreview").src = DEFAULT_PROFILE_IMAGE;
        }
    });
}

const removeProfilePhotoBtn = document.getElementById("removeProfilePhotoBtn");

if (removeProfilePhotoBtn) {
    removeProfilePhotoBtn.addEventListener("click", async () => {

        if (!currentProfile || !currentProfile.profileImageUrl) {
            alert("No uploaded profile photo to remove.");
            return;
        }

        const confirmRemove = confirm("Are you sure you want to remove your profile photo?");
        if (!confirmRemove) return;

        try {
            const response = await fetch(`${API_BASE_URL}/profile/remove-image`, {
                method: "DELETE",
                headers: {
                    Authorization: `Bearer ${profileToken}`
                }
            });

            const result = await response.json();
            alert(result.message);

            if (response.ok) {
                document.getElementById("profileImage").src = DEFAULT_PROFILE_IMAGE;
                document.getElementById("photoPreview").src = DEFAULT_PROFILE_IMAGE;
                document.getElementById("editProfileImage").value = "";
                document.getElementById("selectedFileName").textContent = "No file selected";

                await loadProfile();
            }

        } catch (error) {
            alert("Failed to remove profile photo.");
        }
    });
}

document.addEventListener("DOMContentLoaded", () => {
    loadProfile();
});

// Close popups when user clicks outside the popup card.
document.querySelectorAll(".modal-overlay").forEach(modal => {
    modal.addEventListener("click", (event) => {
        if (event.target === modal) {
            modal.style.display = "none";
        }
    });
});
