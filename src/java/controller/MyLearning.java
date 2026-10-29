package controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import hibernate.Course;
import hibernate.Enrollment;
import hibernate.HibernateUtil;
import hibernate.Status;
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
import org.hibernate.sql.JoinType;

@WebServlet(name = "MyLearning", urlPatterns = {"/MyLearning"})
public class MyLearning extends HttpServlet {

    private final Gson gson = new Gson();
    private static final SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        JsonObject resp = new JsonObject();

        // 1) Session check
        HttpSession ses = request.getSession(false);
        if (ses == null || ses.getAttribute("user") == null) {
            resp.addProperty("status", false);
            resp.addProperty("code", 401);
            resp.addProperty("message", "Not signed in.");
            response.getWriter().write(gson.toJson(resp));
            return;
        }
        User currentUser = (User) ses.getAttribute("user");
        System.out.println("[MyLearningData] GET by userId=" + currentUser.getId());

        Session s = null;
        try {
            // 2) Read-only fetch via Criteria (no tx required for read)
            s = HibernateUtil.getSessionFactory().openSession();

            Criteria c = s.createCriteria(Enrollment.class, "en")
                    .createAlias("en.course", "c", JoinType.INNER_JOIN)
                    .createAlias("en.status", "st", JoinType.INNER_JOIN)
                    .createAlias("c.user", "inst", JoinType.LEFT_OUTER_JOIN)
                    .add(Restrictions.eq("en.user.id", currentUser.getId()));

            @SuppressWarnings("unchecked")
            List<Enrollment> list = c.list();

            JsonArray inProgress = new JsonArray();
            JsonArray completed = new JsonArray();

            for (Enrollment en : list) {
                Course course = en.getCourse();
                Status st = en.getStatus();
                User instructor = course.getUser();

                JsonObject item = new JsonObject();
                item.addProperty("enrollment_id", en.getId());
                item.addProperty("course_id", course.getId());
                item.addProperty("title", course.getTitle());
                item.addProperty("instructor", instructor != null
                        ? (safe(instructor.getFname()) + " " + safe(instructor.getLname())).trim()
                        : "Instructor");
                item.addProperty("thumbnail", safe(course.getThumbnail()));
                item.addProperty("progress", clampPercent(en.getProgress()));
                item.addProperty("status", st != null ? safe(st.getType()) : "In-progress");
                item.addProperty("enrolled_at", DF.format(en.getEnrolled_at()));

                boolean markCompleted = "Completed".equalsIgnoreCase(item.get("status").getAsString())
                        || item.get("progress").getAsInt() >= 100;

                if (markCompleted) {
                    completed.add(item);
                } else {
                    inProgress.add(item);
                }
            }

            resp.addProperty("status", true);
            resp.add("in_progress", inProgress);
            resp.add("completed", completed);

        } catch (Exception e) {
            e.printStackTrace();
            resp.addProperty("status", false);
            resp.addProperty("message", "Error: " + e.getMessage());
        } finally {
            if (s != null) {
                s.close();
            }
        }

        response.getWriter().write(gson.toJson(resp));
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }

    // progress in DB is DOUBLE; we clamp to 0..100 and round to int for the bar
    private static int clampPercent(double d) {
        if (Double.isNaN(d)) {
            return 0;
        }
        if (d < 0) {
            d = 0;
        }
        if (d > 100) {
            d = 100;
        }
        return (int) Math.round(d);
    }
}
