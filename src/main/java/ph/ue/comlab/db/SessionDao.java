package ph.ue.comlab.db;

import ph.ue.comlab.model.Models.LabSession;
import ph.ue.comlab.model.Models.SessionRow;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Lab sessions: time in / time out, the equipment check, and the admin log. */
public class SessionDao {

    /** Student is already logged in on ANOTHER PC? Returns that PC id. */
    public Optional<String> activeElsewhere(String studentNo, String thisPc) throws SQLException {
        String sql = "SELECT pc_id FROM sessions WHERE student_no = ? AND time_out IS NULL AND pc_id <> ? LIMIT 1";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentNo);
            ps.setString(2, thisPc);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString(1)) : Optional.empty();
            }
        }
    }

    /** Save time_in. Any session left open on this PC (e.g. laptop crashed) is closed first. */
    public LabSession start(String studentNo, String pcId) throws SQLException {
        try (Connection c = Database.get()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE sessions SET time_out = NOW(), end_reason = 'RECOVERED' WHERE pc_id = ? AND time_out IS NULL")) {
                    ps.setString(1, pcId);
                    ps.executeUpdate();
                }
                int id;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO sessions (student_no, pc_id, time_in) VALUES (?, ?, NOW())",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, studentNo);
                    ps.setString(2, pcId);
                    ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) { k.next(); id = k.getInt(1); }
                }
                c.commit();
                return find(c, id);
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private LabSession find(Connection c, int id) throws SQLException {
        String sql = "SELECT session_id, student_no, pc_id, time_in, time_out, "
                + "TIMESTAMPDIFF(SECOND, time_in, NOW()) AS secs FROM sessions WHERE session_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new LabSession(rs.getInt(1), rs.getString(2), rs.getString(3),
                        Database.dt(rs, "time_in"), Database.dt(rs, "time_out"), rs.getLong("secs"));
            }
        }
    }

    /** Save time_out. */
    public void end(int sessionId, String reason) throws SQLException {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(
                "UPDATE sessions SET time_out = NOW(), end_reason = ? WHERE session_id = ? AND time_out IS NULL")) {
            ps.setString(1, reason);
            ps.setInt(2, sessionId);
            ps.executeUpdate();
        }
    }

    /** Has the admin ended this session from the dashboard? */
    public boolean isClosed(int sessionId) throws SQLException {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("SELECT time_out IS NOT NULL FROM sessions WHERE session_id = ?")) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) { return !rs.next() || rs.getBoolean(1); }
        }
    }

    public void saveEquipmentCheck(int sessionId, boolean monitor, boolean keyboard, boolean mouse) throws SQLException {
        String sql = "INSERT INTO equipment_checks (session_id, monitor_ok, keyboard_ok, mouse_ok) VALUES (?,?,?,?) "
                + "ON DUPLICATE KEY UPDATE monitor_ok = VALUES(monitor_ok), keyboard_ok = VALUES(keyboard_ok), "
                + "mouse_ok = VALUES(mouse_ok), checked_at = NOW()";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ps.setBoolean(2, monitor);
            ps.setBoolean(3, keyboard);
            ps.setBoolean(4, mouse);
            ps.executeUpdate();
        }
    }

    /** Admin "Sessions" tab. */
    public List<SessionRow> log(String search, boolean activeOnly, boolean todayOnly) throws SQLException {
        String sql = "SELECT s.session_id, s.student_no, st.full_name, s.pc_id, s.time_in, s.time_out, s.end_reason, "
                + "TIMESTAMPDIFF(SECOND, s.time_in, COALESCE(s.time_out, NOW())) AS secs, "
                + "e.monitor_ok, e.keyboard_ok, e.mouse_ok "
                + "FROM sessions s JOIN students st ON st.student_no = s.student_no "
                + "LEFT JOIN equipment_checks e ON e.session_id = s.session_id "
                + "WHERE (s.student_no LIKE ? OR st.full_name LIKE ? OR s.pc_id LIKE ?) "
                + (activeOnly ? "AND s.time_out IS NULL " : "")
                + (todayOnly ? "AND DATE(s.time_in) = CURDATE() " : "")
                + "ORDER BY s.time_in DESC LIMIT 500";
        List<SessionRow> out = new ArrayList<>();
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            String q = "%" + (search == null ? "" : search.trim()) + "%";
            ps.setString(1, q); ps.setString(2, q); ps.setString(3, q);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new SessionRow(rs.getInt("session_id"), rs.getString("student_no"), rs.getString("full_name"),
                            rs.getString("pc_id"), Database.dt(rs, "time_in"), Database.dt(rs, "time_out"),
                            rs.getLong("secs"), Database.boolOrNull(rs, "monitor_ok"),
                            Database.boolOrNull(rs, "keyboard_ok"), Database.boolOrNull(rs, "mouse_ok"),
                            rs.getString("end_reason")));
                }
            }
        }
        return out;
    }
}
