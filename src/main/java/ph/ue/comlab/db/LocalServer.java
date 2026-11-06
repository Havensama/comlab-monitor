package ph.ue.comlab.db;

import ph.ue.comlab.AppConfig;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.function.Consumer;

/**
 * Admin laptop only: makes "just open the Admin app" work.
 * <ol>
 *   <li>If MySQL is not running, starts XAMPP's MySQL (mysqld.exe) in the background,
 *       so nobody has to open the XAMPP Control Panel.</li>
 *   <li>If the comlab database does not exist yet, creates it from the bundled schema.sql.</li>
 *   <li>Makes sure the labuser account exists and may use comlab (the apps log in with it).</li>
 * </ol>
 * Only runs when db.host points at this laptop (localhost / 127.0.0.1).
 */
public final class LocalServer {
    private LocalServer() {}

    public static boolean isLocal() {
        String h = AppConfig.get("db.host", "localhost").trim().toLowerCase();
        return h.equals("localhost") || h.equals("127.0.0.1") || h.equals("::1");
    }

    /** @return null when the database is ready, otherwise a message to show on the login screen. */
    public static String prepare(Consumer<String> status) {
        if (!isLocal()) return null;
        int port = AppConfig.getInt("db.port", 3306);

        // 1) MySQL running?
        if (!portOpen(port)) {
            Path xampp = Paths.get(AppConfig.get("xampp.dir", "C:\\xampp"));
            Path mysqld = xampp.resolve("mysql").resolve("bin").resolve("mysqld.exe");
            Path ini = xampp.resolve("mysql").resolve("bin").resolve("my.ini");
            if (!Files.exists(mysqld))
                return "MySQL is not running and XAMPP was not found at " + xampp
                        + ". Install XAMPP there, or set xampp.dir in config.properties.";
            status.accept("Starting the database…");
            try {
                ProcessBuilder pb = Files.exists(ini)
                        ? new ProcessBuilder(mysqld.toString(), "--defaults-file=" + ini, "--standalone")
                        : new ProcessBuilder(mysqld.toString(), "--standalone");
                pb.directory(xampp.toFile());
                pb.redirectErrorStream(true);
                pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                pb.start(); // keeps running after the Admin app closes, so students stay connected
            } catch (Exception e) {
                return "Could not start MySQL from XAMPP: " + e.getMessage();
            }
            long until = System.currentTimeMillis() + 30_000;
            while (!portOpen(port)) {
                if (System.currentTimeMillis() > until)
                    return "MySQL did not start. Open the XAMPP Control Panel once and check the MySQL log.";
                sleep(500);
            }
            sleep(1500); // let it finish opening its data files
        }

        // 2) + 3) database and app account, using XAMPP's root account (no password by default)
        String rootUrl = "jdbc:mysql://127.0.0.1:" + port
                + "/?useSSL=false&allowPublicKeyRetrieval=true&allowMultiQueries=true&connectTimeout=5000";
        try (Connection c = DriverManager.getConnection(rootUrl,
                AppConfig.get("db.rootUser", "root"), AppConfig.get("db.rootPass", ""));
             Statement st = c.createStatement()) {
            String db = AppConfig.get("db.name", "comlab");
            boolean exists;
            try (ResultSet rs = st.executeQuery(
                    "SELECT 1 FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = '" + db.replace("'", "") + "'")) {
                exists = rs.next();
            }
            if (!exists) {
                status.accept("Setting up the database (first run)…");
                String sql = bundledSchema();
                if (sql == null) return "schema.sql is missing from the app. Rebuild comlab.jar.";
                st.execute(sql);
            }
            String user = AppConfig.get("db.user", "labuser").replace("'", "");
            String pass = AppConfig.get("db.pass", "lab123").replace("'", "''");
            for (String host : new String[]{"%", "localhost"}) {
                st.execute("CREATE USER IF NOT EXISTS '" + user + "'@'" + host + "' IDENTIFIED BY '" + pass + "'");
                st.execute("GRANT ALL PRIVILEGES ON `" + db.replace("`", "") + "`.* TO '" + user + "'@'" + host + "'");
            }
            st.execute("FLUSH PRIVILEGES");
        } catch (SQLException e) {
            // root has a password or is locked: fine as long as the normal login already works
            if (Database.ping()) return null;
            return "Database setup needs MySQL's root password. Add db.rootPass=... to config.properties. ("
                    + e.getMessage() + ")";
        }
        return Database.ping() ? null : "The database started but the app can't log in to it yet. Try again in a few seconds.";
    }

    private static String bundledSchema() {
        try (InputStream in = LocalServer.class.getResourceAsStream("/schema.sql")) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean portOpen(int port) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress("127.0.0.1", port), 400);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }
}
