package controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import hibernate.HibernateUtil;
import hibernate.Orders;
import hibernate.OrderItem;
import hibernate.User;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Order;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import com.itextpdf.text.pdf.*;

@WebServlet(name = "AdminReports", urlPatterns = {"/AdminReports"})
public class AdminReports extends HttpServlet {

    private final Gson gson = new Gson();
    private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // --- Session guard (adjust if you keep roles) ---
        HttpSession ses = request.getSession(false);
        if (ses == null || ses.getAttribute("user") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"status\":false,\"message\":\"Sign in as admin\"}");
            return;
        }

        String action = safe(request.getParameter("action")); // "summary" | "top-courses" | "users" | "download"
        String type   = safe(request.getParameter("type"));   // pdf type: "sales", "top-courses", "users"
        String from   = safe(request.getParameter("from"));   // yyyy-MM-dd
        String to     = safe(request.getParameter("to"));     // yyyy-MM-dd
        int limit     = parseIntOrDefault(request.getParameter("limit"), 10);

        if (action == null || action.isEmpty()) {
            action = "summary";
        }

        switch (action) {
            case "summary":
                handleSummary(from, to, response);
                break;
            case "top-courses":
                handleTopCourses(from, to, limit, response);
                break;
            case "users":
                handleNewUsers(from, to, response);
                break;
            case "download":
                // type = sales | top-courses | users
                handlePdfDownload(type, from, to, limit, response);
                break;
            default:
                writeJsonError(response, "Unknown action");
        }
    }

    // ----------------------- JSON HANDLERS -----------------------

    private void handleSummary(String from, String to, HttpServletResponse response) throws IOException {
        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            DateRange range = resolveRange(from, to);
            System.out.println("[AdminReports] summary range: " + range);

            SummaryResult rAll   = ordersAgg(s, null, null);
            SummaryResult rRange = ordersAgg(s, range.from, range.to);
            SummaryResult rToday = ordersAgg(s, startOfToday(), endOfToday());
            SummaryResult rMonth = ordersAgg(s, startOfMonth(), endOfMonth());

            JsonObject data = new JsonObject();
            data.addProperty("total_orders", rAll.count);
            data.addProperty("total_revenue", rAll.sum);

            data.addProperty("range_orders", rRange.count);
            data.addProperty("range_revenue", rRange.sum);

            data.addProperty("today_orders", rToday.count);
            data.addProperty("today_revenue", rToday.sum);

            data.addProperty("month_orders", rMonth.count);
            data.addProperty("month_revenue", rMonth.sum);

            writeJsonOk(response, data);
        } catch (Exception e) {
            e.printStackTrace();
            writeJsonException(response, e);
        } finally {
            s.close();
        }
    }

    private void handleTopCourses(String from, String to, int limit, HttpServletResponse response) throws IOException {
        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            DateRange range = resolveRange(from, to);

            // We want: sum(order_item.price) grouped by course, filtered by orders.created_at
            // Criteria on OrderItem with alias to Orders & Course
            Criteria c = s.createCriteria(OrderItem.class, "oi")
                    .createAlias("oi.orders", "o")
                    .createAlias("oi.course", "c");

            if (range.from != null) c.add(Restrictions.ge("o.created_at", range.from));
            if (range.to != null)   c.add(Restrictions.le("o.created_at", range.to));

            c.setProjection(Projections.projectionList()
                    .add(Projections.groupProperty("c.id"), "courseId")
                    .add(Projections.groupProperty("c.title"), "title")
                    .add(Projections.sum("price"), "revenue")
                    .add(Projections.rowCount(), "items"))
             .addOrder(Order.desc("revenue"));

            @SuppressWarnings("unchecked")
            List<Object[]> rows = c.list();

            JsonArray arr = new JsonArray();
            int i = 0;
            for (Object[] r : rows) {
                if (i++ >= limit) break;
                JsonObject o = new JsonObject();
                o.addProperty("course_id",   (Integer) r[0]);
                o.addProperty("title",       (String)  r[1]);
                o.addProperty("revenue",     toDouble(r[2]));
                o.addProperty("items_sold",  toLong(r[3]));
                arr.add(o);
            }

            JsonObject data = new JsonObject();
            data.add("top_courses", arr);
            writeJsonOk(response, data);
        } catch (Exception e) {
            e.printStackTrace();
            writeJsonException(response, e);
        } finally {
            s.close();
        }
    }

    private void handleNewUsers(String from, String to, HttpServletResponse response) throws IOException {
        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            DateRange range = resolveRange(from, to);
            Criteria c = s.createCriteria(User.class);

            if (range.from != null) c.add(Restrictions.ge("created_at", range.from));
            if (range.to != null)   c.add(Restrictions.le("created_at", range.to));

            c.setProjection(Projections.projectionList()
                    .add(Projections.rowCount(), "count"));

            Number count = (Number) c.uniqueResult();

            JsonObject data = new JsonObject();
            data.addProperty("new_users", count == null ? 0 : count.longValue());
            writeJsonOk(response, data);
        } catch (Exception e) {
            e.printStackTrace();
            writeJsonException(response, e);
        } finally {
            s.close();
        }
    }

    // ----------------------- PDF HANDLER -----------------------

    private void handlePdfDownload(String type, String from, String to, int limit, HttpServletResponse response) throws IOException {
        if (type == null || type.isEmpty()) type = "sales";
        DateRange range = resolveRange(from, to);

        // Build content
        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            response.setContentType("application/pdf");
            String fileName = (type + "_report_" + (range.from != null ? df.format(range.from) : "all") + "_to_" + (range.to != null ? df.format(range.to) : "all") + ".pdf")
                    .replace("__", "_");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

            Document doc = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
            OutputStream os = response.getOutputStream();
            PdfWriter.getInstance(doc, os);
            doc.open();

            addReportHeader(doc, type, range);

            switch (type) {
                case "sales":
                    SummaryResult rAll   = ordersAgg(s, range.from, range.to);
                    addKeyValue(doc, "Orders",   String.valueOf(rAll.count));
                    addKeyValue(doc, "Revenue",  currency(rAll.sum));
                    addOrdersTable(doc, s, range);
                    break;

                case "top-courses":
                    addTopCoursesTable(doc, s, range, limit);
                    break;

                case "users":
                    addNewUsersTable(doc, s, range);
                    break;

                default:
                    addParagraph(doc, "Unknown report type.");
            }

            doc.close();
            os.flush();
        } catch (Exception e) {
            e.printStackTrace();
            response.reset();
            writeJsonError(response, "Failed to generate PDF: " + e.getMessage());
        } finally {
            s.close();
        }
    }

    // ----------------------- PDF HELPERS -----------------------

    private void addReportHeader(Document doc, String type, DateRange range) throws DocumentException {
        Paragraph h = new Paragraph("Admin Report – " + titleCase(type),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18));
        h.setAlignment(Element.ALIGN_LEFT);
        doc.add(h);

        Paragraph sub = new Paragraph("Range: " + (range.from != null ? df.format(range.from) : "All")
                + " to " + (range.to != null ? df.format(range.to) : "All")
                + "    •    Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()),
                FontFactory.getFont(FontFactory.HELVETICA, 10));
        sub.setSpacingAfter(16f);
        doc.add(sub);
    }

    private void addKeyValue(Document doc, String key, String val) throws DocumentException {
        Paragraph p = new Paragraph(key + ": " + val,
                FontFactory.getFont(FontFactory.HELVETICA, 12));
        p.setSpacingAfter(6f);
        doc.add(p);
    }

    private void addParagraph(Document doc, String text) throws DocumentException {
        Paragraph p = new Paragraph(text, FontFactory.getFont(FontFactory.HELVETICA, 12));
        p.setSpacingAfter(10f);
        doc.add(p);
    }

    private void addOrdersTable(Document doc, Session s, DateRange range) throws DocumentException {
        // Simple orders list (id, user_id, total, status_id, created_at)
        Criteria c = s.createCriteria(Orders.class);
        if (range.from != null) c.add(Restrictions.ge("created_at", range.from));
        if (range.to != null)   c.add(Restrictions.le("created_at", range.to));
        c.addOrder(Order.desc("created_at"));

        @SuppressWarnings("unchecked")
        List<Orders> rows = c.list();

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        addHeader(table, "Order ID", "User ID", "Total (LKR)", "Status ID", "Created At");

        for (Orders o : rows) {
            table.addCell(String.valueOf(o.getId()));
            table.addCell(String.valueOf(o.getUser().getId()));
            table.addCell(currency(o.getTotal()));
            table.addCell(String.valueOf(o.getStatus().getId()));
            table.addCell(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(o.getCreated_at()));
        }
        doc.add(table);
    }

    private void addTopCoursesTable(Document doc, Session s, DateRange range, int limit) throws DocumentException {
        Criteria c = s.createCriteria(OrderItem.class, "oi")
                .createAlias("oi.orders", "o")
                .createAlias("oi.course", "c");

        if (range.from != null) c.add(Restrictions.ge("o.created_at", range.from));
        if (range.to != null)   c.add(Restrictions.le("o.created_at", range.to));

        c.setProjection(Projections.projectionList()
                .add(Projections.groupProperty("c.id"), "courseId")
                .add(Projections.groupProperty("c.title"), "title")
                .add(Projections.sum("price"), "revenue")
                .add(Projections.rowCount(), "items"))
         .addOrder(Order.desc("revenue"));

        @SuppressWarnings("unchecked")
        List<Object[]> rows = c.list();

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        addHeader(table, "Course ID", "Title", "Items Sold", "Revenue (LKR)");

        int i = 0;
        for (Object[] r : rows) {
            if (i++ >= limit) break;
            table.addCell(String.valueOf((Integer) r[0]));
            table.addCell((String) r[1]);
            table.addCell(String.valueOf(toLong(r[3])));
            table.addCell(currency(toDouble(r[2])));
        }
        doc.add(table);
    }

    private void addNewUsersTable(Document doc, Session s, DateRange range) throws DocumentException {
        Criteria c = s.createCriteria(User.class);
        if (range.from != null) c.add(Restrictions.ge("created_at", range.from));
        if (range.to != null)   c.add(Restrictions.le("created_at", range.to));
        c.addOrder(Order.desc("created_at"));

        @SuppressWarnings("unchecked")
        List<User> rows = c.list();

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        addHeader(table, "User ID", "Name", "Email", "Status ID", "Created At");

        for (User u : rows) {
            table.addCell(String.valueOf(u.getId()));
            table.addCell(u.getFname() + " " + u.getLname());
            table.addCell(u.getEmail());
            table.addCell(String.valueOf(u.getStatus_id().getId()));
            table.addCell(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(u.getCreated_at()));
        }
        doc.add(table);
    }

    private void addHeader(PdfPTable t, String... labels) {
        Font f = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        for (String s : labels) {
            PdfPCell th = new PdfPCell(new Phrase(s, f));
            th.setBackgroundColor(new BaseColor(240, 240, 240));
            t.addCell(th);
        }
    }

    // ----------------------- AGGREGATION -----------------------

    private SummaryResult ordersAgg(Session s, Date from, Date to) {
        Criteria c = s.createCriteria(Orders.class);
        if (from != null) c.add(Restrictions.ge("created_at", from));
        if (to   != null) c.add(Restrictions.le("created_at", to));

        c.setProjection(Projections.projectionList()
                .add(Projections.rowCount(), "count")
                .add(Projections.sum("total"), "sum"));

        Object[] r = (Object[]) c.uniqueResult();
        long count = r != null && r[0] != null ? ((Number) r[0]).longValue() : 0L;
        double sum = r != null && r[1] != null ? ((Number) r[1]).doubleValue() : 0.0;
        return new SummaryResult(count, sum);
    }

    // ----------------------- DATE HELPERS -----------------------

    private DateRange resolveRange(String from, String to) {
        Date f = parseDate(from);
        Date t = parseDate(to);
        return new DateRange(f, t);
    }

    private Date parseDate(String s) {
        if (s == null || s.isEmpty()) return null;
        try { return df.parse(s); } catch (ParseException e) { return null; }
    }

    private Date startOfToday() {
        Calendar cal = Calendar.getInstance();
        zeroTime(cal);
        return cal.getTime();
    }

    private Date endOfToday() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    private Date startOfMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        zeroTime(cal);
        return cal.getTime();
    }

    private Date endOfMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    private void zeroTime(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    // ----------------------- UTIL -----------------------

    private void writeJsonOk(HttpServletResponse response, JsonObject data) throws IOException {
        JsonObject resp = new JsonObject();
        resp.addProperty("status", true);
        resp.add("data", data);
        writeJsonResponse(response, resp);
    }

    private void writeJsonError(HttpServletResponse response, String msg) throws IOException {
        JsonObject resp = new JsonObject();
        resp.addProperty("status", false);
        resp.addProperty("message", msg);
        writeJsonResponse(response, resp);
    }

    private void writeJsonException(HttpServletResponse response, Exception e) throws IOException {
        JsonObject resp = new JsonObject();
        resp.addProperty("status", false);
        resp.addProperty("message", "Error: " + e.getMessage());
        writeJsonResponse(response, resp);
    }

    private void writeJsonResponse(HttpServletResponse response, JsonObject obj) throws IOException {
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(obj));
    }

    private String safe(String v) { return v == null ? null : v.trim(); }
    private int parseIntOrDefault(String s, int def) { try { return Integer.parseInt(s); } catch (Exception e) { return def; } }
    private String currency(double d) { return String.format(Locale.US, "%,.2f", d); }
    private double toDouble(Object n) { return n == null ? 0.0 : ((Number) n).doubleValue(); }
    private long toLong(Object n) { return n == null ? 0L : ((Number) n).longValue(); }

    private static class SummaryResult {
        long count; double sum;
        SummaryResult(long c, double s) { this.count = c; this.sum = s; }
    }
    private static class DateRange {
        Date from, to;
        DateRange(Date f, Date t) { this.from = f; this.to = t; }
        public String toString() { return "[from=" + f(from) + ", to=" + f(to) + "]"; }
        private String f(Date d) { return d == null ? "null" : new SimpleDateFormat("yyyy-MM-dd").format(d); }
    }

    private String titleCase(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0,1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
