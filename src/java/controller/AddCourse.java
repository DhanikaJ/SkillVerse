package controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import hibernate.Category;
import hibernate.Course;
import hibernate.HibernateUtil;
import hibernate.Lesson;
import hibernate.Quiz;
import hibernate.QuizQuestion;
import hibernate.Status;
import hibernate.User;
import java.io.IOException;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.hibernate.Session;
import org.hibernate.Transaction;

@WebServlet(name = "AddCourse", urlPatterns = {"/AddCourse"})
@MultipartConfig
public class AddCourse extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Gson gson = new Gson();
        JsonObject resp = new JsonObject();
        HttpSession ses = request.getSession(false);
        if (ses == null || ses.getAttribute("user") == null) {
            resp.addProperty("status", false);
            resp.addProperty("message", "Please log in first.");
            writeJsonResponse(response, resp, gson);
            return;
        }

        User instructor = (User) ses.getAttribute("user");
        JsonObject req = gson.fromJson(request.getReader(), JsonObject.class);

        Session s = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;

        try {
            tx = s.beginTransaction();

            int categoryId = req.get("category").getAsInt();
            Category category = (Category) s.get(Category.class, categoryId);
            if (category == null) {
                throw new Exception("Invalid Category!");
            }

            // Save course
            Course course = new Course();
            course.setTitle(req.get("title").getAsString());
            course.setDescription(req.get("description").getAsString());
            course.setDifficulty(req.get("difficulty").getAsString());
            course.setPricelevel(req.get("priceType").getAsString());
            course.setPrice(req.get("priceLKR").getAsDouble());
            course.setCategory(category);
            course.setUser(instructor);
            course.setStatus((Status) s.get(Status.class, 1)); // Assume 'Active'
            course.setThumbnail(req.get("thumbnailUrl").getAsString());
            course.setCreated_at(new Date());
            s.save(course);
            
            //SAVE LESSON
            int orderIndex = 0;
            JsonArray lessonsArr = req.getAsJsonArray("lessons");
            for (JsonElement elem : lessonsArr) {
                JsonObject lObj = elem.getAsJsonObject();
                Lesson lesson = new Lesson();
                lesson.setCourse(course);
                lesson.setTitle(lObj.get("title").getAsString());
                lesson.setVideo_url(lObj.get("videoUrl").getAsString());
                lesson.setResource_file(lObj.get("resourceUrl").getAsString());
                lesson.setOrder_index(orderIndex);
                s.save(lesson);
                orderIndex++;
            }

            // Save quiz
            JsonObject quizJson = req.getAsJsonObject("quiz");
            Quiz quiz = new Quiz();
            quiz.setCourse(course);
            quiz.setTitle(quizJson.get("title").getAsString());
            s.save(quiz);

            for (JsonElement q : quizJson.get("questions").getAsJsonArray()) {
                JsonObject qObj = q.getAsJsonObject();
                QuizQuestion qq = new QuizQuestion();
                qq.setQuiz(quiz);
                qq.setQuestion_text(qObj.get("questionText").getAsString());
                qq.setOption_a(qObj.get("optionA").getAsString());
                qq.setOption_b(qObj.get("optionB").getAsString());
                qq.setOption_c(qObj.get("optionC").getAsString());
                qq.setOption_d(qObj.get("optionD").getAsString());
                qq.setCorrect_option(qObj.get("correctOption").getAsString());
                s.save(qq);
            }

            tx.commit();
            resp.addProperty("status", true);
            resp.addProperty("message", "Course created successfully!");
        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            resp.addProperty("status", false);
            resp.addProperty("message", "Failed to create course: " + e.getMessage());
            e.printStackTrace();
        } finally {
            s.close();
        }

        writeJsonResponse(response, resp, gson);
    }

    private void writeJsonResponse(HttpServletResponse response, JsonObject obj, Gson gson) throws IOException {
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(obj));
    }

}
