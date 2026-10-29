package model;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebFilter(urlPatterns = {"/home.html", "/instructor.html", "/my-courses.html", "/online-degrees.html", "/cart.html", "/checkout.html", "/lesson.html", "/my-learning.html", "/profile.html", "/course-view.html"})
public class SignInCheckFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        HttpSession ses = request.getSession(false);

        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
        response.setHeader("Pragma", "no-cache");                                   // HTTP 1.0
        response.setHeader("Expires", "0");

        if (ses != null && ses.getAttribute("user") != null) {
            chain.doFilter(req, res);
        } else {
            response.sendRedirect("index.html");
        }
    }

    @Override
    public void destroy() {

    }

}
