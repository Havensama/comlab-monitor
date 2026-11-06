package ph.ue.comlab.student;

import ph.ue.comlab.AppConfig;
import ph.ue.comlab.model.Models;
import ph.ue.comlab.model.Models.LabSession;
import ph.ue.comlab.model.Models.Report;
import ph.ue.comlab.model.Models.Student;
import ph.ue.comlab.ui.*;
import ph.ue.comlab.ui.Theme.W;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/** Figma frame "Workstation Check – Desktop" (node 9:88). Shown right after a student logs in. */
public class WorkstationCheckPanel extends JPanel {

    public interface Listener {
        void submitConcern(ConcernForm.Concern concern);
        void startSession(boolean monitorOk, boolean keyboardOk, boolean mouseOk, ConcernForm.Concern concernOrNull);
        void logout();
    }

    private final EquipmentCard monitor = new EquipmentCard("monitor", "Monitor",
            "Turns on, clear display, no flicker or dead pixels");
    private final EquipmentCard keyboard = new EquipmentCard("keyboard", "Keyboard",
            "All keys respond, no sticky or missing keys");
    private final EquipmentCard mouse = new EquipmentCard("mouse", "Mouse",
            "Cursor moves smoothly, both clicks and scroll work");
    private final JLabel progressText = UI.label("", Theme.inter(W.SEMIBOLD, 13), Theme.TEXT_2);
    private final Progress progress = new Progress();
    private final ConcernForm concern = new ConcernForm("Other concerns", true,
            "Something else not working? Tell us and a lab assistant will check it.");
    private final UI.Banner banner = new UI.Banner();
    private final UI.Pill timerChip = TopNav.chip("Session  00:00:00", null).dot(Theme.SUCCESS_DOT);
    private final JPanel reportsList = Flex.column(14).align(Flex.Align.STRETCH).build();
    private final UI.PrimaryButton startBtn = new UI.PrimaryButton("Start session", "arrow-right");
    private final UI.SecondaryButton submitBtn = new UI.SecondaryButton("Submit report", "send");
    private final Timer ticker;
    private final long startedAtMillis;
    private final long elapsedAtLoad;
    private Listener listener;

    public WorkstationCheckPanel(Student student, LabSession session, List<Report> recent) {
        super(new BorderLayout());
        setBackground(Theme.CANVAS);
        this.elapsedAtLoad = session.elapsedSecondsAtLoad();
        this.startedAtMillis = System.currentTimeMillis();
        String pc = AppConfig.pcId();
        String loc = AppConfig.pcLocation();
        String shortLoc = loc.contains("·") ? loc.substring(0, loc.indexOf('·')).trim() : loc;

        // ---------------- top nav ----------------
        JPanel right = Flex.row(12).align(Flex.Align.CENTER).build(
                TopNav.chip(pc + " · " + shortLoc, "monitor"),
                timerChip,
                UI.vDivider(28),
                TopNav.user(student.initials(), student.fullName(), student.studentNo()),
                UI.iconButton("logout", 18, Theme.TEXT_2, "Log out", () -> { if (listener != null) listener.logout(); }));
        add(new TopNav("Computer Laboratory", "College of Engineering", right), BorderLayout.NORTH);

        // ---------------- check column ----------------
        JPanel titles = Flex.column(6).build(
                UI.label("WORKSTATION CHECK", Theme.tracking(Theme.inter(W.SEMIBOLD, 12), 0.96f), Theme.ORANGE),
                UI.label("Is everything working on your PC?", Theme.tracking(Theme.poppins(W.SEMIBOLD, 28), -0.28f), Theme.TEXT),
                UI.label("Tick each item that works. It takes 10 seconds and helps the lab team fix problems faster.",
                        Theme.inter(W.REGULAR, 15), Theme.TEXT_2));
        JPanel progressBox = Flex.column(8).align(Flex.Align.END).build(progressText, progress);
        JPanel header = Flex.row(16).justify(Flex.Justify.BETWEEN).align(Flex.Align.END).build(titles, progressBox);

        JPanel equipment = Flex.row(16).align(Flex.Align.STRETCH).build(
                Flex.grow(monitor), Flex.grow(keyboard), Flex.grow(mouse));
        for (EquipmentCard c : new EquipmentCard[]{monitor, keyboard, mouse}) c.onChange(this::updateProgress);
        updateProgress();

        submitBtn.addActionListener(e -> onSubmit());
        startBtn.addActionListener(e -> onStart());
        JPanel actions = Flex.row(12).justify(Flex.Justify.BETWEEN).align(Flex.Align.CENTER).build(
                UI.label("You can also report issues anytime during your session.", Theme.inter(W.REGULAR, 13), Theme.MUTED),
                Flex.row(12).align(Flex.Align.START).build(submitBtn, startBtn));

        JPanel checkColumn = Flex.column(24).align(Flex.Align.STRETCH).build(header, banner, equipment, concern, actions);

        // ---------------- side panel ----------------
        JPanel side = UI.fixedColumn(360, 16,
                sessionCard(pc, loc, session),
                recentCard(pc),
                helpTip());
        setRecentReports(recent);

        UI.ScrollPage page = new UI.ScrollPage(Theme.CANVAS);
        Flex.row(24).align(Flex.Align.START).pad(32, 40, 32, 40).into(page, Flex.grow(checkColumn), side);
        add(UI.scroll(page), BorderLayout.CENTER);

        ticker = new Timer(1000, e -> tick());
        ticker.start();
        tick();
    }

