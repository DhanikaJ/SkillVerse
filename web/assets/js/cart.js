// Fetch and display real cart data in the cart drawer
async function fetchCartItems() {
    try {
        const res = await fetch("Cart");
        const txt = await res.text();
        let data;
        try {
            data = JSON.parse(txt);
        } catch (err) {
            return [];
        }
        if (!data.status)
            return [];
        return data.cartItems || [];
    } catch (err) {
        return [];
    }
}

async function renderCart() {
    const items = await fetchCartItems();
    let html = "";
    items.forEach((item, i) => {
        const course = item.course || {};
        html += `
            <div class="d-flex align-items-center mb-3 cart-item" style="background:#222a38;border-radius:12px;padding:10px;">
                <img src="${course.thumbnail || 'assets/images/courses/default.jpg'}" width="54" height="54" class="rounded-3 me-3" style="object-fit:cover;">
                <div class="flex-grow-1">
                    <div class="fw-semibold">${course.title || "Untitled"}</div>
                    <div class="text-warning">LKR ${Number(course.price || 0).toLocaleString("en-US", {minimumFractionDigits: 2})}</div>
                </div>
                <button class="btn btn-sm btn-outline-danger ms-2 px-2 py-0" data-id="${course.id}">
                    <i class="ri-delete-bin-6-line"></i>
                </button>
            </div>`;
    });
    document.getElementById("cartItems").innerHTML = html || `<div class='text-muted py-5 text-center'>Your cart is empty.</div>`;
    // Animate cart items in!
    if (items.length) {
        gsap.from(".cart-item", {
            x: 60,
            opacity: 0,
            duration: 0.45,
            stagger: 0.11,
            ease: "back"
        });
    }
    document.getElementById('cart-count').textContent = items.length;
}

// Open Cart Drawer
function cartIcon() {
    document.getElementById("cartOverlay").style.display = "block";
    document.getElementById("cartItems").innerHTML = `<div class='text-center py-5'><div class="spinner-border text-primary"></div></div>`;
    gsap.to("#cartDrawer", {right: 0, duration: 0.43, ease: "power4.out"});
    gsap.fromTo("#cartDrawer", {boxShadow: "0 0 0 0 #0000"}, {boxShadow: "-4px 0 32px 0 #2196f344", duration: 0.6, delay: 0.12});
    renderCart();
}

// Close Cart Drawer
function closeCart() {
    gsap.to("#cartDrawer", {right: -430, duration: 0.35, ease: "power2.in", onComplete: () => {
            document.getElementById("cartOverlay").style.display = "none";
        }});
}
document.getElementById("cartClose").onclick = closeCart;
document.getElementById("cartOverlay").onclick = closeCart;

// Remove item from cart (live)
document.addEventListener("click", async function (e) {
    const btn = e.target.closest('.btn-outline-danger[data-id]');
    if (btn && document.getElementById("cartItems").contains(btn)) {
        const courseId = btn.getAttribute('data-id');
        // Use your RemoveCartItemServlet (POST), or Cart DELETE if working
        try {
            const res = await fetch("RemoveCartItem", {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({courseId})
            });
            const txt = await res.text();
            let data;
            try {
                data = JSON.parse(txt);
            } catch (err) {
                showToast("Server error removing course");
                return;
            }
            showToast(data.message || "Removed.");
            renderCart(); // Refresh drawer with latest DB state
        } catch (err) {
            showToast("Remove failed.");
        }
    }
});


async function updateCartCount() {
    try {
        const res = await fetch("CartCount");
        const data = await res.json();
        const count = data.count || 0;
        document.getElementById('cart-count').textContent = count;
    } catch (err) {
        document.getElementById('cart-count').textContent = 0;
    }
}




// cart.js

function showToast(message) {
    const toastBody = document.getElementById("toastBody");
    toastBody.innerText = message;

    const toastEl = document.getElementById("cartToast");
    const toast = new bootstrap.Toast(toastEl);
    toast.show();
}

