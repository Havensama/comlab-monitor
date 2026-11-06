package ph.ue.comlab.admin;

import ph.ue.comlab.db.Database;
import ph.ue.comlab.db.SessionDao;
import ph.ue.comlab.model.Models;
import ph.ue.comlab.model.Models.SessionRow;
import ph.ue.comlab.ui.*;
import ph.ue.comlab.ui.DataTable.Column;

import javax.swing.*;
import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Admin "Sessions": the lab log that replaces the paper logbook. */
class SessionsPage implements AdminUI.Page<List<SessionRow>> {

    private final SessionDao dao = new SessionDao();
    private final DataTable<SessionRow> table;
    private final UI.SecondaryButton endBtn = new UI.SecondaryButton("End session", "logout", 14, 16);
    private final JComponent view;
    private final Runnable refresh;
    private volatile String search = "";
    private volatile String filter = "Today";

    SessionsPage(Runnable refresh) {
        this.refresh = refresh;
        table = new DataTable<>(List.of(
                Column.text("Student", 190, SessionRow::studentName),
                Column.text("Student no.", 130, SessionRow::studentNo),
                Column.text("PC", 70, SessionRow::pcId),
                Column.text("Time in", 150, r -> r.timeIn().format(Models.DATE_TIME)),
                Column.text("Time out", 110, r -> r.timeOut() == null ? null : r.timeOut().format(Models.TIME)),
                Column.text("Duration", 90, r -> Models.shortDuration(r.seconds())),
                Column.pill("Pre-check", 110, r -> switch (r.precheckSummary()) {
                    case "All OK" -> UI.status("All OK", Theme.SUCCESS, Theme.SUCCESS_SOFT, null);
                    case "Not done" -> UI.status("Not done", Theme.TEXT_2, Theme.CANVAS, null);
                    default -> UI.status(r.precheckSummary(), Theme.WARNING, Theme.WARNING_SOFT, null);
                }),
                Column.pill("Status", 120, r -> r.active()
                        ? UI.status("Active", Theme.SUCCESS, Theme.SUCCESS_SOFT, Theme.SUCCESS_DOT)
                        : UI.status(endedLabel(r.endReason()), Theme.TEXT_2, Theme.CANVAS, null))
        ), SessionRow::sessionId, "No sessions match.");

        endBtn.setEnabled(false);
        endBtn.addActionListener(e -> endSelected());
        table.onSelect(r -> endBtn.setEnabled(r != null && r.active()));

        UI.SecondaryButton export = new UI.SecondaryButton("Export CSV", "download", 14, 16);
        export.addActionListener(e -> exportCsv());

        JPanel toolbar = Flex.row(12).align(Flex.Align.CENTER).build(
                AdminUI.search("Search name, student no. or PC", 320, q -> { search = q; refresh.run(); }),
                AdminUI.filter(new String[]{"Today", "Active", "All"}, 0, 270, f -> { filter = f; refresh.run(); }),
                Flex.spring(), endBtn, export);

        JPanel page = Flex.column(20).align(Flex.Align.STRETCH).pad(32, 40, 32, 40).build(
                AdminUI.header("SESSION LOGS", "Who used which PC", "Every log in and log out, recorded automatically.", null),
                toolbar, Flex.grow(table));
        page.setOpaque(true);
        page.setBackground(Theme.CANVAS);
        view = page;
    }

    private static String endedLabel(String reason) {
        if ("ADMIN".equals(reason)) return "By admin";
        if ("RECOVERED".equals(reason)) return "Auto-closed";
        return "Logged out";
    }

    @Override public JComponent view() { return view; }

    @Override public List<SessionRow> load() throws Exception {
        return dao.log(search, "Active".equals(filter), "Today".equals(filter));
    }

    @Override public void show(List<SessionRow> rows) {
        table.setRows(rows);
        SessionRow s = table.selected();
        endBtn.setEnabled(s != null && s.active());
    }

    private void endSelected() {
        SessionRow r = table.selected();
        if (r == null || !r.active()) return;
        int ok = JOptionPane.showConfirmDialog(view, "End " + r.studentName() + "'s session on " + r.pcId()
                        + "?\nTheir PC will return to the login screen within 10 seconds.",
                "End session", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        endSession(view, r.sessionId(), refresh);
    }

    static void endSession(JComponent parent, int sessionId, Runnable after) {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception { new SessionDao().end(sessionId, "ADMIN"); return null; }
            @Override protected void done() {
                try { get(); after.run(); }
                catch (Exception e) { AdminUI.error(parent, Database.explain(e)); }
            }
        }.execute();
    }

    private void exportCsv() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("lab-sessions-" + java.time.LocalDate.now() + ".csv"));
        if (fc.showSaveDialog(view) != JFileChooser.APPROVE_OPTION) return;
        try (PrintWriter w = new PrintWriter(fc.getSelectedFile(), StandardCharsets.UTF_8)) {
            w.println("Student,Student No,PC,Time In,Time Out,Duration (min),Monitor OK,Keyboard OK,Mouse OK,End");
            for (SessionRow r : table.rows()) {
                w.printf("\"%s\",%s,%s,%s,%s,%d,%s,%s,%s,%s%n", r.studentName().replace("\"", "\"\""), r.studentNo(), r.pcId(),
                        r.timeIn(), r.timeOut() == null ? "" : r.timeOut(), r.seconds() / 60,
                        yn(r.monitorOk()), yn(r.keyboardOk()), yn(r.mouseOk()), r.active() ? "ACTIVE" : r.endReason());
            }
            AdminUI.info(view, "Export CSV", "Saved " + table.rows().size() + " sessions to\n" + fc.getSelectedFile());
        } catch (Exception e) {
            AdminUI.error(view, "Couldn't save the file: " + e.getMessage());
        }
    }

    private static String yn(Boolean b) { return b == null ? "" : (b ? "Yes" : "No"); }
}
