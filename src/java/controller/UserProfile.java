package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import hibernate.City;
import hibernate.Gender;
import hibernate.HibernateUtil;
import hibernate.User;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import model.Util;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.slf4j.LoggerFactory;

@WebServlet(name = "UserProfile", urlPatterns = {"/UserProfile"})
public class UserProfile extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession ses = request.getSession(false);

        if (ses != null && ses.getAttribute("user") != null) {
            User user = (User) ses.getAttribute("user");

            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("firstName", user.getFname());
            responseObject.addProperty("lastName", user.getLname());
            String name = user.getFname() + (user.getLname().isEmpty() ? "" : " " + user.getLname());
            responseObject.addProperty("name", name);
            responseObject.addProperty("currentpassword", user.getPassword_hash());
            responseObject.addProperty("photo", user.getPhoto() != null ? user.getPhoto() : "");
            responseObject.addProperty("cityId", user.getCity() != null ? user.getCity().getId() : 0);
            responseObject.addProperty("genderId", user.getGender() != null ? user.getGender().getId() : 0);

            Gson gson = new Gson();

            String toJson = gson.toJson(responseObject);
            response.setContentType("application/json");
            response.getWriter().write(toJson);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Gson gson = new Gson();
        JsonObject userData = gson.fromJson(request.getReader(), JsonObject.class);

        String name = userData.get("name").getAsString().trim();
        int cityId = userData.get("citySelect").getAsInt();
        int genderId = userData.get("genderSelect").getAsInt();
        String currentPassword = userData.get("currentpassword").getAsString();
        String newPassword = userData.get("newpassword").getAsString();
        String confirmPassword = userData.get("confirmpassword").getAsString();
        String photo = userData.has("photo") ? userData.get("photo").getAsString() : "";

        String firstName = "";
        String lastName = "";

        if (name.contains(" ")) {
            String[] parts = name.split("\\s+");
            firstName = parts[0];
            lastName = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
        } else {
            firstName = name;
            lastName = "";
        }

        // Now set these on the User entity:
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);

        if (name.isEmpty()) {
            responseObject.addProperty("message", "Your Name cannot be empty");
        } else if (cityId == 0) {
            responseObject.addProperty("message", "Select your City");
        } else if (genderId == 0) {
            responseObject.addProperty("message", "Select your Gender");
        } else if (currentPassword.isEmpty()) {
            responseObject.addProperty("message", "Enter your current password");
        } else if (!newPassword.isEmpty() && !Util.ispwValid(newPassword)) {
            responseObject.addProperty(
                    "message",
                    "The password must contains at least uppercase, lowercase,"
                    + " number, special character and to be minimum eight characters long!"
            );
        } else if (!newPassword.isEmpty() && newPassword.equals(currentPassword)) {
            responseObject.addProperty(
                    "message",
                    "The new password cannot match the current password"
            );
        } else if (!confirmPassword.isEmpty() && !Util.ispwValid(confirmPassword)) {
            responseObject.addProperty(
                    "message",
                    "The password must contains at least uppercase, lowercase,"
                    + " number, special character and to be minimum eight characters long!"
            );
        } else if (!confirmPassword.equals(newPassword)) {
            responseObject.addProperty(
                    "message",
                    "Confirmed password does not matching entered new password!"
            );
        } else {

            HttpSession ses = request.getSession();
            if (ses.getAttribute("user") != null) {
                User u = (User) ses.getAttribute("user");

                Session s = HibernateUtil.getSessionFactory().openSession();
                Transaction tx = null;
                try {
                    tx = s.beginTransaction();
                    City city = (City) s.get(City.class, cityId);
                    Gender gender = (Gender) s.get(Gender.class, genderId);
                    if (city == null) {
                        responseObject.addProperty("message", "Selected city does not exist!");
                        writeJsonResponse(response, responseObject, gson);
                        return;
                    }

                    if (city == null) {
                        responseObject.addProperty("message", "Selected city does not exist!");
                        writeJsonResponse(response, responseObject, gson);
                        return;
                    }

                    Criteria c = s.createCriteria(User.class);
                    c.add(Restrictions.eq("email", u.getEmail()));

                    if (!c.list().isEmpty()) {
                        User u1 = (User) c.list().get(0);

                        u1.setFname(firstName);
                        u1.setLname(lastName);
                        u1.setGender(gender);
                        u1.setCity(city);
                        if (!confirmPassword.isEmpty()) {
                            u1.setPassword_hash(confirmPassword);
                        } else {
                            u1.setPassword_hash(currentPassword);
                        }
                        if (!photo.isEmpty()) {
                            u1.setPhoto(photo);
                        }
                        if (u1.getCreated_at() == null) {
                            u1.setCreated_at(new Date());
                        }


                        ses.setAttribute("user", u1);

                        s.merge(u1);

                        tx.commit();
                        responseObject.addProperty("status", true);
                        responseObject.addProperty("message", "User profile details updated successfully!");
                    }
                } catch (Exception e) {
                    if (tx != null) {
                        tx.rollback();
                    }
                    e.printStackTrace();
                    responseObject.addProperty("message", "Server error: " + e.getMessage());
                } finally {
                    s.close();
                }
                writeJsonResponse(response, responseObject, gson);
            }
        }
    }

    private void writeJsonResponse(HttpServletResponse response, JsonObject obj, Gson gson) throws IOException {
        String toJson = gson.toJson(obj);
        response.setContentType("application/json");
        response.getWriter().write(toJson);
    }
}
