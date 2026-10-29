function showToast(message) {
    const toastBody = document.getElementById("toastBody");
    toastBody.innerText = message;

    const toastEl = document.getElementById("instructorToast");
    const toast = new bootstrap.Toast(toastEl);
    toast.show();
}

function loadData() {
    loadCategories();
}

async function loadCategories() {
    const res = await fetch("CategoryData");
    if (res.ok) {
        const categories = await res.json();
        const sel = document.getElementById("courseCategory");
        sel.innerHTML = '<option value="">Select Category</option>';
        categories.forEach(cat => {
            let opt = document.createElement("option");
            opt.value = cat.id;
            opt.innerHTML = cat.type;
            sel.appendChild(opt);
        });
    }
}

async function submitCourse() {

    showLoader();

    const thumbnailFile = document.getElementById('thumbnailImage').files[0];
    if (!thumbnailFile) {
        showToast("Thumbnail is required!");
        return;
    }
    const uploadedThumbnailUrl = await uploadFileToCloudinary(thumbnailFile);

    const lessons = await collectLessons();

    const courseData = {
        title: document.getElementById('courseTitle').value,
        category: document.getElementById('courseCategory').value,
        difficulty: document.getElementById('courseDifficulty').value.charAt(0),
        priceType: document.getElementById('coursePrice').value.charAt(0),
        description: document.getElementById('courseDescription').value,
        priceLKR: document.getElementById('coursePriceLKR').value,
        thumbnailUrl: uploadedThumbnailUrl,
        lessons: lessons,
        quiz: {
            title: document.getElementById('quizTitle').value,
            questions: []
        }
    };

    // Validate required fields
    if (!courseData.title || !courseData.category || !courseData.difficulty ||
            !courseData.priceType || !courseData.description || !courseData.priceLKR) {
        showToast("Please fill all required fields!");
        return;
    }


    // Handle quizzes
    const questionBlocks = document.getElementsByClassName('question-block');
    for (let qBlock of questionBlocks) {
        let questionText = qBlock.querySelector('input[type=text]').value;
        let options = qBlock.querySelectorAll('input[placeholder^="Answer"]');
        let radios = qBlock.querySelectorAll('input[type=radio]');
        let correctOption = '0';
        radios.forEach((r, idx) => {
            if (r.checked)
                correctOption = String.fromCharCode(65 + idx);
        });

        courseData.quiz.questions.push({
            questionText,
            optionA: options[0].value,
            optionB: options[1].value,
            optionC: options[2].value,
            optionD: options[3].value,
            correctOption
        });
    }

    const response = await fetch('AddCourse', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(courseData)
    });

    const result = await response.json();
    hideLoader();
    showToast(result.message);
    if (result.status)
        clearCourseForm();
}

async function collectLessons() {
    const lessons = [];
    const lessonBlocks = document.getElementsByClassName('lesson-block');
    for (let block of lessonBlocks) {
        const title = block.querySelector('input[type=text]').value;
        const videoUrlField = block.querySelector('input[type=url]');
        let videoUrl = videoUrlField.value;
        const videoFileInput = block.querySelector('input[type=file][accept^="video"]');
        const resourceFileInput = block.querySelector('input[type=file][accept*=".docx"]');

        // Upload video if file provided
        if (videoFileInput.files.length > 0) {
            videoUrl = await uploadFileToCloudinary(videoFileInput.files[0]);
        }

        let resourceUrl = "";
        if (resourceFileInput.files.length > 0) {
            resourceUrl = await uploadFileToCloudinary(resourceFileInput.files[0]);
        }

        lessons.push({
            title,
            videoUrl,
            resourceUrl
        });
    }
    return lessons;
}

async function uploadFileToCloudinary(file) {
    const url = 'https://api.cloudinary.com/v1_1/dkpeuhmnj/auto/upload';
    const formData = new FormData();
    formData.append('file', file);
    formData.append('upload_preset', 'my_unsigned_preset');

    const res = await fetch(url, {method: 'POST', body: formData});
    const data = await res.json();
    return data.secure_url;
}

document.querySelector("#add form").addEventListener("submit", function (e) {
    e.preventDefault();
    submitCourse();
});

function showLoader() {
    document.getElementById('loaderOverlay').style.display = 'flex';
}
function hideLoader() {
    document.getElementById('loaderOverlay').style.display = 'none';
}

function clearCourseForm() {
    document.getElementById('courseTitle').value = '';
    document.getElementById('courseCategory').value = '0';
    document.getElementById('courseDifficulty').value = '';
    document.getElementById('coursePrice').value = '';
    document.getElementById('courseDescription').value = '';
    document.getElementById('thumbnailImage').value = '';
    document.getElementById('coursePriceLKR').value = '';
    document.getElementById('quizTitle').value = '';

    // Remove all lesson and quiz blocks, add one of each
    document.getElementById('lessonsContainer').innerHTML = '';
    addLessonBlock();

    document.getElementById('questionsContainer').innerHTML = '';
    questionCount = 0; // Reset numbering
    addQuestion();
}
