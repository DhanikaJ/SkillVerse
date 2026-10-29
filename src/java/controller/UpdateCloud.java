package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import hibernate.HibernateUtil;
import hibernate.User;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

@WebServlet(name = "UpdateCloud", urlPatterns = {"/UpdateCloud"})
public class UpdateCloud extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        JsonObject resp = new JsonObject();
        resp.addProperty("status", false);

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            resp.addProperty("message", "Not signed in.");
        } else {
            Integer userId = (Integer) session.getAttribute("userId");
            Gson gson = new Gson();
            JsonObject reqBody = gson.fromJson(request.getReader(), JsonObject.class);

            String photo = reqBody.get("photo").getAsString();

            Session s = HibernateUtil.getSessionFactory().openSession();
            try {
                s.beginTransaction();
                User user = (User) s.createCriteria(User.class)
                        .add(Restrictions.eq("id", userId))
                        .uniqueResult();

                if (user != null) {
                    user.setPhoto(photo);
                    s.update(user);
                    s.getTransaction().commit();
                    resp.addProperty("status", true);
                    resp.addProperty("message", "Profile photo updated.");
                } else {
                    resp.addProperty("message", "User not found.");
                }
            } catch (Exception e) {
                if (s.getTransaction().isActive()) {
                    s.getTransaction().rollback();
                }
                resp.addProperty("message", "Failed to update profile photo.");
            } finally {
                s.close();
            }
        }
        response.setContentType("application/json");
        response.getWriter().write(new Gson().toJson(resp));
    }
}
