function showToast(message) {
    const toastBody = document.getElementById("toastBody");
    toastBody.innerText = message;

    const toastEl = document.getElementById("signupToast");
    const toast = new bootstrap.Toast(toastEl);
    toast.show();
}

async function signUp() {
    const email = document.getElementById("email").value.trim();
    const name = document.getElementById("name").value.trim();
    const password = document.getElementById("password").value;

    let firstName = "";
    let lastName = "";

    if (name.includes(" ")) {
        const parts = name.split(" ");
        firstName = parts[0];
        lastName = parts.slice(1).join(" ");
    } else {
        firstName = name;
        lastName = ""; // optional
    }

    const user = {
        email: email,
        firstName: firstName,
        lastName: lastName,
        password: password
    };


    const userJson = JSON.stringify(user);
    const response = await fetch(
            "SignUp",
            {
                method: "POST",
                body: userJson,
                header: {
                    "Content-Type": "application/json"
                }
            }
    );

    if (response.ok) {
        const responseJson = await response.json();
        if (responseJson.status) {
            const signupModalEl = document.getElementById('signupModal');
            const signupModal = bootstrap.Modal.getInstance(signupModalEl)
                    || new bootstrap.Modal(signupModalEl);
            signupModal.hide();

            const otpModal = new bootstrap.Modal(document.getElementById('otpModal'));
            otpModal.show();
        } else {
            showToast(responseJson.message);
        }
    } else {
        showToast("Registration failed. Please try again.");
    }
}

