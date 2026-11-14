package ph.ue.comlab.db;

import ph.ue.comlab.model.Models.Report;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** PC problem reports (from the pre-check, the "Other concerns" form, and "Report issue"). */
public class ReportDao {

    private static final String SELECT = "SELECT r.report_id, r.pc_id, r.student_no, st.full_name, r.category, r.priority, "
            + "r.description, r.status, r.source, r.created_at, (r.photo IS NOT NULL) AS has_photo "
            + "FROM reports r LEFT JOIN students st ON st.student_no = r.student_no ";

    public void create(Integer sessionId, String pcId, String studentNo, String category, String priority,
                       String description, String source, byte[] photo, String photoName) throws SQLException {
        String sql = "INSERT INTO reports (session_id, pc_id, student_no, category, priority, description, source, photo, photo_name) "
                + "VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (sessionId == null) ps.setNull(1, Types.INTEGER); else ps.setInt(1, sessionId);
            ps.setString(2, pcId);
            ps.setString(3, studentNo);
            ps.setString(4, category);
            ps.setString(5, priority);
            ps.setString(6, description);
            ps.setString(7, source);
            if (photo == null) ps.setNull(8, Types.BLOB); else ps.setBytes(8, photo);
            ps.setString(9, photoName);
            ps.executeUpdate();
        }
    }

    /** "Recent reports on PC-14" side panel. */
    public List<Report> recentForPc(String pcId, int limit) throws SQLException {
        return query(SELECT + "WHERE r.pc_id = ? ORDER BY r.created_at DESC LIMIT " + limit, pcId);
    }

    /** Admin "Reports" tab. status = null for all, or PENDING / IN_PROGRESS / RESOLVED / OPEN. */
    public List<Report> list(String status, String search) throws SQLException {
        String where = "WHERE (r.pc_id LIKE ? OR st.full_name LIKE ? OR r.description LIKE ? OR r.category LIKE ?) ";
        if ("OPEN".equals(status)) where += "AND r.status <> 'RESOLVED' ";
        else if (status != null) where += "AND r.status = '" + status.replaceAll("[^A-Z_]", "") + "' ";
        String q = "%" + (search == null ? "" : search.trim()) + "%";
        return query(SELECT + where + "ORDER BY (r.status = 'RESOLVED'), FIELD(r.priority,'High','Medium','Low'), r.created_at DESC LIMIT 500",
                q, q, q, q);
    }

    public void setStatus(int reportId, String status) throws SQLException {
        String sql = "UPDATE reports SET status = ?, resolved_at = " + ("RESOLVED".equals(status) ? "NOW()" : "NULL")
                + " WHERE report_id = ?";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, reportId);
            ps.executeUpdate();
        }
    }

    public byte[] photo(int reportId) throws SQLException {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("SELECT photo FROM reports WHERE report_id = ?")) {
            ps.setInt(1, reportId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getBytes(1) : null; }
        }
    }

    private List<Report> query(String sql, String... params) throws SQLException {
        List<Report> out = new ArrayList<>();
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setString(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Report(rs.getInt("report_id"), rs.getString("pc_id"), rs.getString("student_no"),
                            rs.getString("full_name"), rs.getString("category"), rs.getString("priority"),
                            rs.getString("description"), rs.getString("status"), rs.getString("source"),
                            Database.dt(rs, "created_at"), rs.getBoolean("has_photo")));
                }
            }
        }
        return out;
    }
}
