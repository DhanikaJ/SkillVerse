package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import hibernate.Cart;
import hibernate.HibernateUtil;
import hibernate.OrderItem;
import hibernate.Orders;
import hibernate.Status;
import hibernate.User;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import model.PayHere;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

@WebServlet(name = "CheckOut", urlPatterns = {"/CheckOut"})
public class CheckOut extends HttpServlet {

    private static final int STATUS_PENDING = 3; // Adjust if needed

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Gson gson = new Gson();
        JsonObject resp = new JsonObject();
        Session s = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;

        try {
            // 1. Session and user check
            HttpSession ses = request.getSession(false);
            if (ses == null || ses.getAttribute("user") == null) {
                resp.addProperty("status", false);
                resp.addProperty("message", "Session expired. Please log in.");
                writeJsonResponse(response, resp, gson);
                return;
            }
            User user = (User) ses.getAttribute("user");
            System.out.println("[DEBUG] Processing checkout for user ID: " + user.getId());

            // 2. Parse request
            JsonObject body = gson.fromJson(request.getReader(), JsonObject.class);
            String fullName = body.get("fullName").getAsString().trim();
            String email = body.get("email").getAsString().trim();

            // 3. Validate
            if (fullName.isEmpty() || email.isEmpty()) {
                resp.addProperty("status", false);
                resp.addProperty("message", "Full name and email are required.");
                writeJsonResponse(response, resp, gson);
                return;
            }

            // 4. Get cart items for user
            Criteria c = s.createCriteria(Cart.class);
            c.add(Restrictions.eq("user", user));
            List<Cart> cartList = c.list();
            if (cartList == null || cartList.isEmpty()) {
                resp.addProperty("status", false);
                resp.addProperty("message", "Your cart is empty.");
                writeJsonResponse(response, resp, gson);
                return;
            }
            System.out.println("[DEBUG] Found " + cartList.size() + " items in cart");

            // 5. Start order transaction
            tx = s.beginTransaction();
            System.out.println("[DEBUG] Started transaction");

            // Create order
            Orders order = new Orders();
            order.setUser(user);
            order.setTotal(0.0); // Will update after items
            Status pendingStatus = (Status) s.get(Status.class, STATUS_PENDING);
            if (pendingStatus == null) {
                throw new RuntimeException("Status with ID " + STATUS_PENDING + " not found");
            }
            order.setStatus(pendingStatus);
            order.setCreated_at(new Date());

            // Save order and get ID
            Integer orderId = (Integer) s.save(order);
            System.out.println("[DEBUG] New order created with ID: " + orderId);

            // Force immediate flush to ensure order is in database
            s.flush();
            System.out.println("[DEBUG] Order flushed to database");

            double total = 0.0;
            StringBuilder itemsList = new StringBuilder();

            // Create order items
            for (int i = 0; i < cartList.size(); i++) {
                Cart cart = cartList.get(i);
                double itemPrice = cart.getCourse().getPrice();
                total += itemPrice;

                OrderItem oi = new OrderItem();
                oi.setOrders(order);
                oi.setCourse(cart.getCourse());
                oi.setPrice(itemPrice);
                s.save(oi);
                System.out.println("[DEBUG] Created order item for course: " + cart.getCourse().getTitle());

                // Build items string (no trailing comma)
                itemsList.append(cart.getCourse().getTitle());
                if (i < cartList.size() - 1) {
                    itemsList.append(", ");
                }
            }

            // Update order total
            order.setTotal(total);
            s.update(order);

            // Commit transaction BEFORE generating PayHere parameters
            tx.commit();
            System.out.println("[DEBUG] Transaction committed successfully. Order ID " + orderId + " is now in database");

            // Verify the order is actually in database after commit
            Orders verifyOrder = (Orders) s.get(Orders.class, orderId);
            if (verifyOrder == null) {
                System.out.println("[ERROR] Order not found after commit! This is a critical error.");
                resp.addProperty("status", false);
                resp.addProperty("message", "Order creation failed. Please try again.");
                writeJsonResponse(response, resp, gson);
                return;
            }
            System.out.println("[DEBUG] Order verification successful. Order " + orderId + " exists with total: " + verifyOrder.getTotal());

            // 6. Prepare PayHere params (after successful commit)
            String merchantId = "1224974";
            String merchantSecret = "OTc2MzYzNzUzNDg2MDkyNjMyMTU1OTY3NDkwMjE4Mzg3OTI4Njc=";
            String orderCode = "#000" + orderId;
            String currency = "LKR";
            String formattedAmount = String.format("%.2f", total);
            String merchantSecretMD5 = PayHere.generateMD5(merchantSecret);
            String hash = PayHere.generateMD5(merchantId + orderCode + formattedAmount + currency + merchantSecretMD5);

            System.out.println("[DEBUG] PayHere Parameters:");
            System.out.println("  merchantId: " + merchantId);
            System.out.println("  orderCode: " + orderCode);
            System.out.println("  amount: " + formattedAmount);
            System.out.println("  currency: " + currency);
            System.out.println("  merchantSecret: " + merchantSecret);
            System.out.println("  merchantSecretMD5: " + merchantSecretMD5);
            System.out.println("  Pre-hash: " + merchantId + orderCode + formattedAmount + currency + merchantSecretMD5);
            System.out.println("  PayHere Hash: " + hash);

            JsonObject payHereJson = new JsonObject();
            payHereJson.addProperty("sandbox", true);
            payHereJson.addProperty("merchant_id", merchantId);
            String returnUrl = "http://localhost:8080/SkillVerse/success.html" +
                              "?order_id=" + orderCode + 
                              "&amount=" + formattedAmount + 
                              "&currency=" + currency;
            payHereJson.addProperty("return_url", returnUrl);
            payHereJson.addProperty("cancel_url", "http://localhost:8080/SkillVerse/cancel.html");
            payHereJson.addProperty("notify_url", "https://c4e991194d5b.ngrok-free.app/SkillVerse/VerifyPayments");
            payHereJson.addProperty("order_id", orderCode);
            payHereJson.addProperty("items", itemsList.toString());
            payHereJson.addProperty("amount", formattedAmount);
            payHereJson.addProperty("currency", currency);
            payHereJson.addProperty("hash", hash);

            // Customer details
            String[] nameParts = fullName.split(" ");
            payHereJson.addProperty("first_name", nameParts.length > 0 ? nameParts[0] : fullName);
            payHereJson.addProperty("last_name", nameParts.length > 1 ? nameParts[1] : "");
            payHereJson.addProperty("email", email);
            payHereJson.addProperty("phone", "");
            payHereJson.addProperty("address", "");
            payHereJson.addProperty("city", "");
            payHereJson.addProperty("country", "Sri Lanka");

            resp.addProperty("status", true);
            resp.add("payhereJson", new Gson().toJsonTree(payHereJson));
            System.out.println("[DEBUG] PayHere parameters prepared successfully for order: " + orderCode);

        } catch (Exception e) {
            System.out.println("[ERROR] Exception in CheckOut: " + e.getMessage());
            e.printStackTrace();

            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                    System.out.println("[DEBUG] Transaction rolled back");
                } catch (Exception rollbackEx) {
                    System.out.println("[ERROR] Failed to rollback transaction: " + rollbackEx.getMessage());
                }
            }

            resp.addProperty("status", false);
            resp.addProperty("message", "Checkout error: " + e.getMessage());
        } finally {
            if (s != null && s.isOpen()) {
                s.close();
                System.out.println("[DEBUG] Hibernate session closed");
            }
        }

        writeJsonResponse(response, resp, gson);
    }

    private void writeJsonResponse(HttpServletResponse response, JsonObject resp, Gson gson) throws IOException {
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(resp));
    }

}
