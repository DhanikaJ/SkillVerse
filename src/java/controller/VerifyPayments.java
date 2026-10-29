package controller;

import hibernate.Cart;
import hibernate.Enrollment;
import hibernate.HibernateUtil;
import hibernate.OrderItem;
import hibernate.Orders;
import hibernate.Payment;
import hibernate.PaymentMethod;
import hibernate.Status;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.PayHere;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

@WebServlet(name = "VerifyPayments", urlPatterns = {"/VerifyPayments"})
public class VerifyPayments extends HttpServlet {

    private static final int PAYHERE_SUCCESS = 2;
    private static final int ORDER_STATUS_SUCCESS = 8;
    private static final int PAYMENT_METHOD_PAYHERE = 1;
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final int ENROLLMENT_STATUS = 6;
    private static final long RETRY_DELAY_MS = 2000; // 2 seconds

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("\n=== PayHere Payment Verification Started ===");

        // Extract and validate PayHere parameters
        String merchant_id = request.getParameter("merchant_id");
        String order_id = request.getParameter("order_id");
        String payhere_amount = request.getParameter("payhere_amount");
        String payhere_currency = request.getParameter("payhere_currency");
        String status_code = request.getParameter("status_code");
        String md5sig = request.getParameter("md5sig");

        System.out.println("Received Parameters:");
        System.out.println("  merchant_id: " + merchant_id);
        System.out.println("  order_id: " + order_id);
        System.out.println("  payhere_amount: " + payhere_amount);
        System.out.println("  payhere_currency: " + payhere_currency);
        System.out.println("  status_code: " + status_code);
        System.out.println("  md5sig: " + md5sig);

