package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import hibernate.HibernateUtil;
import hibernate.User;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import model.Util;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

@WebServlet(name = "SignIn", urlPatterns = {"/SignIn"})
public class SignIn extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Gson gson = new Gson();
        JsonObject signIn = gson.fromJson(request.getReader(), JsonObject.class);

        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);

        String email = signIn.get("email").getAsString();
        String password = signIn.get("password").getAsString();
        boolean rememberMe = signIn.has("remember") && signIn.get("remember").getAsBoolean();

        if (email.isEmpty()) {
            responseObject.addProperty("message", "Email cannot be empty!");
        } else if (!Util.isemailValid(email)) {
            responseObject.addProperty("message", "Please enter a valid email!");
        } else if (password.isEmpty()) {
            responseObject.addProperty("message", "password cannot be empty");
        } else if (!Util.ispwValid(password)) {
            responseObject.addProperty("message", "Password must contain at least one uppercase letter, "
                    + "one lowercase letter, "
                    + "one number, one special character, "
                    + "and be at least eight characters long.");
        } else {
            SessionFactory sf = HibernateUtil.getSessionFactory();
            Session s = sf.openSession();

            Criteria c = s.createCriteria(User.class);

            Criterion cr1 = Restrictions.eq("email", email);
            Criterion cr2 = Restrictions.eq("password_hash", password);

            c.add(cr1);
            c.add(cr2);

            if (c.list().isEmpty()) {
                responseObject.addProperty("message", "Invalid Credentials");
            } else {
                User u = (User) c.list().get(0);
                HttpSession ses = request.getSession();
                ses.setAttribute("email", u.getEmail());

                // ✅ Remember Me - Set cookies (30 days)
                if (rememberMe) {
                    Cookie emailCookie = new Cookie("remembered_email", email);
                    emailCookie.setMaxAge(60 * 60 * 24 * 30);
                    emailCookie.setPath("/");
                    response.addCookie(emailCookie);

                    Cookie pwCookie = new Cookie("remembered_password", password);
                    pwCookie.setMaxAge(60 * 60 * 24 * 30);
                    pwCookie.setPath("/");
                    response.addCookie(pwCookie);
                } else {
                    // Clear cookies
                    Cookie emailCookie = new Cookie("remembered_email", "");
                    emailCookie.setMaxAge(0);
                    emailCookie.setPath("/");
                    response.addCookie(emailCookie);

                    Cookie pwCookie = new Cookie("remembered_password", "");
                    pwCookie.setMaxAge(0);
                    pwCookie.setPath("/");
                    response.addCookie(pwCookie);
                }
                
                responseObject.addProperty("status", true);

                if (!u.getVerification().equals("Verified")) {  //not verified
                    ses.setAttribute("email", email);
                    responseObject.addProperty("message", "1");
                } else { //verified
                    ses.setAttribute("user", u);
                    responseObject.addProperty("message", "2");
                }
            }
            s.close();
        }
        String responseText = gson.toJson(responseObject);
        response.setContentType("application/json");
        response.getWriter().write(responseText);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        JsonObject responseObject = new JsonObject();

        if (request.getSession().getAttribute("user") != null) {
//            String email = request.getSession().getAttribute("email").toString();
//            SessionFactory sf = HibernateUtil.getSessionFactory();
//            Session s = sf.openSession();
//            Criteria c = s.createCriteria(User.class);
//            c.add(Restrictions.eq("email", email));
//            
//            if(!c.list().isEmpty()){
//             
//            }
//            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "1");
        } else {
//            responseObject.addProperty("status", false);
            responseObject.addProperty("message", "2");
        }

        Gson gson = new Gson();
        String toJson = gson.toJson(responseObject);
        response.setContentType("application/json");
        response.getWriter().write(toJson);
    }

}
