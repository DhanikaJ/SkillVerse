package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import hibernate.Cart;
import hibernate.HibernateUtil;
import hibernate.User;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

@WebServlet(name = "CheckoutUserCart", urlPatterns = {"/CheckoutUserCart"})
public class CheckoutUserCart extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Gson gson = new Gson();
        JsonObject resp = new JsonObject();
        HttpSession ses = request.getSession(false);
        User user = (ses != null) ? (User) ses.getAttribute("user") : null;

        if (user == null) {
            resp.addProperty("status", false);
            resp.addProperty("message", "User not logged in.");
            response.setContentType("application/json");
            response.getWriter().write(gson.toJson(resp));
            return;
        }
        // Compose fullName as Firstname + Lastname
        String fullName = (user.getFname() + " " + user.getLname()).trim();

        // Cart
        Session s = HibernateUtil.getSessionFactory().openSession();
        List<Cart> cartList = new ArrayList<>();
        try {
            Criteria c = s.createCriteria(Cart.class);
            c.add(Restrictions.eq("user", user));
            cartList = c.list();
            // (Clear sensitive fields from cart if needed)
            for (Cart cart : cartList) {
                if (cart.getCourse() != null && cart.getCourse().getUser() != null) {
                    cart.getCourse().setUser(null);
                }
            }
        } finally {
            s.close();
        }

        // User info
        JsonObject userObj = new JsonObject();
        userObj.addProperty("fullName", fullName);
        userObj.addProperty("email", user.getEmail());

        resp.addProperty("status", true);
        resp.add("user", userObj);
        resp.add("cartItems", gson.toJsonTree(cartList));

        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(resp));
    }
}
