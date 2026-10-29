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
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

@WebServlet(name = "VerifyAccount", urlPatterns = {"/VerifyAccount"})
public class VerifyAccount extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Gson gson = new Gson();

        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);

        HttpSession ses = request.getSession();

        if (ses.getAttribute("email") == null) {
            responseObject.addProperty("message", "1"); //1=Email not found!
        } else {

            String email = ses.getAttribute("email").toString();

            JsonObject verification = gson.fromJson(request.getReader(), JsonObject.class);

            String verificationCode = verification.get("verificationCode").getAsString();

            SessionFactory sf = HibernateUtil.getSessionFactory();
            Session s = sf.openSession();

            Criteria c1 = s.createCriteria(User.class);

            Criterion cr1 = Restrictions.eq("email", email);
            Criterion cr2 = Restrictions.eq("verification", verificationCode);

            c1.add(cr1);
            c1.add(cr2);

            if (c1.list().isEmpty()) {
                responseObject.addProperty("message", "Invalid Verification Code!");
            } else {
                User user = (User) c1.list().get(0);
                user.setVerification("Verified");

                s.update(user);
                s.beginTransaction().commit();
                s.close();

                responseObject.addProperty("status", true);
                responseObject.addProperty("message", "Verification Successful!");
            }
        }
        String responseText = gson.toJson(responseObject);
        response.setContentType("application/json");
        response.getWriter().write(responseText);
    }

}
