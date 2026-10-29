package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import hibernate.Cart;
import hibernate.HibernateUtil;
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
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

@WebServlet(name = "RemoveCartItemServlet", urlPatterns = {"/RemoveCartItem"})
public class RemoveCartItemServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Gson gson = new Gson();
        JsonObject resp = new JsonObject();
        HttpSession ses = request.getSession(false);
        User user = (ses != null) ? (User) ses.getAttribute("user") : null;

        // Read JSON from POST body
        JsonObject reqBody = gson.fromJson(request.getReader(), JsonObject.class);
        int courseId = reqBody.has("courseId") ? reqBody.get("courseId").getAsInt() : 0;

        Session s = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            if (user != null) {
                // Logged-in: Remove by user and course
                Criteria c = s.createCriteria(Cart.class);
                c.add(Restrictions.eq("user", user));
                c.add(Restrictions.eq("course.id", courseId));
                List<Cart> cartList = c.list();

                if (!cartList.isEmpty()) {
                    tx = s.beginTransaction();
                    for (Cart cart : cartList) {
                        s.delete(cart);
                    }
                    tx.commit();
                    resp.addProperty("status", true);
                    resp.addProperty("message", "Removed from cart.");
                } else {
                    resp.addProperty("status", false);
                    resp.addProperty("message", "Course not found in cart.");
                }
            } else {
                // Guest: Remove by session_id and course
                String sessionId = (ses != null) ? ses.getId() : "";
                Criteria c = s.createCriteria(Cart.class);
                c.add(Restrictions.isNull("user"));
                c.add(Restrictions.eq("session_id", sessionId));
                c.add(Restrictions.eq("course.id", courseId));
                List<Cart> cartList = c.list();

                if (!cartList.isEmpty()) {
                    tx = s.beginTransaction();
                    for (Cart cart : cartList) {
                        s.delete(cart);
                    }
                    tx.commit();
                    resp.addProperty("status", true);
                    resp.addProperty("message", "Removed from cart.");
                } else {
                    resp.addProperty("status", false);
                    resp.addProperty("message", "Course not found in cart.");
                }
            }
        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            e.printStackTrace();
            resp.addProperty("status", false);
            resp.addProperty("message", "Error: " + e.getMessage());
        } finally {
            s.close();
        }
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(resp));
    }
}
