package controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import hibernate.Course;
import hibernate.HibernateUtil;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.Criteria;
import org.hibernate.Session;

@WebServlet(name = "CourseList", urlPatterns = {"/CourseList"})
public class CourseList extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Gson gson = new Gson();
        JsonObject resp = new JsonObject();
        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            Criteria c = s.createCriteria(Course.class);
            // Optionally: c.setMaxResults(8); // Limit to 8 for popular
            List<Course> courseList = c.list();

            JsonArray arr = new JsonArray();
            for (Course course : courseList) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", course.getId());
                obj.addProperty("title", course.getTitle());
                obj.addProperty("img", course.getThumbnail()); // Cloudinary URL
                obj.addProperty("description", course.getDescription());
                obj.addProperty("categoryId", course.getCategory() != null ? course.getCategory().getId() : 0);
                obj.addProperty("difficulty", course.getDifficulty()); // Should be 'B','I','A' or map to text
                obj.addProperty("pricelevel", course.getPricelevel()); // Should be 'F','P'

                // Instructor name
                String instructor = (course.getUser() != null)
                        ? course.getUser().getFname() + " " + course.getUser().getLname()
                        : "Unknown";
                obj.addProperty("instructor", instructor);

                // University or org (if you have a field, else hardcode or remove)
                obj.addProperty("university", course.getCategory() != null ? course.getCategory().getType() : "Unknown");

                // Demo: You can calculate average rating if you have reviews
                obj.addProperty("rating", 4.5); // TODO: Replace with actual rating if available

                arr.add(obj);
            }
            resp.add("courses", arr);
            resp.addProperty("status", true);
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
