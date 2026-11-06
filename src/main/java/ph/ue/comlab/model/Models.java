package ph.ue.comlab.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Plain data classes (one per table / view). Kept in one file for easy reading. */
public final class Models {
    private Models() {}

    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a");
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d");
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("MMM d, h:mm a");

    /** students table */
    public record Student(String studentNo, String fullName, String course) {
        public String initials() {
            String[] parts = fullName.trim().split("\\s+");
            if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
            return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
        }
    }

    /** admins table */
    public record Admin(String username, String fullName) {}

    /** sessions table — one row per log in / log out */
    public record LabSession(int sessionId, String studentNo, String pcId, LocalDateTime timeIn,
                             LocalDateTime timeOut, long elapsedSecondsAtLoad) {}

    /** reports table */
    public record Report(int reportId, String pcId, String studentNo, String studentName, String category,
                         String priority, String description, String status, String source,
                         LocalDateTime createdAt, boolean hasPhoto) {
        public String title() {
            String d = description == null ? "" : description.trim();
            return d.isEmpty() ? category : d;
        }
    }

    /** pcs table + who is currently using it */
    public record PcStatus(String pcId, String location, boolean online, String currentStudentNo,
                           String currentStudentName, LocalDateTime sessionStart, long sessionSeconds,
                           int openReports) {}

    /** a row of the admin "Sessions" log */
    public record SessionRow(int sessionId, String studentNo, String studentName, String pcId,
                             LocalDateTime timeIn, LocalDateTime timeOut, long seconds,
                             Boolean monitorOk, Boolean keyboardOk, Boolean mouseOk, String endReason) {
        public boolean active() { return timeOut == null; }
        public String precheckSummary() {
            if (monitorOk == null) return "Not done";
            int bad = (monitorOk ? 0 : 1) + (keyboardOk ? 0 : 1) + (mouseOk ? 0 : 1);
            return bad == 0 ? "All OK" : bad + " issue" + (bad > 1 ? "s" : "");
        }
    }

    /** numbers on the admin dashboard */
    public record Stats(int pcsOnline, int pcsTotal, int activeSessions, int openReports,
                        int highPriorityOpen, int sessionsToday) {}

    public static String hms(long seconds) {
        long h = seconds / 3600, m = (seconds % 3600) / 60, s = seconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    public static String shortDuration(long seconds) {
        Duration d = Duration.ofSeconds(seconds);
        if (d.toHours() > 0) return d.toHours() + "h " + d.toMinutesPart() + "m";
        if (d.toMinutes() > 0) return d.toMinutes() + "m";
        return seconds + "s";
    }
}
