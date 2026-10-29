package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import hibernate.HibernateUtil;
import hibernate.Status;
import hibernate.User;
import java.io.IOException;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import model.Mail;
import model.Util;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;

@WebServlet(name = "SignUp", urlPatterns = {"/SignUp"})
public class SignUp extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Gson gson = new Gson();
        JsonObject user = gson.fromJson(request.getReader(), JsonObject.class);

        final String email = user.get("email").getAsString();
        String firstName = user.get("firstName").getAsString();
        String lastName = user.get("lastName").getAsString();
        String password = user.get("password").getAsString();

        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);

        if (email.isEmpty()) {
            responseObject.addProperty("message", "Email can not be empty!");
        } else if (!Util.isemailValid(email)) {
            responseObject.addProperty("message", "Please enter a valid email!");
        } else if (firstName.isEmpty()) {
            responseObject.addProperty("message", "Name can not be empty!");
        } else if (!Util.ispwValid(password)) {
            responseObject.addProperty("message", "Password must contain at least one uppercase letter, "
                    + "one lowercase letter, "
                    + "one number, one special character, "
                    + "and be at least eight characters long.");
        } else {

            //hibernate save
            SessionFactory sf = HibernateUtil.getSessionFactory();
            Session s = sf.openSession();

            Criteria criteria = s.createCriteria(User.class);
            criteria.add(Restrictions.eq("email", email));

            if (!criteria.list().isEmpty()) {
                responseObject.addProperty("message", "User with this email already exists!");
            } else {
                User u = new User();
                u.setEmail(email);
                u.setFname(firstName);
                u.setLname(lastName);
                u.setPassword_hash(password);

                //generate verification code
                final String verificationcode = Util.generateCode();
                u.setVerification(verificationcode);
                //generate verification code

                Status activeStatus = (Status) s.get(Status.class, 1);
                
                u.setStatus_id(activeStatus);
                u.setCreated_at(new Date());

                s.save(u);
                s.beginTransaction().commit();
                //hibernate save

                //send email
                final String htmlMessage
                        = "<!DOCTYPE html>"
                        + "<html lang='en'>"
                        + "<head>"
                        + "<meta charset='UTF-8'>"
                        + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                        + "<title>SkillVerse Email Verification</title>"
                        + "</head>"
                        + "<body style='background-color:#f6f6f6; margin:0; padding:0; font-family: Arial, sans-serif;'>"
                        + "<table align='center' cellpadding='0' cellspacing='0' width='100%' style='max-width:500px; background:#fff; border-radius:8px; box-shadow:0 2px 6px rgba(0,0,0,0.08);'>"
                        + "<tr>"
                        + "<td style='background:#006bff; padding:24px 0; border-top-left-radius:8px; border-top-right-radius:8px;'>"
                        + "<img src='assets/images/logo/whiteplanet.png'/> "
                        + "<h2 style='color:#fff; margin:0; text-align:center;'>SkillVerse</h2>"
                        + "</td>"
                        + "</tr>"
                        + "<tr>"
                        + "<td style='padding:32px 24px 16px 24px;'>"
                        + "<h3 style='color:#333; margin-top:0;'>Verify Your Email Address</h3>"
                        + "<p style='color:#555; font-size:16px;'>Hi there,</p>"
                        + "<p style='color:#555; font-size:16px;'>Thank you for signing up with <b>SkillVerse</b>! To complete your registration, please enter the following verification code in the app:</p>"
                        + "<div style='margin:24px 0; text-align:center;'>"
                        + "<span style='display:inline-block; padding:18px 36px; font-size:32px; background:#eaf4ff; color:#006bff; border-radius:6px; letter-spacing:6px; font-weight:bold;'>" + verificationcode + "</span>"
                        + "</div>"
                        + "<p style='color:#555; font-size:15px;'>This code is valid for 15 minutes. If you did not create an account, you can safely ignore this email.</p>"
                        + "<p style='color:#888; font-size:13px; margin-top:32px;'>Best regards,<br>SkillVerse Team</p>"
                        + "</td>"
                        + "</tr>"
                        + "<tr>"
                        + "<td style='padding:16px 24px; background:#f3f7fa; border-bottom-left-radius:8px; border-bottom-right-radius:8px; color:#aaa; text-align:center; font-size:13px;'>© " + java.time.Year.now() + " SkillVerse. All rights reserved.</td>"
                        + "</tr>"
                        + "</table>"
                        + "</body>"
                        + "</html>";
                        
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Mail.sendMail(email, "SkillVerse | Email Verification", htmlMessage);
                    }
                }).start();
                //send email

                //session management
                HttpSession ses = request.getSession();
                ses.setAttribute("email", email);
                //session management

                responseObject.addProperty("status", true);
                responseObject.addProperty("message", "Registration successful! Please check your email for the verification code.");
            }
            s.close();
        }

        String responseText = gson.toJson(responseObject);
        response.setContentType("application/json");
        response.getWriter().write(responseText);

    }
}
