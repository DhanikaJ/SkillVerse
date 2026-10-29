document.addEventListener("DOMContentLoaded", function () {
    loadPurchases();
});

async function loadPurchases() {
    const container = document.getElementById("purchase-cards-container");
    container.innerHTML = "<div class='text-center text-secondary my-4'>Loading...</div>";

    try {
        const res = await fetch("MyPurchases");
        if (!res.ok) throw new Error("Failed to load purchases");
        const data = await res.json();
        container.innerHTML = "";

        if (!data.status || !data.purchases || data.purchases.length === 0) {
            container.innerHTML = "<div class='text-center text-secondary my-4'>No purchases found.</div>";
            return;
        }

        data.purchases.forEach(p => {
            container.innerHTML += `
                <div class="purchase-course-card">
                    <img src="${p.thumbnail}" alt="Course" class="purchase-thumb">
                    <div class="purchase-info">
                        <div class="purchase-title-course">${p.title}</div>
                        <div class="purchase-meta mb-1">by ${p.instructor} • ${p.difficulty}</div>
                        <div class="purchase-date mb-2"><i class="ri-calendar-check-line me-1"></i>Purchased: ${p.purchased_at}</div>
                    </div>
                    <div class="text-end me-2">
                        <div class="purchase-price">LKR ${Number(p.price).toLocaleString("en-LK", {minimumFractionDigits:2})}</div>
                    </div>
                    <div class="purchase-actions">
                        <a href="lesson.html?id=${p.course_id}" class="btn purchase-btn purchase-btn-primary mb-1">
                            <i class="ri-play-circle-line me-1"></i>Go to Course
                        </a>
                        ${
                            p.certificate_url ?
                            `<a href="${p.certificate_url}" target="_blank" class="btn purchase-btn purchase-btn-outline"><i class="ri-award-line me-1"></i>Download Certificate</a>` :
                            `<button class="btn purchase-btn purchase-btn-outline" disabled><i class="ri-award-line me-1"></i>Certificate Pending</button>`
                        }
                    </div>
                </div>
            `;
        });
    } catch (e) {
        container.innerHTML = "<div class='text-center text-danger my-4'>Error loading purchases.</div>";
    }
}
