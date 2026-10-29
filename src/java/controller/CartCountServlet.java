package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import hibernate.Cart;
import hibernate.HibernateUtil;
import hibernate.User;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

@WebServlet(name = "CartCountServlet", urlPatterns = {"/CartCount"})
public class CartCountServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Gson gson = new Gson();
        JsonObject resp = new JsonObject();
        HttpSession ses = request.getSession(false);
        User user = (ses != null) ? (User) ses.getAttribute("user") : null;

        int count = 0;
        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            if (user != null) {
                Criteria c = s.createCriteria(Cart.class);
                c.add(Restrictions.eq("user", user));
                count = c.list().size();
            } else {
                String sessionId = (ses != null) ? ses.getId() : "";
                Criteria c = s.createCriteria(Cart.class);
                c.add(Restrictions.isNull("user"));
                c.add(Restrictions.eq("session_id", sessionId));
                count = c.list().size();
            }
            resp.addProperty("status", true);
            resp.addProperty("count", count);
        } catch (Exception e) {
            resp.addProperty("status", false);
            resp.addProperty("count", 0);
        } finally {
            s.close();
        }
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(resp));
    }
}
