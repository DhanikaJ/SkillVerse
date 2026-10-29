function showToast(message) {
    const toastBody = document.getElementById("toastBody");
    toastBody.innerText = message;

    const toastEl = document.getElementById("singleproductToast");
    const toast = new bootstrap.Toast(toastEl);
    toast.show();
}

async function loadCourseDetails() {
    // Get course ID from URL: course-view.html?id=123
    const urlParams = new URLSearchParams(window.location.search);
    const id = urlParams.get('id');
    if (!id) {
        document.querySelector('.ww').innerHTML = '<div class="text-danger py-5">No course selected.</div>';
        return;
    }

    // Show loader
    const mainDiv = document.querySelector('.ww');
    mainDiv.innerHTML = '<div class="text-center w-100 py-5"><div class="spinner-border text-primary"></div></div>';

    try {
        const res = await fetch(`CourseDetails?id=${id}`);
        const data = await res.json();
        if (!data.status)
            throw new Error(data.message);

        const c = data.course;
        setBreadcrumb(c);
        localStorage.setItem("lastCourseId", c.id);
        localStorage.setItem("lastCourseTitle", c.title);
        localStorage.setItem("lastCourseCategory", c.category);

        mainDiv.innerHTML = `
        <div class="d-flex course-header gap-4 flex-wrap flex-lg-nowrap">
            <div class="flex-shrink-0 mb-3 mb-lg-0">
                <img src="${c.img}" alt="Course Thumbnail" class="course-thumb">
            </div>
            <div class="flex-grow-1">
                <div class="course-info-block">
                    <span class="badge-category">${c.category}</span>
                    <span class="badge-status">${c.status}</span>
                    <div class="course-title mt-2 mb-2">${c.title}</div>
                    <div class="course-meta">
                        <i class="ri-user-3-line me-1"></i>by ${c.instructor} &nbsp; | &nbsp;
                        <i class="ri-calendar-event-line me-1"></i>Created at: ${c.createdAt}
                    </div>
                    <div class="course-meta mb-3">
                        <i class="ri-user-6-line me-1"></i>Published by: ${c.publisher}
                    </div>
                    <div class="course-desc">${c.description}</div>
                    <div class="price-tag mb-2">LKR ${Number(c.price).toFixed(2)}</div>
                    <button class="btn btn-primary enroll-btn" data-id="${c.id}">Add to Cart</button>
                </div>
                <div class="course-info-block mt-3">
                    <div class="mb-2 fw-bold">What you'll learn</div>
                    <ul class="mb-2" id="lessons-list">
                        <!-- Lessons injected here -->
                    </ul>
                    <div class="mb-2 fw-bold">Course Requirements</div>
                    <ul>
                        <li>Basic Python knowledge recommended</li>
                        <li>No prior experience in data analysis required</li>
                    </ul>
                </div>
            </div>
        </div>`;

        // Render lesson titles
        const lessonsList = document.getElementById('lessons-list');
        if (c.lessons && c.lessons.length > 0) {
            c.lessons.forEach(title => {
                lessonsList.innerHTML += `<li>${title}</li>`;
            });

        } else {
            lessonsList.innerHTML = '<li>No lessons available yet.</li>';
        }

    } catch (err) {
        mainDiv.innerHTML = `<div class="text-danger py-5">${err.message || "Could not load course details."}</div>`;
    }
}

window.addEventListener('DOMContentLoaded', loadCourseDetails);

function setBreadcrumb(course) {
    const ol = document.getElementById("breadcrumb-list");
    ol.innerHTML = `
        <li class="breadcrumb-item"><a href="home.html" class="text-decoration-none"><i class="ri-home-4-line"></i></a></li>
        <li class="breadcrumb-item"><a href="404.html" class="text-decoration-none">Browse</a></li>
        <li class="breadcrumb-item"><a href="course-view.html?category=${encodeURIComponent(course.category)}" class="text-decoration-none">${course.category}</a></li>
        <li class="breadcrumb-item active" aria-current="page">${course.title}</li>
    `;
}

document.addEventListener('click', async function (e) {
    if (e.target.classList.contains('enroll-btn')) {
        const courseId = e.target.getAttribute('data-id');
        try {
            const res = await fetch("Cart", {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({courseId})
            });

            // Debug response (step 1)
            const txt = await res.text();

            let data;
            try {
                data = JSON.parse(txt);
            } catch (err) {
                showToast("Server error (not JSON)");
                return;
            }

            // See if message exists
            showToast(data.message !== undefined ? data.message : "No message from backend!");

            if (data.status) {
                //updateCartCount();
            }
        } catch (err) {
            showToast("Failed to add to cart: " + err.message);
        }
    }
});