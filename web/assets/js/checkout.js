function showToast(message) {
    const toastBody = document.getElementById("toastBody");
    toastBody.innerText = message;

    const toastEl = document.getElementById("checkoutToast");
    const toast = new bootstrap.Toast(toastEl);
    toast.show();
}

// --- 1. Load User & Cart Data for Checkout ---
async function loadCheckoutData() {
    try {
        const res = await fetch("CheckoutUserCart");
        const json = await res.json();

        if (!json.status) {
            showToast(json.message || "Session expired. Please log in.");
            window.location = "sign-in.html";
            return;
        }

        // Fill user details
        document.getElementById("fullName").value = json.user.fullName || "";
        document.getElementById("email").value = json.user.email || "";

        // Render order summary
        const cartItems = json.cartItems || [];
        let total = 0;
        let itemsHtml = "";

        cartItems.forEach(cart => {
            itemsHtml += `
                <div class="d-flex justify-content-between align-items-center mb-2">
                    <span>${cart.course.title}</span>
                    <span>LKR ${Number(cart.course.price).toLocaleString("en-US", {minimumFractionDigits: 2})}</span>
                </div>
            `;
            total += Number(cart.course.price);
        });

        document.getElementById("checkout-items").innerHTML = itemsHtml || '<div class="text-muted">No items in cart.</div>';
        document.getElementById("checkout-total").innerText = "LKR " + total.toLocaleString("en-US", {minimumFractionDigits: 2});

        // Store total for validation/debug
        window.skillverseCheckoutTotal = total;


    } catch (err) {
        showToast("Could not load checkout details. Please refresh.");
    }
}

// Enhanced validation and debugging for PayHere parameters
function validatePayHereParams(payhereJson) {

    // Required parameters for PayHere
    const requiredFields = [
        "sandbox", "merchant_id", "return_url", "cancel_url", "notify_url",
        "order_id", "items", "amount", "currency", "hash",
        "first_name", "last_name", "email"
    ];

    const errors = [];

    // Check required fields
    requiredFields.forEach(field => {
        if (!payhereJson[field] && payhereJson[field] !== false) {
            errors.push(`Missing required field: ${field}`);
        }
    });

    // Validate specific field formats
    if (payhereJson.amount && isNaN(parseFloat(payhereJson.amount))) {
        errors.push("Amount must be a valid number");
    }

    if (payhereJson.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(payhereJson.email)) {
        errors.push("Invalid email format");
    }

    if (payhereJson.merchant_id && payhereJson.merchant_id.length === 0) {
        errors.push("Merchant ID cannot be empty");
    }

    // Check for common issues
    if (payhereJson.items && payhereJson.items.endsWith(", ")) {
        errors.push("Items string should not end with comma and space");
    }

    if (payhereJson.hash && payhereJson.hash.length !== 32) {
        errors.push("Hash should be 32 characters (MD5)");
    }


    if (errors.length > 0) {
        return {valid: false, errors};
    }

    return {valid: true, errors: []};
}

// Updated checkout function with better validation
async function checkout(event) {
    event.preventDefault();

    const fullName = document.getElementById("fullName").value.trim();
    const email = document.getElementById("email").value.trim();

    if (!fullName || !email) {
        showToast("Please enter your full name and email.");
        return;
    }

    // Show loading state
    const submitBtn = document.querySelector('.payhere-btn');
    const originalText = submitBtn.innerHTML;
    submitBtn.innerHTML = '<i class="ri-loader-4-line"></i> Processing...';
    submitBtn.disabled = true;

    try {
        const res = await fetch("CheckOut", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({fullName, email})
        });

        const json = await res.json();

        if (json.status && json.payhereJson) {
            // Validate parameters before sending to PayHere
            const validation = validatePayHereParams(json.payhereJson);

            if (!validation.valid) {
                showToast("Payment setup error: " + validation.errors.join(", "));
                return;
            }
            
            // Store order data for success page BEFORE starting payment
            const orderData = {
                orderId: json.payhereJson.order_id,
                amount: json.payhereJson.amount,
                currency: json.payhereJson.currency
            };
            storeOrderDataForSuccess(orderData.orderId, orderData.amount, orderData.currency);

            // Wait for PayHere SDK
            if (typeof payhere === "undefined") {
                throw new Error("PayHere SDK not loaded");
            }

            // Setup handlers
            setupPayHereHandlers();

            // Remove any properties that might cause issues
            const cleanParams = {...json.payhereJson};
            delete cleanParams.iframe; // Remove iframe if it exists


            try {
                payhere.startPayment(cleanParams);
            } catch (payhereError) {
                throw new Error("Failed to initialize PayHere: " + payhereError.message);
            }

        } else {
            throw new Error(json.message || "Checkout failed");
        }

    } catch (err) {
        showToast("Could not complete checkout: " + err.message);
    } finally {
        // Reset button
        submitBtn.innerHTML = originalText;
        submitBtn.disabled = false;
    }
}

// --- 3. Setup PayHere Event Handlers ---
function setupPayHereHandlers() {
    if (typeof payhere === "undefined") {
        return;
    }

    payhere.onCompleted = function (orderId) {
        showToast("Payment successful! Redirecting to orders...");
        // Update stored order data with completion timestamp
        try {
            const storedData = localStorage.getItem('skillverse_recent_order');
            if (storedData) {
                const orderData = JSON.parse(storedData);
                orderData.completedAt = new Date().toISOString();
                orderData.payhereOrderId = orderId;
                localStorage.setItem('skillverse_recent_order', JSON.stringify(orderData));
            }
        } catch (e) {
        }
        
        setTimeout(() => {
            window.location = "success.html";
        }, 500);
    };

    payhere.onDismissed = function () {
        showToast("Payment was cancelled.");
        
        // Clear stored order data since payment was cancelled
        try {
            localStorage.removeItem('skillverse_recent_order');
        } catch (e) {
        }
    };

    payhere.onError = function (error) {
        showToast("Payment failed: " + error);
        
        // Clear stored order data since payment failed
        try {
            localStorage.removeItem('skillverse_recent_order');
        } catch (e) {
            console.warn("Could not clear order data:", e);
        }
    };
}

// --- 4. Initialize on DOM Ready ---
document.addEventListener("DOMContentLoaded", function () {

    // Load checkout data
    loadCheckoutData();

    // Bind form submission
    const checkoutForm = document.getElementById("checkoutForm");
    if (checkoutForm) {
        checkoutForm.addEventListener("submit", checkout);
    }

    // Check if PayHere SDK is loaded
    setTimeout(() => {
        if (typeof payhere === "undefined") {
            showToast("Payment system loading failed. Please refresh the page.");
        } else {
            console.log("✅ PayHere SDK loaded successfully");
        }
    }, 3000);
});

// Store order data for success page
function storeOrderDataForSuccess(orderId, amount, currency = 'LKR') {
    try {
        const orderData = {
            orderId: orderId,
            amount: amount,
            currency: currency,
            timestamp: new Date().toISOString()
        };
        // Store in localStorage for success page to retrieve
        localStorage.setItem('skillverse_recent_order', JSON.stringify(orderData));
    } catch (e) {
        console.error("Could not store order data:", e);
    }
}