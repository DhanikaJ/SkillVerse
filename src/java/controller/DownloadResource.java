package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import hibernate.HibernateUtil;
import hibernate.Lesson;
import hibernate.User;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

@WebServlet(name = "DownloadResource", urlPatterns = {"/DownloadResource"})
public class DownloadResource extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Gson gson = new Gson();
        JsonObject resp = new JsonObject();
        response.setCharacterEncoding("UTF-8");

        HttpSession ses = request.getSession(false);
        if (ses == null || ses.getAttribute("user") == null) {
            System.out.println("[DownloadResource] No session or user.");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.addProperty("status", false);
            resp.addProperty("message", "Unauthorized");
            writeJson(response, gson, resp);
            return;
        }
        User user = (User) ses.getAttribute("user");

        String lessonIdStr = request.getParameter("lesson_id");
        String idxStr = request.getParameter("idx");
        int idx = 0;

        if (lessonIdStr == null || lessonIdStr.trim().isEmpty()) {
            resp.addProperty("status", false);
            resp.addProperty("message", "Missing lesson_id");
            writeJson(response, gson, resp);
            return;
        }
        try {
            if (idxStr != null && !idxStr.isEmpty()) {
                idx = Integer.parseInt(idxStr);
            }
        } catch (NumberFormatException e) {
            idx = 0;
        }

        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            System.out.println("[DownloadResource] user=" + user.getId() + ", lesson_id=" + lessonIdStr + ", idx=" + idx);

            Criteria c = s.createCriteria(Lesson.class);
            c.add(Restrictions.eq("id", Integer.parseInt(lessonIdStr)));
            Lesson lesson = (Lesson) c.uniqueResult();

            if (lesson == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.addProperty("status", false);
                resp.addProperty("message", "Lesson not found");
                writeJson(response, gson, resp);
                return;
            }

            // OPTIONAL: enforce enrollment before download
            // Criteria ec = s.createCriteria(Enrollment.class);
            // ec.add(Restrictions.eq("user.id", user.getId()));
            // ec.add(Restrictions.eq("course.id", lesson.getCourse().getId()));
            // if (ec.uniqueResult() == null) { ... return; }

            String resourceField = lesson.getResource_file();
            if (resourceField == null || resourceField.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.addProperty("status", false);
                resp.addProperty("message", "No resource set for this lesson");
                writeJson(response, gson, resp);
                return;
            }

            // Support comma-separated resources
            String[] resources = resourceField.split("\\s*,\\s*");
            if (idx < 0 || idx >= resources.length) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.addProperty("status", false);
                resp.addProperty("message", "Invalid resource index");
                writeJson(response, gson, resp);
                return;
            }

            String resourceUrl = resources[idx];
            System.out.println("[DownloadResource] Resolved URL: " + resourceUrl);

            // Stream the remote file to client with attachment header
            try {
                URL url = new URL(resourceUrl);
                URLConnection conn = url.openConnection();
                conn.setConnectTimeout(15_000);
                conn.setReadTimeout(60_000);

                String contentType = conn.getContentType();
                if (contentType == null || contentType.trim().isEmpty()) {
                    contentType = "application/octet-stream";
                }

                String fileName = guessFileName(resourceUrl, contentType);
                // Force attachment
                response.setContentType(contentType);
                response.setHeader("Content-Disposition", contentDisposition(fileName));
                long len = conn.getContentLengthLong();
                if (len > 0) {
                    response.setContentLengthLong(len);
                }

                try (InputStream in = new BufferedInputStream(conn.getInputStream());
                     OutputStream out = new BufferedOutputStream(response.getOutputStream())) {
                    byte[] buf = new byte[8192];
                    int r;
                    while ((r = in.read(buf)) != -1) {
                        out.write(buf, 0, r);
                    }
                    out.flush();
                }
                System.out.println("[DownloadResource] Download streamed OK: " + fileName);
            } catch (Exception ex) {
                ex.printStackTrace();
                response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
                resp.addProperty("status", false);
                resp.addProperty("message", "Failed to fetch resource: " + ex.getMessage());
                writeJson(response, gson, resp);
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.addProperty("status", false);
            resp.addProperty("message", "Error: " + e.getMessage());
            writeJson(response, gson, resp);
        } finally {
            s.close();
        }
    }

    private void writeJson(HttpServletResponse response, Gson gson, JsonObject resp) throws IOException {
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(resp));
    }

    private String guessFileName(String url, String contentType) {
        try {
            String path = new URL(url).getPath();
            String name = path.substring(path.lastIndexOf('/') + 1);
            if (name == null || name.isEmpty() || name.contains("?")) {
                name = "resource";
            }
            // If Cloudinary-style extension missing, add from MIME
            if (!name.contains(".") && contentType != null) {
                if (contentType.contains("pdf")) name += ".pdf";
                else if (contentType.contains("zip")) name += ".zip";
                else if (contentType.contains("msword")) name += ".doc";
                else if (contentType.contains("officedocument.wordprocessingml")) name += ".docx";
                else if (contentType.contains("officedocument.spreadsheetml")) name += ".xlsx";
                else if (contentType.contains("officedocument.presentationml")) name += ".pptx";
            }
            return name;
        } catch (Exception e) {
            return "resource";
        }
    }

    private String contentDisposition(String fileName) {
        // RFC 5987 for UTF-8 filenames
        String ascii = fileName.replaceAll("[\\r\\n\"]", "_");
        String utf8 = java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        return "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + utf8;
    }
}
