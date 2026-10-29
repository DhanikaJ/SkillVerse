let allCourses = [];

function showToast(message) {
    const toastBody = document.getElementById("toastBody");
    toastBody.innerText = message;

    const toastEl = document.getElementById("signupToast");
    const toast = new bootstrap.Toast(toastEl);
    toast.show();
}

async function loadData() {
    await loadCourses();
    updateCurrentCourses(allCourses);
}

async function loadCourses() {
    const courseList = document.getElementById('course-list');
    courseList.innerHTML = '<div class="text-center w-100 py-5"><div class="spinner-border text-primary"></div></div>';
    try {
        const res = await fetch("CourseList");
        const data = await res.json();
        if (!data.status)
            throw new Error(data.message || "Failed to load courses");
        allCourses = data.courses; // Save globally!
        renderCourses(allCourses);
    } catch (err) {
        courseList.innerHTML = `<div class="text-danger py-5 w-100 text-center">Could not load courses.</div>`;
        showToast(err);
    }
}

function renderCourses(courses) {
    const courseList = document.getElementById('course-list');
    courseList.innerHTML = "";
    if (courses.length === 0) {
        courseList.innerHTML = '<div class="text-muted text-center w-100 py-5">No courses match your filter.</div>';
        return;
    }
    courses.forEach((c, idx) => {
        courseList.innerHTML += `
            <div class="col d-flex">
              <div class="card shadow h-100 w-100 gsap-glow" data-index="${idx}">
                <img src="${c.img}" class="card-img-top rounded-3 mb-2" style="height:144px; object-fit:cover;">
                <div class="card-body d-flex flex-column">
                  <div class="text-primary fw-semibold mb-1">${c.title}</div>
                  <div class="text-muted small mb-1">${c.university} • <span>${c.instructor}</span></div>
                  <div class="mb-2 text-warning" style="font-size:1.07em;">${renderStars(c.rating)}</div>
                  <p class="text-muted small mb-3 flex-grow-1">${c.description}</p>
                  <button class="enroll btn btn-primary mt-auto" onclick="handleEnroll(${c.id})">Enroll Now</button>
                </div>
              </div>
            </div>`;
    });
    if (window.gsap) {
        gsap.from('.gsap-glow', {
            opacity: 0,
            y: 48,
            duration: 0.7,
            stagger: 0.16,
            ease: "power2.out"
        });
    }
}

function renderStars(rating) {
    let stars = '';
    const rounded = Math.round(rating * 2) / 2;
    for (let i = 1; i <= 5; i++) {
        if (i <= Math.floor(rounded))
            stars += '<i class="ri-star-fill"></i>';
        else if (i === Math.ceil(rounded) && rounded % 1 !== 0)
            stars += '<i class="ri-star-half-line"></i>';
        else
            stars += '<i class="ri-star-line"></i>';
    }
    return stars;
}

// "Enroll" handler
function handleEnroll(courseId) {
    //window.location = "course-view.html?id=" + encodeURIComponent(courseId);
    showToast("Please Login too see courses!");
}

//pagination
let currentCourses = [];  // Courses currently being shown after filters/search
let currentPage = 1;
const pageSize = 4;       // Courses per page (change as needed)

function showPage(page) {
    currentPage = page;
    const start = (page - 1) * pageSize;
    const end = start + pageSize;
    renderCourses(currentCourses.slice(start, end));
    renderPagination();
}

function renderPagination() {
    const total = currentCourses.length;
    const totalPages = Math.ceil(total / pageSize);
    const pagination = document.getElementById('course-pagination');
    if (totalPages <= 1) {
        pagination.innerHTML = '';
        return;
    }

    let html = '';
    html += `<li class="page-item${currentPage === 1 ? ' disabled' : ''}">
                <a class="page-link" href="#" tabindex="-1" onclick="showPage(${currentPage - 1});return false;">Previous</a>
            </li>`;
    for (let i = 1; i <= totalPages; i++) {
        html += `<li class="page-item${i === currentPage ? ' active' : ''}">
                    <a class="page-link" href="#" onclick="showPage(${i});return false;">${i}</a>
                 </li>`;
    }
    html += `<li class="page-item${currentPage === totalPages ? ' disabled' : ''}">
                <a class="page-link" href="#" onclick="showPage(${currentPage + 1});return false;">Next</a>
             </li>`;
    pagination.innerHTML = html;
}

// Always call this after any filtering or searching
function updateCurrentCourses(courses) {
    currentCourses = courses.slice();
    showPage(1);
}

// Use this after loading, filtering, or searching
// Example: after search/filter:
updateCurrentCourses(allCourses);