    public void setListener(Listener l) { listener = l; }

    private long elapsedSeconds() { return elapsedAtLoad + (System.currentTimeMillis() - startedAtMillis) / 1000; }

    private void tick() { timerChip.set("Session  " + Models.hms(elapsedSeconds()), Theme.TEXT_2, Theme.CANVAS, Theme.SUCCESS_DOT); }

    public void stopTimer() { ticker.stop(); }

    private void updateProgress() {
        int n = (monitor.isChecked() ? 1 : 0) + (keyboard.isChecked() ? 1 : 0) + (mouse.isChecked() ? 1 : 0);
        progressText.setText(n + " of 3 checked");
        progress.setValue(n / 3f);
    }

    // ---------------- actions ----------------
    private void onSubmit() {
        String problem = concern.validateInput();
        if (problem != null) { banner.error(problem); return; }
        banner.clear();
        setBusy(true);
        if (listener != null) listener.submitConcern(concern.value());
    }

    private void onStart() {
        ConcernForm.Concern c = null;
        if (!concern.isBlank()) {
            String problem = concern.validateInput();
            if (problem != null) { banner.error(problem + " (or clear the Other concerns form)"); return; }
            c = concern.value();
        }
        int unchecked = (monitor.isChecked() ? 0 : 1) + (keyboard.isChecked() ? 0 : 1) + (mouse.isChecked() ? 0 : 1);
        if (unchecked > 0) {
            int ans = JOptionPane.showConfirmDialog(this,
                    unchecked + " item(s) are not ticked. They will be reported to the lab admin as not working.\nContinue?",
                    "Start session", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (ans != JOptionPane.OK_OPTION) return;
        }
        banner.clear();
        setBusy(true);
        if (listener != null) listener.startSession(monitor.isChecked(), keyboard.isChecked(), mouse.isChecked(), c);
    }

    public void setBusy(boolean busy) {
        startBtn.setEnabled(!busy);
        submitBtn.setEnabled(!busy);
        setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : null);
    }

    public void concernSent(List<Report> recent) {
        setBusy(false);
        concern.clear();
        banner.success("Report sent. A lab assistant will check it soon — thank you!");
        setRecentReports(recent);
    }

    public void showError(String msg) {
        setBusy(false);
        banner.error(msg);
    }

    // ---------------- side cards ----------------
    private JComponent sessionCard(String pc, String loc, LabSession s) {
        UI.Card card = new UI.Card(Theme.PAGE, 16, Theme.BORDER);
        Flex.column(16).align(Flex.Align.STRETCH).pad(20).into(card,
                UI.label("Your session", Theme.poppins(W.SEMIBOLD, 16), Theme.TEXT),
                kv("Workstation", pc),
                kv("Location", loc),
                kv("Logged in", s.timeIn() == null ? "—" : s.timeIn().format(Models.TIME)),
                Flex.row(0).justify(Flex.Justify.BETWEEN).align(Flex.Align.CENTER).build(
                        UI.label("Status", Theme.inter(W.REGULAR, 14), Theme.TEXT_2),
                        UI.status("Pre-check pending", Theme.WARNING, Theme.WARNING_SOFT, Theme.WARNING_DOT)));
        return card;
    }

    static JPanel kv(String k, String v) {
        return Flex.row(12).justify(Flex.Justify.BETWEEN).align(Flex.Align.CENTER).build(
                UI.label(k, Theme.inter(W.REGULAR, 14), Theme.TEXT_2),
                UI.label(v, Theme.inter(W.MEDIUM, 14), Theme.TEXT));
    }

    private JComponent recentCard(String pc) {
        UI.Card card = new UI.Card(Theme.PAGE, 16, Theme.BORDER);
        Flex.column(14).align(Flex.Align.STRETCH).pad(20).into(card,
                UI.label("Recent reports on " + pc, Theme.poppins(W.SEMIBOLD, 16), Theme.TEXT),
                reportsList);
        return card;
    }

    public void setRecentReports(List<Report> recent) {
        reportsList.removeAll();
        if (recent == null || recent.isEmpty()) {
            reportsList.add(new UI.WrapText("No reports yet — this PC looks healthy.", Theme.inter(W.REGULAR, 13),
                    Theme.MUTED, 1.4f, 320));
        } else {
            for (Report r : recent) reportsList.add(reportRow(r));
        }
        reportsList.revalidate();
        reportsList.repaint();
    }