async function loadCart() {
    try {
        const res = await fetch("Cart");
        const txt = await res.text();
        let data;
        try {
            data = JSON.parse(txt);
        } catch (err) {
            throw new Error("Cart response not JSON");
        }
        if (!data.status)
            throw new Error(data.message || "Could not load cart.");
        renderCartItems(data.cartItems || []);
        updateCartSummary(data.cartItems || []);
    } catch (err) {
        const cartCard = document.querySelector(".cart-card");
        if (cartCard)
            cartCard.innerHTML = `<div class="text-danger py-5">${err.message || "Failed to load cart."}</div>`;
        updateCartSummary([]);
    }
}

function renderCartItems(items) {
    const cartCard = document.querySelector('.cart-card');
    if (!cartCard)
        return;
    if (!items.length) {
        cartCard.innerHTML = `<div class="text-muted py-5">Your cart is empty.</div>`;
        return;
    }
    cartCard.innerHTML = '';
    items.forEach(item => {
        const course = item.course || {};
        const title = course.title || "Untitled";
        const instructor = course.user ? (course.user.fname || "Unknown") : "Unknown";
        const price = typeof course.price === "number" ? course.price : 0;
        const img = course.thumbnail || "assets/images/courses/default.jpg";
        const id = course.id || "";

        const el = document.createElement('div');
        el.className = 'cart-item d-flex align-items-center mb-3';
        el.innerHTML = `
            <img src="${img}" alt="Course" style="width:80px; height:80px; object-fit:cover; border-radius:8px; margin-right:16px;">
            <div class="flex-grow-1">
                <div class="cart-item-title mb-1">${title}</div>
                <div class="text-muted mb-1">by ${instructor}</div>
                <div class="cart-item-price">LKR ${Number(price).toLocaleString("en-US", {minimumFractionDigits: 2})}</div>
            </div>
            <button class="cart-remove-btn ms-3 btn btn-link text-danger" data-id="${id}">
                <i class="ri-delete-bin-6-line"></i>
            </button>
        `;
        cartCard.appendChild(el);
    });

    // Attach remove event listeners
    cartCard.querySelectorAll('.cart-remove-btn').forEach(btn => {
        btn.addEventListener('click', async function () {
            const courseId = btn.getAttribute('data-id');
            await removeFromCart(courseId); // Calls the function below
        });
    });
}


function updateCartSummary(items) {
    let subtotal = 0;
    items.forEach(item => {
        if (item.course && typeof item.course.price === "number")
            subtotal += item.course.price;
    });

    const subtotalElem = document.getElementById("subtotal-amount");
    if (subtotalElem)
        subtotalElem.innerText = `LKR ${subtotal.toLocaleString("en-US", {minimumFractionDigits: 2})}`;
    else
        console.log("subtotal-amount element not found!");

    const totalElem = document.getElementById("total-amount");
    if (totalElem)
        totalElem.innerText = `LKR ${subtotal.toLocaleString("en-US", {minimumFractionDigits: 2})}`;
    else
        console.log("total-amount element not found!");
    // Taxes are zero in this LMS sample
}



// Remove from cart (DELETE /Cart)
async function removeFromCart(courseId) {
    try {
        const res = await fetch("RemoveCartItem", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({courseId})
        });
        const txt = await res.text();
        let data;
        try {
            data = JSON.parse(txt);
        } catch (err) {
            showToast("Server error removing course");
            return;
        }
        showToast(data.message || "Remove failed.");
        if (data.status) {
            loadCart();
        }
    } catch (err) {
        showToast(err.message || "Remove failed.");
    }
}


async function clearCart() {
    try {
        const res = await fetch("Cart?clear=1", {method: "DELETE"});
        const txt = await res.text();
        let data;
        try {
            data = JSON.parse(txt);
        } catch (err) {
            showToast("Server error clearing cart");
            return;
        }
        showToast(data.message || "Cart cleared.");
        loadCart();
    } catch (err) {
        showToast("Cart clear failed.");
    }

}