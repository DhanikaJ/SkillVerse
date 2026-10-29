package controller;

import com.google.gson.Gson;
import hibernate.Gender;
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

@WebServlet(name = "GenderData", urlPatterns = {"/GenderData"})
public class GenderData extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Session s = HibernateUtil.getSessionFactory().openSession();

        Criteria c = s.createCriteria(Gender.class);
        List<Gender> genderList = c.list();

        Gson gson = new Gson();
        String toJson = gson.toJson(genderList);
        response.setContentType("application/json");
        response.getWriter().write(toJson);
        s.close();
    }

}
