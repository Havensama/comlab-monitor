package ph.ue.comlab.db;

import ph.ue.comlab.AppConfig;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.LocalDateTime;

/**
 * Opens connections to the MySQL database on the admin laptop.
 * A fresh connection is opened per operation: slower by a few ms but survives Wi-Fi hiccups,
 * which matters more on demo day than raw speed.
 */
public final class Database {
    private Database() {}

    public static Connection get() throws SQLException {
        String url = "jdbc:mysql://" + AppConfig.get("db.host", "localhost") + ":" + AppConfig.getInt("db.port", 3306)
                + "/" + AppConfig.get("db.name", "comlab")
                + (AppConfig.getBool("db.ssl", false)
                        ? "?sslMode=REQUIRED"                      // cloud database (e.g. Aiven): encrypted
                        : "?useSSL=false&allowPublicKeyRetrieval=true") // XAMPP on the local network
                + "&connectTimeout=8000&socketTimeout=15000"
                + "&characterEncoding=UTF-8";
        return DriverManager.getConnection(url, AppConfig.get("db.user", "labuser"), AppConfig.get("db.pass", "lab123"));
    }

    /** Quick check used on startup and by the "can't reach server" banner. */
    public static boolean ping() {
        try (Connection c = get(); Statement s = c.createStatement()) {
            s.execute("SELECT 1");
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /** Human-friendly explanation for the most common demo-day failures. */
    public static String explain(Exception e) {
        String m = String.valueOf(e.getMessage());
        String host = AppConfig.get("db.host", "localhost");
        if (m.contains("Communications link failure") || m.contains("connect timed out") || m.contains("Connection refused"))
            return "Can't reach the lab server at " + host + ". Check the Wi-Fi/hotspot, that MySQL is running "
                    + "on the admin laptop, and that its firewall allows port 3306.";
        if (m.contains("No suitable driver"))
            return "The MySQL driver is missing. Open the project in IntelliJ as a Maven project (pom.xml) "
                    + "so mysql-connector-j downloads, or run the built comlab.jar.";
        if (m.contains("Access denied"))
            return "The server rejected the database login. Check db.user / db.pass in config.properties "
                    + "and that schema.sql was run (it creates the labuser account).";
        if (m.contains("Unknown database"))
            return "Database 'comlab' not found on " + host + ". Run schema.sql on the admin laptop first.";
        if (m.contains("is not allowed to connect"))
            return "MySQL on the admin laptop does not allow remote logins yet. Run the GRANT lines in schema.sql.";
        return m;
    }

    public static String sha256(String text) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static LocalDateTime dt(ResultSet rs, String col) throws SQLException {
        Timestamp t = rs.getTimestamp(col);
        return t == null ? null : t.toLocalDateTime();
    }

    static Boolean boolOrNull(ResultSet rs, String col) throws SQLException {
        boolean v = rs.getBoolean(col);
        return rs.wasNull() ? null : v;
    }
}
