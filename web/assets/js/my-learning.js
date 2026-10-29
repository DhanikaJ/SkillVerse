// my-learning.js
let myLearningData = {in_progress: [], completed: []};

document.addEventListener("DOMContentLoaded", () => {
    wireTabs();
    loadMyLearning();
});

function wireTabs() {
    const inBtn = document.getElementById("inProgressTab");
    const cBtn = document.getElementById("completedTab");
    const inList = document.getElementById("inProgressList");
    const cList = document.getElementById("completedList");

    inBtn.addEventListener("click", () => {
        inBtn.classList.add("active");
        cBtn.classList.remove("active");
        inList.style.display = "";
        cList.style.display = "none";
    });

    cBtn.addEventListener("click", () => {
        cBtn.classList.add("active");
        inBtn.classList.remove("active");
        cList.style.display = "";
        inList.style.display = "none";
    });
}

async function loadMyLearning() {
    try {
        const res = await fetch("MyLearning", {credentials: "include"});
        if (!res.ok) {
            showToast("Failed to load learning data.");
            return;
        }
        const json = await res.json();
        if (!json.status) {
            showToast(json.message || "Not authorized.");
            return;
        }
        myLearningData = json;
        renderList("inProgressList", json.in_progress);
        renderList("completedList", json.completed);
    } catch (err) {
        showToast("Error loading data.");
    }
}

function renderList(containerId, items) {
    const wrap = document.getElementById(containerId);
    if (!items || items.length === 0) {
        wrap.innerHTML = `<div class="text-muted small">No courses to show.</div>`;
        return;
    }

    wrap.innerHTML = items.map(renderCard).join("");
}

function renderCard(item) {
    const title = escapeHtml(item.title || "Course");
    const provider = escapeHtml(item.instructor || "Instructor");
    const progress = Number(item.progress || 0);
    const thumb = item.thumbnail || "assets/images/placeholder-course.png";
    // if your backend already sends `continue_url`, use it; otherwise fall back to course_id builder
    const contUrl = item.continue_url || (item.course_id ? `lesson.html?course_id=${encodeURIComponent(item.course_id)}` : "#");

    const rightBadge = (progress >= 100)
            ? `<span class="badge bg-success rounded-pill">Completed</span>`
            : `<a href="${contUrl}" class="btn btn-primary btn-sm">Continue</a>`;

    // matches your HTML style snippet
    return `
    <div class="col-12">
      <div class="d-flex gap-3 align-items-center p-3 rounded-3 border" style="background:var(--bs-body-bg);">
        <img src="${thumb}" alt="thumb" width="80" height="60" class="rounded" style="object-fit:cover;">
        <div class="flex-grow-1" style="min-width:230px;">
          <div class="d-flex align-items-center mb-2">
            <img src="assets/images/logo/whiteplanet.png" width="36" height="36" alt="Provider" class="me-2">
            <span class="text-muted" style="font-size:1.01em;">${provider}</span>
          </div>
          <div class="fw-bold" style="font-size:1.12em;">${title}</div>
          <div class="text-muted mb-1" style="font-size:0.98em;">Course &middot; ${progress}% complete</div>
          <div class="progress" style="width:94%;">
            <div class="progress-bar" role="progressbar" style="width:${progress}%"></div>
          </div>
        </div>
        <div class="ms-auto d-flex align-items-center gap-2">
          ${rightBadge}
        </div>
      </div>
    </div>
  `;
}

function showToast(msg) {
    const el = document.getElementById("mlToast");
    const body = document.getElementById("toastBody");
    if (!el || !body)
        return;
    body.textContent = msg;
    try {
        const t = new bootstrap.Toast(el);
        t.show();
    } catch (_) { /* no bootstrap toast loaded, ignore */
    }
}

// very tiny HTML escape
function escapeHtml(s) {
    return String(s || "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
}

async function getUserData() {
    const response = await fetch("UserProfile");

    if (response.ok) {
        const json = await response.json();
        document.getElementById("profile-img").src = json.photo ? json.photo : "assets/images/profile.png";
    } else {
        document.getElementById("profile-img").src = "assets/images/profile.png";
    }
}