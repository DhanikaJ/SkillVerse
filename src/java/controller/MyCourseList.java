package controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import hibernate.Course;
import hibernate.HibernateUtil;
import hibernate.User;
import java.io.IOException;
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

@WebServlet(name = "MyCourseList", urlPatterns = {"/MyCourseList"})
public class MyCourseList extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Gson gson = new Gson();
        JsonObject resp = new JsonObject();

        // Check if user is logged in
        HttpSession httpSession = request.getSession(false);
        if (httpSession == null || httpSession.getAttribute("user") == null) {
            resp.addProperty("status", false);
            resp.addProperty("message", "User not logged in");
            response.setContentType("application/json");
            response.getWriter().write(gson.toJson(resp));
            return;
        }

        // Get logged-in user
        User loggedInUser = (User) httpSession.getAttribute("user");

        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            // Create criteria to get courses only for the logged-in user
            Criteria c = s.createCriteria(Course.class);
            c.add(Restrictions.eq("user.id", loggedInUser.getId()));

            // Optional: Order by creation date (newest first)
            // c.addOrder(Order.desc("createdAt"));
            List<Course> courseList = c.list();

            JsonArray arr = new JsonArray();
            for (Course course : courseList) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", course.getId());
                obj.addProperty("title", course.getTitle());
                obj.addProperty("img", course.getThumbnail()); // Cloudinary URL
                obj.addProperty("description", course.getDescription());
                obj.addProperty("categoryId", course.getCategory() != null ? course.getCategory().getId() : 0);
                obj.addProperty("categoryName", course.getCategory() != null ? course.getCategory().getType() : "Unknown");
                obj.addProperty("difficulty", course.getDifficulty()); // Should be 'B','I','A'
                obj.addProperty("pricelevel", course.getPricelevel()); // Should be 'F','P'
                obj.addProperty("price", course.getPrice());
                obj.addProperty("statusId", course.getStatus() != null ? course.getStatus().getId() : 0);
                obj.addProperty("statusName", course.getStatus() != null ? course.getStatus().getType() : "Unknown");
                obj.addProperty("createdAt", course.getCreated_at() != null ? course.getCreated_at().toString() : "");

                // Instructor name (should be the logged-in user)
                String instructor = (course.getUser() != null)
                        ? course.getUser().getFname() + " " + course.getUser().getLname()
                        : "Unknown";
                obj.addProperty("instructor", instructor);
                obj.addProperty("university", course.getCategory() != null ? course.getCategory().getType() : "Unknown");

                // You can calculate actual statistics here if needed
                obj.addProperty("rating", 4.5); // TODO: Calculate actual average rating
                obj.addProperty("enrollmentCount", 0); // TODO: Count actual enrollments
                obj.addProperty("lessonCount", 0); // TODO: Count actual lessons

                arr.add(obj);
            }

            resp.add("courses", arr);
            resp.addProperty("status", true);
            resp.addProperty("totalCourses", courseList.size());
            resp.addProperty("message", "Courses loaded successfully");

        } catch (Exception e) {
            resp.addProperty("status", false);
            resp.addProperty("message", "Error loading courses: " + e.getMessage());
            e.printStackTrace();
        } finally {
            s.close();
        }

        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(resp));
    }
}
