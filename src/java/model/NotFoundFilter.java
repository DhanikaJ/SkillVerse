package model;

import java.io.IOException;
import java.util.Set;
import java.util.HashSet;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@WebFilter("/*") // Apply to all URLs
public class NotFoundFilter implements Filter {
    
    private Set<String> validPaths;
    private Set<String> validExtensions;
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialize known valid paths (add your actual pages/servlets here)
        validPaths = new HashSet<>();
        
        // Add your HTML pages
        validPaths.add("/home.html");
        validPaths.add("/my-learning.html");
        validPaths.add("/online-degrees.html");
        validPaths.add("/my-courses.html");
        validPaths.add("/instructor.html");
        validPaths.add("/profile.html");
        validPaths.add("/admin-reports.html");
        validPaths.add("/404.html");
        validPaths.add("/index.html");
        validPaths.add("/navbar.html");
        validPaths.add("/lesson.html");
        validPaths.add("/course-view.html");
        validPaths.add("/cart.html");
        validPaths.add("/checkout.html");
        validPaths.add("/footer.html");
        validPaths.add("/cart-drawer.html");
        validPaths.add("/cancel.html");
        validPaths.add("/navbar-normal.html");
        validPaths.add("/publicCourses.html");
        validPaths.add("/purchases.html");
        validPaths.add("/success.html");
        
        // Add your servlet mappings (without servlet prefix)
        validPaths.add("/AdminReports");
        validPaths.add("/SignIn");
        validPaths.add("/SignUp");
        validPaths.add("/SignOut");
        validPaths.add("/AddCourse");
        validPaths.add("/CourseDetails");
        validPaths.add("/CategoryData");
        validPaths.add("/CourseList");
        validPaths.add("/RemoveCartItemServlet");
        validPaths.add("/CheckOut");
        validPaths.add("/CheckoutUserCart");
        validPaths.add("/UserProfile");
        validPaths.add("/UpdateCloud");
        validPaths.add("/VerifyPayments");
        validPaths.add("/CartCountServlet");
        validPaths.add("/CartServlet");
        validPaths.add("/CityData");
        validPaths.add("/DownloadResource");
        validPaths.add("/GenderData");
        validPaths.add("/GetLatestOrder");
        validPaths.add("/InstructorProfile");
        validPaths.add("/LessonData");
        validPaths.add("/MyCourseList");
        validPaths.add("/MyLearning");
        validPaths.add("/MyPurchases");
        
        // Add root path
        validPaths.add("/");
        validPaths.add("");
        
        // Initialize valid file extensions
        validExtensions = new HashSet<>();
        validExtensions.add(".css");
        validExtensions.add(".js");
        validExtensions.add(".jpg");
        validExtensions.add(".jpeg");
        validExtensions.add(".png");
        validExtensions.add(".gif");
        validExtensions.add(".svg");
        validExtensions.add(".ico");
        validExtensions.add(".woff");
        validExtensions.add(".woff2");
        validExtensions.add(".ttf");
        validExtensions.add(".eot");
        validExtensions.add(".pdf");
        validExtensions.add(".mp4");
        validExtensions.add(".webm");
        validExtensions.add(".ogg");
        validExtensions.add(".mp3");
        validExtensions.add(".wav");
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestURI = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        
        // Remove context path to get relative path
        String relativePath = requestURI;
        if (contextPath != null && !contextPath.isEmpty()) {
            relativePath = requestURI.substring(contextPath.length());
        }
        
        // Check if this is a valid path or file
        if (isValidPath(relativePath)) {
            // Continue with the request
            chain.doFilter(request, response);
        } else {
            // Check if the resource actually exists after filter chain
            // This handles dynamic content and server-side includes
            try {
                // Try to process the request first
                chain.doFilter(request, response);
                
                // If we get here and the response status is 404, redirect to custom page
                if (httpResponse.getStatus() == HttpServletResponse.SC_NOT_FOUND) {
                    redirectToCustom404(httpRequest, httpResponse);
                }
            } catch (Exception e) {
                // If any exception occurs, redirect to 404
                redirectToCustom404(httpRequest, httpResponse);
            }
        }
    }
    
    /**
     * Check if the requested path is valid
     */
    private boolean isValidPath(String path) {
        // Normalize path
        if (path == null) {
            return false;
        }
        
        // Remove query parameters
        if (path.contains("?")) {
            path = path.substring(0, path.indexOf("?"));
        }
        
        // Check exact matches
        if (validPaths.contains(path)) {
            return true;
        }
        
        // Check if it's a static resource (assets folder, etc.)
        if (path.startsWith("/assets/") || 
            path.startsWith("/css/") || 
            path.startsWith("/js/") || 
            path.startsWith("/images/") ||
            path.startsWith("/uploads/") ||
            path.startsWith("/resources/") ||
            path.startsWith("/static/")) {
            return true;
        }
        
        // Check file extensions
        for (String extension : validExtensions) {
            if (path.toLowerCase().endsWith(extension)) {
                return true;
            }
        }
        
        // Check if it matches servlet patterns (you may need to adjust these)
        if (path.matches("^/[A-Za-z]+$")) { // Simple servlet name pattern
            return true;
        }
        
        return false;
    }
    
    /**
     * Redirect to custom 404 page
     */
    private void redirectToCustom404(HttpServletRequest request, HttpServletResponse response) throws IOException {
        
        // Set status to 404
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        
        // Get the context path for proper redirection
        String contextPath = request.getContextPath();
        String redirectURL = contextPath + "/404.html";
        
        // Forward to 404.html instead of redirect to maintain URL
        try {
            request.getRequestDispatcher("/404.html").forward(request, response);
        } catch (ServletException | IOException e) {
            // Fallback to redirect if forward fails
            response.sendRedirect(redirectURL);
        }
    }
    
    @Override
    public void destroy() {
        // Cleanup resources if needed
        if (validPaths != null) {
            validPaths.clear();
        }
        if (validExtensions != null) {
            validExtensions.clear();
        }
    }
}