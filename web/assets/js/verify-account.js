async function verifyAccount() {

    const verificationCode = document.getElementById("verificationCode").value;

    const verification = {
        verificationCode: verificationCode
    };

    const verificationJson = JSON.stringify(verification);
    const response = await fetch(
            "VerifyAccount",
            {
                method: "POST",
                body: verificationJson,
                header: {
                    "Content-Type": "application/json"
                }
            }
    );

    if (response.ok) {
        const json = await response.json();

        if (json.status) {
            window.location = "home.html";
        } else {
            if (json.message === "1") {  //Email not Found!
                const signupModalEl = document.getElementById('otpModal');
            const signupModal = bootstrap.Modal.getInstance(signupModalEl)
                    || new bootstrap.Modal(signupModalEl);
            signupModal.hide();

            const otpModal = new bootstrap.Modal(document.getElementById('loginModal'));
            otpModal.show();
            } else {
                showToast(responseJson.message);
            }
        }
    } else {
        showToast("Verification failed. Please try again.");
    }
}