//Advanced Search
document.getElementById('course-search-form').addEventListener('submit', function (e) {
    e.preventDefault();
    applySearch();
});
document.getElementById('course-search-form2').addEventListener('submit', function (e) {
    e.preventDefault();
    applySearch();
});
document.getElementById('course-search-input').addEventListener('input', function () {
    if (this.value.trim().length === 0)
        applySearch(); // Optionally, live search
});
document.getElementById('course-search-input2').addEventListener('input', function () {
    if (this.value.trim().length === 0)
        applySearch(); // Optionally, live search
});

function applySearch() {
    const q = document.getElementById('course-search-input').value.trim().toLowerCase();
    let filtered = allCourses;

    if (q.length > 0) {
        filtered = allCourses.filter(course =>
            (course.title && course.title.toLowerCase().includes(q)) ||
                    (course.instructor && course.instructor.toLowerCase().includes(q)) ||
                    (course.description && course.description.toLowerCase().includes(q))
        );
    }

    updateCurrentCourses(filtered);
}

//design
const logoLight = "assets/images/logo/skillverseblue2.gif";
const logoDark = "assets/images/logo/skillversewhite2.gif";

// --- Dark mode persist + animated toggle ---
function setDarkMode(isDark) {
    document.body.classList.toggle('dark-mode', isDark);
    document.body.classList.toggle('bg-dark', isDark);
    document.body.classList.toggle('text-white', isDark);
    document.body.classList.toggle('bg-light', !isDark);
    document.body.classList.toggle('text-dark', !isDark);
    document.getElementById('main-logo').src = isDark ? logoDark : logoLight;
    localStorage.setItem('mode', isDark ? 'dark' : 'light');
}

// On page load
if (localStorage.getItem('mode') === 'dark') {
    setDarkMode(true);
    document.getElementById('toggle-icon').textContent = '☀️';
}

// Animate icon on click
document.getElementById('dark-toggle').onclick = function () {
    const btn = this;
    const icon = btn.querySelector('.transition');
    const isDark = !document.body.classList.contains('dark-mode');

    // Animate current icon out
    icon.classList.add('spinning');
    setTimeout(() => {
        icon.classList.remove('spinning');
        icon.classList.add('in');
        icon.textContent = isDark ? '☀️' : '🌙';
        // Animate icon in
        setTimeout(() => icon.classList.remove('in'), 300);
    }, 300);

    setDarkMode(isDark);
};

// Show/Hide password in login modal
document.addEventListener("DOMContentLoaded", function () {
    const togglePassword = document.getElementById("togglePassword");
    const passwordField = document.getElementById("loginPassword");
    if (togglePassword) {
        togglePassword.addEventListener('click', function () {
            const type = passwordField.type === 'password' ? 'text' : 'password';
            passwordField.type = type;
            this.innerHTML = type === 'text'
                    ? '<i class="ri-eye-line"></i>'
                    : '<i class="ri-eye-off-line"></i>';
        });
    }
});

// --- GSAP infinite carousel ---
const carousel = document.getElementById('review-carousel');
const cards = Array.from(carousel.children);

// Duplicate cards to make the loop seamless
cards.forEach(card => carousel.appendChild(card.cloneNode(true)));

let isPaused = false;
let speed = 2; // px per frame

carousel.addEventListener('mouseenter', () => isPaused = true);
carousel.addEventListener('mouseleave', () => isPaused = false);

function animateCarousel() {
    if (!isPaused) {
        // Move all children to the left
        carousel.scrollLeft += speed;

        // If scrolled past the width of one set, reset scroll
        if (carousel.scrollLeft >= carousel.scrollWidth / 2) {
            carousel.scrollLeft = 0;
        }
    }
    requestAnimationFrame(animateCarousel);
}
animateCarousel();

const phrases = [
    "Learn without limits",
    "Upgrade your future",
    "Build skills online",
    "Start your story today"
];

const el = document.getElementById("hero-title");
const typingSpeed = 70;
const deletingSpeed = 35;
const pauseTime = 1200;
let phraseIndex = 0;
let charIndex = 0;
let isDeleting = false;

function typeEffect() {
    const current = phrases[phraseIndex];
    el.innerHTML = current.slice(0, isDeleting ? charIndex : charIndex + 1) +
            '<img src="assets/images/logo/whiteplanet.png" alt="" style="height: 100px; vertical-align: middle; margin-left: 2px;">';

    if (!isDeleting) {
        charIndex++;
        if (charIndex === current.length) {
            isDeleting = true;
            setTimeout(typeEffect, pauseTime);
            return;
        }
        setTimeout(typeEffect, typingSpeed);
    } else {
        charIndex--;
        if (charIndex < 0) {
            isDeleting = false;
            phraseIndex = (phraseIndex + 1) % phrases.length;
            setTimeout(typeEffect, 500);
            return;
        }
        setTimeout(typeEffect, deletingSpeed);
    }
}

typeEffect();