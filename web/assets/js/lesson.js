function showToast(message) {
    const toastBody = document.getElementById("toastBody");
    toastBody.innerText = message;

    const toastEl = document.getElementById("lessonToast");
    const toast = new bootstrap.Toast(toastEl);
    toast.show();
}

(function () {
    const $sidebar = document.querySelector(".course-sidebar");
    const $video = document.querySelector(".video-player");
    const $resourcesBox = document.querySelector(".course-content-main .mb-4"); // the first .mb-4 under content is your resources box

    // --- “best effort” anti-download/screen-record measures (can’t fully prevent recording) ---
    if ($video) {
// Remove download option & PiP
        $video.setAttribute("controlsList", "nodownload noplaybackrate noremoteplayback");
        $video.setAttribute("playsinline", "");
        $video.disablePictureInPicture = true;
        // Block right-click on video
        $video.addEventListener("contextmenu", (e) => e.preventDefault());
        // Block common save/record keys (very best effort, still bypassable)
        document.addEventListener("keydown", (e) => {
            const key = e.key.toLowerCase();
            if (
                    (e.ctrlKey && (key === "s" || key === "p" || key === "u")) || // save/print/view-source
                    (e.ctrlKey && e.shiftKey && (key === "i" || key === "c" || key === "j")) // devtools combos
                    ) {
                e.preventDefault();
                e.stopPropagation();
                return false;
            }
        });
    }

// Parse course_id
    const params = new URLSearchParams(window.location.search);
    const course_id = params.get("course_id");
    if (!course_id) {
        showToast("Missing course reference. Redirecting to My Learning.");
        window.location.href = "my-learning.html";
        return;
    }

// Fetch lesson data
    loadCourse(course_id);
    async function loadCourse(id) {
        try {
            const res = await fetch(`LessonData?course_id=${encodeURIComponent(id)}`, {credentials: "include"});
            const data = await res.json();
            if (!res.ok || data.status === false) {
                showToast(data.message || "Failed to load lessons");
                return;
            }

            const course = data.course; // {id,title,thumbnail,publisher_name}
            const lessons = data.lessons || [];

            // ✅ Insert course & publisher name into page
            const $headerBox = document.querySelector(".course-header");
            if ($headerBox) {
                $headerBox.innerHTML = `
                <h3 class="mb-1">${escapeHtml(course.title)}</h3>
                <div class="text-muted small">
                    Published by: ${escapeHtml(course.publisher_name || "Unknown")}
                </div>
            `;
            }

            // Build sidebar
            renderSidebar(lessons);

            // Load first playable video
            const firstWithVideo = lessons.find(l => l.video_url && l.video_url.trim() !== "");
            if (firstWithVideo) {
                setVideo(firstWithVideo.video_url, course?.thumbnail);
                renderResources(firstWithVideo.id, firstWithVideo.resource_files || []);
            } else {
                setVideo("", course?.thumbnail);
                renderResources([]);
            }

            // Lesson click handlers
            $sidebar.querySelectorAll(".lesson-link").forEach((btn, idx) => {
                btn.addEventListener("click", () => {
                    $sidebar.querySelectorAll(".lesson-link").forEach(b => b.classList.remove("active"));
                    btn.classList.add("active");
                    const lesson = lessons[idx];
                    setVideo(lesson.video_url || "", course?.thumbnail);
                    renderResources(lesson.id, lesson.resource_files || []);
                });
            });
        } catch (err) {
            showToast("Error loading lessons");
        }
    }


    function renderSidebar(lessons) {
// Replace everything after title with generated buttons
        const titleEl = $sidebar.querySelector(".sidebar-title");
        // wipe old
        $sidebar.innerHTML = "";
        $sidebar.appendChild(titleEl);
        lessons.forEach((l, i) => {
            const btn = document.createElement("button");
            btn.className = `lesson-link ${i === 0 ? "active" : ""}`;
            btn.innerHTML = `<i class="ri-play-circle-line me-2"></i> ${escapeHtml(l.title || "Lesson")}`;
            $sidebar.appendChild(btn);
        });
        if (lessons.length === 0) {
            const empty = document.createElement("div");
            empty.className = "text-muted small mt-2";
            empty.textContent = "No lessons yet.";
            $sidebar.appendChild(empty);
        }
    }

    function setVideo(url, poster) {
        if (!$video)
            return;
        // Reset player
        $video.pause();
        $video.removeAttribute("src");
        const oldSrc = $video.querySelector("source");
        if (oldSrc)
            oldSrc.remove();
        $video.setAttribute("poster", poster || "assets/images/courses/course-demo.jpg");
        if (url && url.trim() !== "") {
            const source = document.createElement("source");
            source.src = url;
            source.type = "video/mp4"; // keep mp4; Cloudinary links are usually mp4
            $video.appendChild(source);
            $video.load();
        } else {
// No URL
// Optionally show a toast: “No video for this lesson”
        }
    }

    function renderResources(lessonId, links) {
        const wrapper = $resourcesBox;
        if (!wrapper)
            return;

        const titleDiv = wrapper.querySelector(".fw-bold.mb-2");
        wrapper.innerHTML = "";
        if (titleDiv)
            wrapper.appendChild(titleDiv);

        if (!links || links.length === 0) {
            const span = document.createElement("div");
            span.className = "text-muted";
            span.textContent = "No resources available for this lesson.";
            wrapper.appendChild(span);
            return;
        }

        links.forEach((href, idx) => {
            const a = document.createElement("a");

            // Route through our download proxy to force "attachment" download:
            a.href = `DownloadResource?lesson_id=${encodeURIComponent(lessonId)}&idx=${encodeURIComponent(idx)}`;
            a.target = "_self"; // same tab since it's a download
            a.className = "resource-link me-3";
            a.setAttribute("rel", "noopener");

            // For same-origin, HTML5 download hint works; our proxy sets the header to make it universal:
            a.setAttribute("download", "");

            a.innerHTML = `<i class="ri-file-2-line me-1"></i>Resource ${idx + 1}`;
            wrapper.appendChild(a);
        });
    }





    function escapeHtml(str) {
        return String(str).replace(/[&<>"']/g, s => ({
                "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
            }[s]));
    }
})();
