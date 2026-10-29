package controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import hibernate.User;
import java.io.IOException;
import java.text.SimpleDateFormat;
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

@WebServlet(name = "MyPurchases", urlPatterns = {"/MyPurchases"})
public class MyPurchases extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Gson gson = new Gson();
        JsonObject resp = new JsonObject();

        HttpSession ses = request.getSession(false);
        if (ses == null || ses.getAttribute("user") == null) {
            resp.addProperty("status", false);
            resp.addProperty("message", "Not logged in");
            writeJsonResponse(response, resp, gson);
            return;
        }
        User user = (User) ses.getAttribute("user");
        System.out.println("[MyPurchases] UserID: " + user.getId());

        Session s = hibernate.HibernateUtil.getSessionFactory().openSession();
        try {
            // Query Payment table for this user, only successful payments (status_id=8 = Success)
            Criteria paymentCrit = s.createCriteria(hibernate.Payment.class);
            paymentCrit.add(Restrictions.eq("user", user));
            paymentCrit.add(Restrictions.eq("status.id", 8)); // 8 = Success

            @SuppressWarnings("unchecked")
            List<hibernate.Payment> payments = paymentCrit.list();

            JsonArray arr = new JsonArray();
            for (hibernate.Payment pay : payments) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

                hibernate.Course course = pay.getCourse();

                // Instructor name (get User from course)
                hibernate.User instructor = course.getUser();
                String instructorName = instructor.getFname() + " " + instructor.getLname();

                // Difficulty mapping (I = Intermediate, B = Beginner, A = Advanced)
                String difficultyStr = course.getDifficulty();
                char diffChar = (difficultyStr != null && !difficultyStr.isEmpty()) ? difficultyStr.charAt(0) : ' ';
                String diff = mapDifficulty(diffChar);

                // Certificate (if exists)
                //String certUrl = getCertificateUrl(s, user.getId(), course.getId());
                JsonObject obj = new JsonObject();
                obj.addProperty("course_id", course.getId());
                obj.addProperty("title", course.getTitle());
                obj.addProperty("instructor", instructorName);
                obj.addProperty("difficulty", diff);
                obj.addProperty("thumbnail", course.getThumbnail());
                obj.addProperty("purchased_at", sdf.format(pay.getPaid_at()));
                obj.addProperty("price", pay.getAmount());
                //if (certUrl != null) obj.addProperty("certificate_url", certUrl);

                arr.add(obj);
            }

            resp.addProperty("status", true);
            resp.add("purchases", arr);
        } catch (Exception e) {
            e.printStackTrace();
            resp.addProperty("status", false);
            resp.addProperty("message", "Error: " + e.getMessage());
        } finally {
            s.close();
        }
        writeJsonResponse(response, resp, gson);
    }

    private static String mapDifficulty(char d) {
        switch (Character.toUpperCase(d)) {
            case 'I':
                return "Intermediate";
            case 'A':
                return "Advanced";
            case 'B':
                return "Beginner";
            default:
                return "Unknown";
        }
    }

    // Helper: get certificate path for (user, course)
//    private static String getCertificateUrl(Session s, int userId, int courseId) {
//        Criteria certCrit = s.createCriteria(hibernate.Certificate.class);
//        certCrit.add(Restrictions.eq("user.id", userId));
//        certCrit.add(Restrictions.eq("course.id", courseId));
//        hibernate.Certificate cert = (hibernate.Certificate) certCrit.uniqueResult();
//        if (cert != null) return cert.getCertificatePath();
//        return null;
//    }
    // Helper: Write JSON response (as per your pattern)
    private void writeJsonResponse(HttpServletResponse response, JsonObject resp, Gson gson) throws IOException {
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(resp));
    }
}
