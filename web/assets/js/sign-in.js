function showToast(message) {
    const toastBody = document.getElementById("toastBody");
    toastBody.innerText = message;

    const toastEl = document.getElementById("signupToast");
    const toast = new bootstrap.Toast(toastEl);
    toast.show();
}

async function signIn() {

    const email = document.getElementById("loginEmail").value;
    const password = document.getElementById("loginPassword").value;
    const remember = document.getElementById("rememberMe").checked;

    const signIn = {
        email: email,
        password: password,
        remember: remember
    };

    const signInJson = JSON.stringify(signIn);

    const response = await fetch(
            "SignIn",
            {
                method: "POST",
                body: signInJson,
                headers: {
                    "Content-Type": "application/json"
                }
            }
    );

    if (response.ok) {
        const json = await response.json();

        if (json.status) {

            // ✅ Save credentials if remember me is checked
            if (remember) {
                localStorage.setItem("remembered_email", email);
                localStorage.setItem("remembered_password", password);
                localStorage.setItem("remember_me", "true");
            } else {
                localStorage.removeItem("remembered_email");
                localStorage.removeItem("remembered_password");
                localStorage.removeItem("remember_me");
            }

            if (json.message === "1") {
                const signinModalEl = document.getElementById('loginModal');
                const signinModal = bootstrap.Modal.getInstance(signinModalEl)
                        || new bootstrap.Modal(signinModalEl);
                signinModal.hide();

                const otpModal = new bootstrap.Modal(document.getElementById('otpModal'));
                otpModal.show();
            } else {
                window.location = "home.html";
            }
        } else {
            showToast(json.message);
        }
    } else {
        showToast("Sign In Failed. Please try again!");
    }
}

// ✅ Auto-fill email + password if remembered
function loadRememberedLogin() {
    const remembered = localStorage.getItem("remember_me");
    if (remembered === "true") {
        const emailField = document.getElementById("loginEmail");
        const passwordField = document.getElementById("loginPassword");
        const rememberCheckbox = document.getElementById("rememberMe");

        if (emailField && passwordField && rememberCheckbox) {
            emailField.value = localStorage.getItem("remembered_email") || "";
            passwordField.value = localStorage.getItem("remembered_password") || "";
            rememberCheckbox.checked = true;
        }
    }
}



