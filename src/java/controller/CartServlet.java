package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import hibernate.Cart;
import hibernate.Course;
import hibernate.HibernateUtil;
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
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

@WebServlet(name = "CartServlet", urlPatterns = {"/Cart"})
public class CartServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession ses = request.getSession(false);
        if (ses != null) {
            if (ses.getAttribute("cart") != null) {
                System.out.println("[DEBUG] CartServlet found session 'cart' attribute. Removing it. Path: " + request.getServletPath());
                ses.removeAttribute("cart");
            }
        }
        Gson gson = new Gson();
        JsonObject resp = new JsonObject();
        User user = (ses != null) ? (User) ses.getAttribute("user") : null;

        List<Cart> cartList;
        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            if (user != null) {
                Criteria c = s.createCriteria(Cart.class);
                c.add(Restrictions.eq("user", user));
                cartList = c.list();
            } else {
                String sessionId = (ses != null) ? ses.getId() : "";
                Criteria c = s.createCriteria(Cart.class);
                c.add(Restrictions.isNull("user"));
                c.add(Restrictions.eq("session_id", sessionId));
                cartList = c.list();
            }
            resp.addProperty("status", true);
            resp.add("cartItems", gson.toJsonTree(cartList));
            System.out.println("[get] Returning cartList with size: " + (cartList != null ? cartList.size() : 0) + " for user: " + (user != null ? user.getId() : "guest, session_id=" + (ses != null ? ses.getId() : "")));
        } catch (Exception e) {
            e.printStackTrace();
            resp.addProperty("status", false);
            resp.addProperty("message", "Error loading cart: " + e.getMessage());
        } finally {
            s.close();
        }
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(resp));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Gson gson = new Gson();
        JsonObject resp = new JsonObject();

        JsonObject reqBody = gson.fromJson(request.getReader(), JsonObject.class);
        int courseId = reqBody.has("courseId") ? reqBody.get("courseId").getAsInt() : 0;

        HttpSession ses = request.getSession();
        if (ses != null) {
            if (ses.getAttribute("cart") != null) {
                System.out.println("[post] CartServlet found session 'cart' attribute. Removing it. Path: " + request.getServletPath());
                ses.removeAttribute("cart");
            }
        }
        User user = (User) ses.getAttribute("user");

        if (courseId <= 0) {
            resp.addProperty("status", false);
            resp.addProperty("message", "Invalid course.");
            writeJsonResponse(response, resp, gson);
            return;
        }

        Session s = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            Course course = (Course) s.get(Course.class, courseId);
            if (course == null) {
                resp.addProperty("status", false);
                resp.addProperty("message", "Course not found.");
            } else if (user != null) {
                // Logged-in: Only user & course, never session_id!
                Criteria c = s.createCriteria(Cart.class);
                c.add(Restrictions.eq("user", user));
                c.add(Restrictions.eq("course", course));
                List<Cart> existing = c.list();

                if (existing.isEmpty()) {
                    tx = s.beginTransaction();
                    Cart cartItem = new Cart();
                    cartItem.setUser(user);
                    cartItem.setCourse(course);
                    cartItem.setAdded_at(new Date());
                    cartItem.setSession_id(null); // Always null for logged-in
                    s.save(cartItem);
                    tx.commit();
                    resp.addProperty("status", true);
                    resp.addProperty("message", "Added to cart.");
                } else {
                    resp.addProperty("status", false);
                    resp.addProperty("message", "Already in cart.");
                }
            } else {
                // Guest: Use session_id, user is null
                String sessionId = ses.getId();
                Criteria c = s.createCriteria(Cart.class);
                c.add(Restrictions.isNull("user"));
                c.add(Restrictions.eq("session_id", sessionId));
                c.add(Restrictions.eq("course", course));
                List<Cart> existing = c.list();

                if (existing.isEmpty()) {
                    tx = s.beginTransaction();
                    Cart cartItem = new Cart();
                    cartItem.setUser(null);
                    cartItem.setSession_id(sessionId);
                    cartItem.setCourse(course);
                    cartItem.setAdded_at(new Date());
                    s.save(cartItem);
                    tx.commit();
                    resp.addProperty("status", true);
                    resp.addProperty("message", "Added to cart.");
                } else {
                    resp.addProperty("status", false);
                    resp.addProperty("message", "Already in cart.");
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
        writeJsonResponse(response, resp, gson);
    }

    private void writeJsonResponse(HttpServletResponse response, JsonObject resp, Gson gson) throws IOException {
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(resp));
    }
}