        if (merchant_id == null || order_id == null || payhere_amount == null
                || payhere_currency == null || status_code == null || md5sig == null) {
            System.out.println("[ERROR] Missing required parameters");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            // Hash verification
            String merchantSecret = "OTc2MzYzNzUzNDg2MDkyNjMyMTU1OTY3NDkwMjE4Mzg3OTI4Njc=";
            String merchantSecretMD5 = PayHere.generateMD5(merchantSecret);
            String calculatedHash = PayHere.generateMD5(merchant_id + order_id + payhere_amount + payhere_currency + status_code + merchantSecretMD5);

            System.out.println("Hash Verification:");
            System.out.println("  Calculated hash: " + calculatedHash);
            System.out.println("  PayHere hash: " + md5sig);

            if (!calculatedHash.equals(md5sig)) {
                System.out.println("[ERROR] Hash verification failed!");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            if (Integer.parseInt(status_code) != PAYHERE_SUCCESS) {
                System.out.println("[WARNING] Payment not successful. Status code: " + status_code);
                response.setStatus(HttpServletResponse.SC_OK);
                return;
            }

            System.out.println("[SUCCESS] Hash verification passed!");

            // Extract order ID
            String orderIdString = order_id.replaceAll("\\D+", "");
            if (orderIdString.isEmpty()) {
                System.out.println("[ERROR] Could not extract order ID from: " + order_id);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            int orderIdInt = Integer.parseInt(orderIdString);
            System.out.println("[DEBUG] Processing payment for order ID: " + orderIdInt);

            // Process payment with retry mechanism
            boolean paymentProcessed = processPaymentWithRetry(orderIdInt, payhere_amount, order_id);

            if (paymentProcessed) {
                System.out.println("[SUCCESS] Payment processing completed successfully!");
                response.setStatus(HttpServletResponse.SC_OK);
            } else {
                System.out.println("[ERROR] Payment processing failed after all retries");
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

        } catch (Exception e) {
            System.out.println("[ERROR] Exception during payment verification: " + e.getMessage());
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            System.out.println("=== PayHere Payment Verification Ended ===\n");
        }
    }

    private boolean processPaymentWithRetry(int orderIdInt, String payhereAmount, String orderId) {
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            System.out.println("[DEBUG] Attempt " + attempt + "/" + MAX_RETRY_ATTEMPTS + " to process payment for order " + orderIdInt);

            Session s = null;
            Transaction tr = null;

            try {
                // Create fresh session for each attempt
                s = HibernateUtil.getSessionFactory().openSession();

                // Clear any cached data
                s.clear();

                tr = s.beginTransaction();

                // Try to find the order using multiple strategies
                Orders order = findOrderWithMultipleStrategies(s, orderIdInt);

                if (order == null) {
                    System.out.println("[WARNING] Order " + orderIdInt + " not found in attempt " + attempt);

                    if (attempt < MAX_RETRY_ATTEMPTS) {
                        System.out.println("[DEBUG] Retrying in " + RETRY_DELAY_MS + "ms...");
                        Thread.sleep(RETRY_DELAY_MS);
                        continue;
                    } else {
                        System.out.println("[ERROR] Order " + orderIdInt + " not found after " + MAX_RETRY_ATTEMPTS + " attempts");
                        return false;
                    }
                }

                System.out.println("[SUCCESS] Order found on attempt " + attempt
                        + ": ID=" + order.getId() + ", Total=" + order.getTotal());

                // Verify amount
                double orderTotal = order.getTotal();
                double payhereAmountDouble = Double.parseDouble(payhereAmount);
                if (Math.abs(orderTotal - payhereAmountDouble) > 0.01) {
                    System.out.println("[ERROR] Amount mismatch! Order: " + orderTotal
                            + ", PayHere: " + payhereAmountDouble);
                    return false;
                }

                // Process the successful payment
                boolean success = processSuccessfulPayment(s, order, orderId);

                if (success) {
                    tr.commit();
                    System.out.println("[SUCCESS] Payment processed successfully on attempt " + attempt);
                    return true;
                } else {
                    tr.rollback();
                    System.out.println("[ERROR] Failed to process payment on attempt " + attempt);
                    return false;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("[ERROR] Thread interrupted during retry");
                return false;
            } catch (Exception e) {
                System.out.println("[ERROR] Exception on attempt " + attempt + ": " + e.getMessage());
                if (tr != null && tr.isActive()) {
                    try {
                        tr.rollback();
                    } catch (Exception rollbackEx) {
                        System.out.println("[ERROR] Rollback failed: " + rollbackEx.getMessage());
                    }
                }

                if (attempt < MAX_RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                } else {
                    e.printStackTrace();
                    return false;
                }
            } finally {
                if (s != null && s.isOpen()) {
                    s.close();
                }
            }
        }

        return false;
    }

    private Orders findOrderWithMultipleStrategies(Session s, int orderIdInt) {
        System.out.println("[DEBUG] Trying multiple strategies to find order " + orderIdInt);

        // Strategy 1: Direct get
        Orders order = (Orders) s.get(Orders.class, orderIdInt);
        if (order != null) {
            System.out.println("[DEBUG] Order found using s.get()");
            return order;
        }

        // Strategy 2: Criteria query
        Criteria criteria = s.createCriteria(Orders.class);
        criteria.add(Restrictions.eq("id", orderIdInt));
        order = (Orders) criteria.uniqueResult();
        if (order != null) {
            System.out.println("[DEBUG] Order found using Criteria");
            return order;
        }

        // Strategy 3: Clear session and try again
        s.clear();
        order = (Orders) s.get(Orders.class, orderIdInt);
        if (order != null) {
            System.out.println("[DEBUG] Order found after clearing session");
            return order;
        }

        // Strategy 4: Manual refresh of session factory (if needed)
        try {
            s.getSessionFactory().getCurrentSession().clear();
        } catch (Exception e) {
            // Ignore if getCurrentSession fails
        }

        order = (Orders) s.get(Orders.class, orderIdInt);
        if (order != null) {
            System.out.println("[DEBUG] Order found after session factory refresh");
            return order;
        }

        // Debug: Show what orders are actually in the database
        System.out.println("[DEBUG] Orders currently in database:");
        List<Orders> allOrders = s.createCriteria(Orders.class).list();
        for (Orders o : allOrders) {
            System.out.println("  -> Order ID: " + o.getId() + ", Total: " + o.getTotal());
        }

        System.out.println("[DEBUG] Order " + orderIdInt + " not found with any strategy");
        return null;
    }

    private boolean processSuccessfulPayment(Session s, Orders order, String orderId) {
        try {
            // Update order status
            Status successStatus = (Status) s.get(Status.class, ORDER_STATUS_SUCCESS);
            if (successStatus == null) {
                System.out.println("[ERROR] Success status not found");
                return false;
            }

            order.setStatus(successStatus);
            s.update(order);

            // Get order items
            Criteria itemsCriteria = s.createCriteria(OrderItem.class);
            itemsCriteria.add(Restrictions.eq("orders", order));
            List<OrderItem> items = itemsCriteria.list();

            // Process each item
            for (OrderItem item : items) {
                // Create payment record
                Payment payment = new Payment();
                payment.setUser(order.getUser());
                payment.setCourse(item.getCourse());
                payment.setAmount(item.getPrice());

                PaymentMethod paymentMethod = (PaymentMethod) s.get(PaymentMethod.class, PAYMENT_METHOD_PAYHERE);
                if (paymentMethod != null) {
                    payment.setPaymentmethod(paymentMethod);
                }

                payment.setStatus(successStatus);
                payment.setTxn_reference(orderId);
                payment.setPaid_at(new Date());
                s.save(payment);

                // Create enrollment (check for duplicates)
                Criteria enrollmentCheck = s.createCriteria(Enrollment.class);
                enrollmentCheck.add(Restrictions.eq("user", order.getUser()));
                enrollmentCheck.add(Restrictions.eq("course", item.getCourse()));
                List<Enrollment> existingEnrollments = enrollmentCheck.list();

                if (existingEnrollments.isEmpty()) {
                    Enrollment enrollment = new Enrollment();
                    enrollment.setUser(order.getUser());
                    enrollment.setCourse(item.getCourse());
                    enrollment.setEnrolled_at(new Date());
                    enrollment.setProgress(0);
                    enrollment.setStatus(successStatus);
                    s.save(enrollment);
                }
            }

            // Clear cart
            Criteria cartCriteria = s.createCriteria(Cart.class);
            cartCriteria.add(Restrictions.eq("user", order.getUser()));
            List<Cart> cartItems = cartCriteria.list();

            for (Cart cart : cartItems) {
                s.delete(cart);
            }

            System.out.println("[DEBUG] Payment processing completed: " + items.size()
                    + " items, " + cartItems.size() + " cart items cleared");
            return true;

        } catch (Exception e) {
            System.out.println("[ERROR] Failed to process successful payment: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