    /** One row: icon tile, title + "Oct 16 · Hardware", status pill. Also used by the admin dashboard. */
    public static JPanel reportRow(Report r) {
        boolean done = "RESOLVED".equals(r.status());
        UI.Card tile = new UI.Card(done ? Theme.SUCCESS_SOFT : Theme.WARNING_SOFT, 10, null);
        tile.setLayout(new GridBagLayout());
        tile.add(new JLabel(Icons.get(done ? "check" : "alert", 18, done ? Theme.SUCCESS : Theme.WARNING)));
        tile.setPreferredSize(new Dimension(36, 36));
        String meta = (r.createdAt() == null ? "" : r.createdAt().format(Models.DATE) + " · ") + r.category();
        JPanel text = Flex.column(2).align(Flex.Align.STRETCH).build(
                new UI.WrapText(r.title(), Theme.inter(W.MEDIUM, 14), Theme.TEXT, 1.2f, 400),
                UI.label(meta, Theme.inter(W.REGULAR, 12), Theme.MUTED));
        return Flex.row(12).align(Flex.Align.CENTER).build(tile, Flex.grow(text), statusPill(r.status()));
    }

    public static UI.Pill statusPill(String status) {
        return switch (status == null ? "" : status) {
            case "RESOLVED" -> UI.status("Resolved", Theme.SUCCESS, Theme.SUCCESS_SOFT, null);
            case "IN_PROGRESS" -> UI.status("In progress", Theme.WARNING, Theme.WARNING_SOFT, null);
            default -> UI.status("Pending", Theme.ORANGE, Theme.ORANGE_SOFT, null);
        };
    }

    private JComponent helpTip() {
        UI.Card card = new UI.Card(Theme.ORANGE_SOFT, 16, null);
        Flex.row(12).align(Flex.Align.START).pad(16).into(card,
                new JLabel(Icons.get("clock", 20, Theme.ORANGE)),
                Flex.grow(new UI.WrapText("Remember to log out before leaving. Your session time in and time out are recorded for the lab log.",
                        Theme.inter(W.REGULAR, 13), Theme.TEXT_2, 1.5f, 296)));
        return card;
    }

    // =====================================================================
    //  Equipment card (Figma component "Equipment Check", State=Working / Unchecked)
    // =====================================================================
    static class EquipmentCard extends UI.Card {
        private final UI.Check check = new UI.Check(22, 6, false);
        private final UI.Pill status = UI.status("Not yet checked", Theme.TEXT_2, Theme.CANVAS, null);
        private Runnable onChange = () -> {};

        EquipmentCard(String icon, String name, String desc) {
            super(Theme.PAGE, 16, Theme.BORDER);
            UI.Card tile = new UI.Card(Theme.ORANGE_SOFT, 12, null);
            tile.setLayout(new GridBagLayout());
            tile.add(new JLabel(Icons.get(icon, 22, Theme.ORANGE, 1.8f)));
            tile.setPreferredSize(new Dimension(44, 44));

            JPanel top = Flex.row(0).justify(Flex.Justify.BETWEEN).align(Flex.Align.START).build(tile, check);
            JPanel text = Flex.column(4).align(Flex.Align.STRETCH).build(
                    Flex.hug(UI.label(name, Theme.poppins(W.SEMIBOLD, 18), Theme.TEXT)),
                    new UI.WrapText(desc, Theme.inter(W.REGULAR, 13), Theme.TEXT_2, 1.45f, 400));
            Flex.column(14).align(Flex.Align.STRETCH).pad(20).into(this, top, text, Flex.hug(status));

            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { check.toggle(); }
            });
            check.onChange(v -> refresh());
            refresh();
        }

        void onChange(Runnable r) { onChange = r; }
        boolean isChecked() { return check.isChecked(); }

        private void refresh() {
            boolean on = check.isChecked();
            setColors(Theme.PAGE, on ? Theme.ORANGE : Theme.BORDER, on ? 1.5f : 1f);
            if (on) status.set("Working", Theme.SUCCESS, Theme.SUCCESS_SOFT, Theme.SUCCESS_DOT);
            else status.set("Not yet checked", Theme.TEXT_2, Theme.CANVAS, null);
            revalidate();
            onChange.run();
        }
    }

    /** Thin orange progress bar (Figma "Track"). */
    static class Progress extends JComponent {
        private float value;
        Progress() { setPreferredSize(new Dimension(180, 6)); }
        void setValue(float v) { value = v; repaint(); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.hq(g);
            g.setColor(Theme.BORDER);
            g.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 6, 6));
            g.setColor(Theme.ORANGE);
            g.fill(new RoundRectangle2D.Double(0, 0, getWidth() * value, getHeight(), 6, 6));
            g.dispose();
        }
    }
}
