function showToast(message) {
    const toastBody = document.getElementById("toastBody");
    toastBody.innerText = message;

    const toastEl = document.getElementById("profileToast");
    const toast = new bootstrap.Toast(toastEl);
    toast.show();
}

function loadData() {
    getCityData();
    getGenderData();
    getUserData();
}

async function getUserData() {
    const response = await fetch("UserProfile");

    if (response.ok) {
        const json = await response.json();
        document.getElementById("profile-name").innerHTML = `${json.firstName} ${json.lastName}` || "Add your Name";
        document.getElementById("profile-img").src = json.photo ? json.photo : "assets/images/profile.png";
        document.getElementById("currentpassword").value = `${json.currentpassword}`;
        document.getElementById("name").value = `${json.name}`;
        // Set city/gender dropdowns ONLY IF the option exists
        let citySelect = document.getElementById("citySelect");
        if (citySelect && json.cityId) {
            citySelect.value = String(json.cityId);
        }
        let genderSelect = document.getElementById("genderSelect");
        if (genderSelect && json.genderId) {
            genderSelect.value = String(json.genderId);
        }
    } else {
        document.getElementById("profile-name").innerHTML = "Add your Name";
        document.getElementById("profile-img").src = "assets/images/profile.png";
        document.getElementById("currentpassword").value = "Password Missing";
        document.getElementById("name").value = "Add your Name";
        document.getElementById("citySelect").value = "0";
        document.getElementById("genderSelect").value = "0";
    }
}

async function getCityData() {
    const response = await fetch("CityData");
    if (response.ok) {
        const cityList = await response.json();
        const citySelect = document.getElementById("citySelect");
        citySelect.innerHTML = '<option value="0" selected>Select your City</option>';

        cityList.forEach(city => {
            let option = document.createElement("option");
            option.innerHTML = city.name;
            option.value = city.id;
            citySelect.appendChild(option);
        });
    }
}

async function getGenderData() {
    const response = await fetch("GenderData");
    if (response.ok) {
        const genderList = await response.json();
        const genderSelect = document.getElementById("genderSelect");
        genderSelect.innerHTML = '<option value="0" selected>Select Gender</option>';

        genderList.forEach(gender => {
            let option = document.createElement("option");
            option.innerHTML = gender.name;
            option.value = gender.id;
            genderSelect.appendChild(option);
        });
    }
}

async function saveChanges() {

    const name = document.getElementById("name").value;
    const citySelect = document.getElementById("citySelect").value;
    if (!citySelect || citySelect === "0") {
        showToast("Please select a city.");
        return;
    }
    const genderSelect = document.getElementById("genderSelect").value;
    if (!genderSelect || genderSelect === "0") {
        showToast("Please select a gender.");
        return;
    }
    const currentpassword = document.getElementById("currentpassword").value;
    const newpassword = document.getElementById("newpassword").value;
    const confirmpassword = document.getElementById("confirmpassword").value;
    const photo = uploadedPhotoUrl || document.getElementById('profile-img').src || "";

    const userDataObject = {
        name: name,
        citySelect: citySelect,
        genderSelect: genderSelect,
        photo: photo,
        currentpassword: currentpassword,
        newpassword: newpassword,
        confirmpassword: confirmpassword
    };

    const userDataJson = JSON.stringify(userDataObject);

    const response = await fetch("UserProfile", {
        method: "PUT",
        body: userDataJson,
        headers: {
            "content-type": "application/json"
        }
    });

    if (response.ok) {
        const json = await response.json();
        if (json.status) {
            getUserData();
        } else {
            showToast(json.message);
        }
    } else {
        showToast("Profile details update failed!");
    }
}

let uploadedPhotoUrl = null;
document.querySelector('.edit-icon').addEventListener('click', async function () {
    // Create a hidden file input if not already present
    let fileInput = document.getElementById('profileFileInput');
    if (!fileInput) {
        fileInput = document.createElement('input');
        fileInput.type = 'file';
        fileInput.accept = 'image/*';
        fileInput.style.display = 'none';
        fileInput.id = 'profileFileInput';
        document.body.appendChild(fileInput);
    }

    // Listen for file selection
    fileInput.onchange = async function (event) {
        const file = event.target.files[0];
        if (!file)
            return;

        // Preview the image instantly
        const reader = new FileReader();
        reader.onload = function (e) {
            document.getElementById('profile-img').src = e.target.result;
        };
        reader.readAsDataURL(file);

        // Optionally: you can trigger your upload logic here or after "Save" button
        uploadedPhotoUrl = await uploadToCloudinary(file);
    };
    fileInput.click();
});

// Example Cloudinary upload function
async function uploadToCloudinary(file) {
    const url = 'https://api.cloudinary.com/v1_1/dkpeuhmnj/image/upload';
    const formData = new FormData();
    formData.append('file', file);
    formData.append('upload_preset', 'my_unsigned_preset');

    const res = await fetch(url,
            {method: 'POST',
                body: formData});
    const data = await res.json();
    return data.secure_url;
}

