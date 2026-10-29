function showToast(message) {
    const toastBody = document.getElementById("toastBody");
    toastBody.innerText = message;

    const toastEl = document.getElementById("instructorToast");
    const toast = new bootstrap.Toast(toastEl);
    toast.show();
}

async function getUserData() {
    const response = await fetch("InstructorProfile");

    if (response.ok) {
        const json = await response.json();
        document.getElementById("username").innerHTML = `${json.firstName} ${json.lastName}`;
        document.getElementById("firstName").innerHTML = `Welcome Back, ${json.firstName}!`;
        document.getElementById("profileemail").innerHTML = `${json.email}`;
        document.getElementById("since").innerHTML = `Joined: ${json.since}`;
        document.getElementById("profile-img").src = json.photo ? json.photo : "assets/images/profile.png";
    } else {
        document.getElementById("username").innerHTML = "Add your Name";
        document.getElementById("firstName").innerHTML = "Add your First Name";
        document.getElementById("profileemail").innerHTML = "Email missing";
        document.getElementById("since").innerHTML = `Joined: Missing`;
        document.getElementById("profile-img").src = "assets/images/profile.png";
    }
}
