package controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import hibernate.Course;
import hibernate.Lesson;
import hibernate.User;
import hibernate.HibernateUtil;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@WebServlet(name = "LessonData", urlPatterns = {"/LessonData"})
public class LessonData extends HttpServlet {

    private void writeJson(HttpServletResponse response, JsonObject resp, Gson gson) throws IOException {
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(resp));
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Gson gson = new Gson();
        JsonObject resp = new JsonObject();

        HttpSession ses = request.getSession(false);
        if (ses == null || ses.getAttribute("user") == null) {
            resp.addProperty("status", false);
            resp.addProperty("message", "Please log in to view lessons.");
            writeJson(response, resp, gson);
            return;
        }
        User user = (User) ses.getAttribute("user");

        String courseIdStr = request.getParameter("course_id");
        if (courseIdStr == null || courseIdStr.trim().isEmpty()) {
            resp.addProperty("status", false);
            resp.addProperty("message", "Missing course_id.");
            writeJson(response, resp, gson);
            return;
        }

        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            // --- Load course ---
            Criteria cCourse = s.createCriteria(Course.class);
            cCourse.add(Restrictions.eq("id", Integer.parseInt(courseIdStr)));
            Course course = (Course) cCourse.uniqueResult();

            if (course == null) {
                resp.addProperty("status", false);
                resp.addProperty("message", "Course not found.");
                writeJson(response, resp, gson);
                return;
            }

            // OPTIONAL ACCESS CHECK (enable when your purchase/enrollment flow is ready):
            // boolean allowed = isUserAllowedForCourse(s, user, course);
            // if (!allowed) {
            //     resp.addProperty("status", false);
            //     resp.addProperty("message", "You don't have access to this course.");
            //     writeJson(response, resp, gson);
            //     return;
            // }
            // --- Load lessons (by order_index then id) ---
            Criteria cLesson = s.createCriteria(Lesson.class);
            cLesson.add(Restrictions.eq("course", course));
            cLesson.addOrder(Order.asc("order_index"));
            cLesson.addOrder(Order.asc("id"));
            @SuppressWarnings("unchecked")
            List<Lesson> lessons = cLesson.list();

            JsonObject courseJson = new JsonObject();
            courseJson.addProperty("id", course.getId());
            courseJson.addProperty("title", course.getTitle());
            courseJson.addProperty("thumbnail", course.getThumbnail());

            if (course.getUser()!= null) {
                courseJson.addProperty("publisher_name", course.getUser().getFname() + " " + course.getUser().getLname());
            } else {
                courseJson.addProperty("publisher_name", "");
            }

            JsonArray lessonsArr = new JsonArray();
            for (Lesson l : lessons) {
                JsonObject j = new JsonObject();
                j.addProperty("id", l.getId());
                j.addProperty("title", l.getTitle());
                j.addProperty("video_url", safe(l.getVideo_url()));

                // resource_file is a TEXT field; allow comma-separated or single URL; filter empties
                List<String> files = splitResources(safe(l.getResource_file()));
                JsonArray rf = new JsonArray();
                for (String f : files) {
                    rf.add(f);
                }
                j.add("resource_files", rf);

                lessonsArr.add(j);
            }

            resp.addProperty("status", true);
            resp.add("course", courseJson);
            resp.add("lessons", lessonsArr);
            writeJson(response, resp, gson);

        } catch (Exception e) {
            e.printStackTrace();
            resp.addProperty("status", false);
            resp.addProperty("message", "Error: " + e.getMessage());
            writeJson(response, resp, gson);
        } finally {
            s.close();
        }
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private List<String> splitResources(String raw) {
        if (raw == null) {
            return new ArrayList<>();
        }
        String str = raw.trim();
        if (str.isEmpty()) {
            return new ArrayList<>();
        }
        // Support both single URL and comma/whitespace separated lists
        String[] parts = str.split("[,\\s]+");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    // Example access gate (plug your logic later)
    // private boolean isUserAllowedForCourse(Session s, User user, Course course) {
    //     // If free course
    //     if ("F".equalsIgnoreCase(course.getPricelevel())) return true;
    //     // TODO: check enrollment/payment/order_item for user+course
    //     return true; // temporarily allow while you build checkout
    // }
}
