package ph.ue.comlab;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/**
 * Settings read from config.properties (next to the .jar, or the project folder in IntelliJ).
 * Change db.host on demo day if the admin laptop's IP changes — no rebuild needed.
 */
public final class AppConfig {
    private static final Properties P = new Properties();
    private static Path file;

    private AppConfig() {}

    public static void load() {
        file = locate();
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file)) { P.load(r); } catch (IOException ignored) {}
        } else {
            writeDefault(file);
            try (Reader r = Files.newBufferedReader(file)) { P.load(r); } catch (IOException ignored) {}
        }
    }

    private static Path locate() {
        // Packaged app (ComLab Student.exe / ComLab Admin.exe): config.properties sits next to the .exe
        String exe = System.getProperty("jpackage.app-path");
        if (exe != null) {
            Path p = Paths.get(exe).toAbsolutePath().getParent().resolve("config.properties");
            if (Files.exists(p)) return p;
        }
        Path cwd = Paths.get("config.properties").toAbsolutePath();
        if (Files.exists(cwd)) return cwd;
        try {
            Path jarDir = Paths.get(AppConfig.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
            if (jarDir != null) {
                Path p = jarDir.resolve("config.properties");
                if (Files.exists(p)) return p;
            }
        } catch (Exception ignored) {}
        return cwd;
    }

    private static void writeDefault(Path p) {
        String s = """
                # ===== UE CompLab Monitoring System =====
                # Which app this laptop runs: student or admin
                app.mode=student

                # ---- Database (lives on the ADMIN laptop) ----
                # On the admin laptop itself use localhost.
                # On the student laptop put the admin laptop's IPv4 address (run ipconfig on the admin laptop).
                db.host=localhost
                db.port=3306
                db.name=comlab
                db.user=labuser
                db.pass=lab123

                # ---- This workstation (student laptop only) ----
                pc.id=PC-14
                pc.location=Lab 6 · CoE Building
                lab.hours=7:00 AM – 9:00 PM

                # Secret exit for the kiosk: press Ctrl+Shift+F12, then type this password
                kiosk.exitPassword=letmeout
                # Set to false while developing in IntelliJ so the window doesn't lock your screen
                kiosk.lock=true
                """;
        try { Files.writeString(p, s); } catch (IOException ignored) {}
    }

    public static String get(String key, String def) { return P.getProperty(key, def).trim(); }
    public static int getInt(String key, int def) {
        try { return Integer.parseInt(get(key, String.valueOf(def))); } catch (NumberFormatException e) { return def; }
    }
    public static boolean getBool(String key, boolean def) { return Boolean.parseBoolean(get(key, String.valueOf(def))); }
    public static Path file() { return file; }

    public static String pcId()       { return get("pc.id", "PC-01"); }
    public static String pcLocation() { return get("pc.location", "Computer Laboratory"); }
}
