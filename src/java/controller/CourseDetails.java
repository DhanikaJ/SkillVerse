package controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import hibernate.Course;
import hibernate.HibernateUtil;
import hibernate.Lesson;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

@WebServlet(name = "CourseDetails", urlPatterns = {"/CourseDetails"})
public class CourseDetails extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Gson gson = new Gson();
        JsonObject resp = new JsonObject();
        String courseIdStr = request.getParameter("id");
        if (courseIdStr == null) {
            resp.addProperty("status", false);
            resp.addProperty("message", "Missing course ID");
            response.getWriter().write(gson.toJson(resp));
            return;
        }

        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            int courseId = Integer.parseInt(courseIdStr);
            Criteria c = s.createCriteria(Course.class);
            c.add(Restrictions.eq("id", courseId));
            Course course = (Course) c.uniqueResult();

            if (course == null) {
                resp.addProperty("status", false);
                resp.addProperty("message", "Course not found");
            } else {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", course.getId());
                obj.addProperty("title", course.getTitle());
                obj.addProperty("img", course.getThumbnail());
                obj.addProperty("description", course.getDescription());
                obj.addProperty("category", course.getCategory() != null ? course.getCategory().getType() : "N/A");
                obj.addProperty("status", course.getStatus() != null ? course.getStatus().getType() : "Active");
                obj.addProperty("createdAt", course.getCreated_at() != null ? course.getCreated_at().toString() : "");
                obj.addProperty("instructor", (course.getUser() != null ? (course.getUser().getFname() + " " + course.getUser().getLname()) : "Unknown"));
                obj.addProperty("publisher", "Skillverse Team"); // or from your org logic
                obj.addProperty("price", course.getPrice());

                // Lessons (for What you'll learn)
                Criteria lCrit = s.createCriteria(Lesson.class);
                lCrit.add(Restrictions.eq("course", course));
                lCrit.addOrder(org.hibernate.criterion.Order.asc("order_index"));
                List<Lesson> lessonList = lCrit.list();
                JsonArray lessonsArr = new JsonArray();
                for (Lesson lesson : lessonList) {
                    if (lesson.getTitle() != null && !lesson.getTitle().isEmpty()) {
                        lessonsArr.add(lesson.getTitle());
                    }
                }
                obj.add("lessons", lessonsArr);

                resp.add("course", obj);
                resp.addProperty("status", true);
            }
        } catch (Exception e) {
            resp.addProperty("status", false);
            resp.addProperty("message", e.getMessage());
            e.printStackTrace();
        } finally {
            s.close();
        }
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(resp));
    }

}
