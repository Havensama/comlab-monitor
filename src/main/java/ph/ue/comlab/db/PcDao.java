package ph.ue.comlab.db;

import ph.ue.comlab.model.Models.PcStatus;
import ph.ue.comlab.model.Models.Stats;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Workstations: registration, heartbeat ("I'm online") and the admin dashboard. */
public class PcDao {

    /** A PC is "online" if its student app sent a heartbeat in the last 30 seconds. */
    public static final int ONLINE_WINDOW_SECONDS = 30;

    /** Called by the student app on startup and every 10 seconds. Creates the PC row if it's new. */
    public void heartbeat(String pcId, String location) throws SQLException {
        String sql = "INSERT INTO pcs (pc_id, location, last_seen) VALUES (?, ?, NOW()) "
                + "ON DUPLICATE KEY UPDATE last_seen = NOW(), location = VALUES(location)";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, pcId);
            ps.setString(2, location);
            ps.executeUpdate();
        }
    }

    /** Called when the student app is closed with the secret exit, so the PC shows offline right away. */
    public void markOffline(String pcId) throws SQLException {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("UPDATE pcs SET last_seen = NULL WHERE pc_id = ?")) {
            ps.setString(1, pcId);
            ps.executeUpdate();
        }
    }

    public List<PcStatus> all() throws SQLException {
        String sql = "SELECT p.pc_id, p.location, "
                + "(p.last_seen IS NOT NULL AND p.last_seen > NOW() - INTERVAL " + ONLINE_WINDOW_SECONDS + " SECOND) AS online, "
                + "s.student_no, st.full_name, s.time_in, TIMESTAMPDIFF(SECOND, s.time_in, NOW()) AS secs, "
                + "(SELECT COUNT(*) FROM reports r WHERE r.pc_id = p.pc_id AND r.status <> 'RESOLVED') AS open_reports "
                + "FROM pcs p "
                + "LEFT JOIN sessions s ON s.pc_id = p.pc_id AND s.time_out IS NULL "
                + "LEFT JOIN students st ON st.student_no = s.student_no "
                + "ORDER BY p.pc_id";
        List<PcStatus> out = new ArrayList<>();
        try (Connection c = Database.get(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                out.add(new PcStatus(rs.getString("pc_id"), rs.getString("location"), rs.getBoolean("online"),
                        rs.getString("student_no"), rs.getString("full_name"), Database.dt(rs, "time_in"),
                        rs.getLong("secs"), rs.getInt("open_reports")));
            }
        }
        return out;
    }

    public Stats stats() throws SQLException {
        String sql = "SELECT "
                + "(SELECT COUNT(*) FROM pcs WHERE last_seen > NOW() - INTERVAL " + ONLINE_WINDOW_SECONDS + " SECOND), "
                + "(SELECT COUNT(*) FROM pcs), "
                + "(SELECT COUNT(*) FROM sessions WHERE time_out IS NULL), "
                + "(SELECT COUNT(*) FROM reports WHERE status <> 'RESOLVED'), "
                + "(SELECT COUNT(*) FROM reports WHERE status <> 'RESOLVED' AND priority = 'High'), "
                + "(SELECT COUNT(*) FROM sessions WHERE DATE(time_in) = CURDATE())";
        try (Connection c = Database.get(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return new Stats(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getInt(5), rs.getInt(6));
        }
    }
}
