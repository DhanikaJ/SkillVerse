package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import hibernate.HibernateUtil;
import hibernate.Orders;
import hibernate.Status;
import hibernate.User;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

@WebServlet(name = "GetLatestOrder", urlPatterns = {"/GetLatestOrder"})
public class GetLatestOrder extends HttpServlet {

    private static final int ORDER_STATUS_SUCCESS = 8; // Your success status ID

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Gson gson = new Gson();
        JsonObject resp = new JsonObject();
        Session s = HibernateUtil.getSessionFactory().openSession();

        try {
            // Check user session
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("user") == null) {
                resp.addProperty("status", false);
                resp.addProperty("message", "Session expired. Please log in.");
                writeJsonResponse(response, resp, gson);
                return;
            }

            User user = (User) session.getAttribute("user");
            System.out.println("[DEBUG] Fetching latest order for user ID: " + user.getId());

            // Get the latest successful order for this user
            Criteria criteria = s.createCriteria(Orders.class);
            criteria.add(Restrictions.eq("user", user));

            // Only get successful orders
            Status successStatus = (Status) s.get(Status.class, ORDER_STATUS_SUCCESS);
            if (successStatus != null) {
                criteria.add(Restrictions.eq("status", successStatus));
            }

            // Order by ID descending to get latest
            criteria.addOrder(Order.desc("id"));
            criteria.setMaxResults(1);

            List<Orders> orders = criteria.list();

            if (!orders.isEmpty()) {
                Orders latestOrder = orders.get(0);
                System.out.println("[DEBUG] Found latest order: ID=" + latestOrder.getId()
                        + ", Total=" + latestOrder.getTotal());

                // Create response with order details
                JsonObject orderJson = new JsonObject();
                orderJson.addProperty("id", latestOrder.getId());
                orderJson.addProperty("total", latestOrder.getTotal());
                orderJson.addProperty("status", latestOrder.getStatus().getType());
                orderJson.addProperty("created_at", latestOrder.getCreated_at().toString());

                resp.addProperty("status", true);
                resp.add("order", orderJson);

            } else {
                System.out.println("[DEBUG] No successful orders found for user");
                resp.addProperty("status", false);
                resp.addProperty("message", "No orders found");
            }

        } catch (Exception e) {
            System.out.println("[ERROR] Exception in GetLatestOrder: " + e.getMessage());
            e.printStackTrace();
            resp.addProperty("status", false);
            resp.addProperty("message", "Failed to fetch order details");
        } finally {
            s.close();
        }

        writeJsonResponse(response, resp, gson);
    }

    private void writeJsonResponse(HttpServletResponse response, JsonObject resp, Gson gson) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(gson.toJson(resp));
    }

}
