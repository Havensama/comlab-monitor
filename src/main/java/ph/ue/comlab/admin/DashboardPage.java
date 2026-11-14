package ph.ue.comlab.admin;

import ph.ue.comlab.db.PcDao;
import ph.ue.comlab.db.ReportDao;
import ph.ue.comlab.model.Models;
import ph.ue.comlab.model.Models.PcStatus;
import ph.ue.comlab.model.Models.Report;
import ph.ue.comlab.model.Models.Stats;
import ph.ue.comlab.student.WorkstationCheckPanel;
import ph.ue.comlab.ui.*;
import ph.ue.comlab.ui.Theme.W;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.List;
import java.util.function.Consumer;

/** Admin home: numbers at the top, a live map of every workstation, and the newest open reports. */
class DashboardPage implements AdminUI.Page<DashboardPage.Data> {

    record Data(Stats stats, List<PcStatus> pcs, List<Report> open) {}

    private final PcDao pcDao = new PcDao();
    private final ReportDao reportDao = new ReportDao();

    private final AdminUI.Stat online = new AdminUI.Stat("monitor", "PCs online", Theme.ORANGE, Theme.ORANGE_SOFT);
    private final AdminUI.Stat active = new AdminUI.Stat("users", "Students in the lab", Theme.SUCCESS, Theme.SUCCESS_SOFT);
    private final AdminUI.Stat open = new AdminUI.Stat("alert", "Open reports", Theme.WARNING, Theme.WARNING_SOFT);
    private final AdminUI.Stat today = new AdminUI.Stat("clock", "Sessions today", Theme.TEXT_2, Theme.CANVAS);
    private final JPanel grid = new JPanel(new GridLayout(0, 4, 12, 12));
    private final JPanel latest = Flex.column(14).align(Flex.Align.STRETCH).build();
    private final JComponent view;
    private final Consumer<PcStatus> onPcClick;

    DashboardPage(Runnable viewAllReports, Consumer<PcStatus> onPcClick) {
        this.onPcClick = onPcClick;
        grid.setOpaque(false);

        JPanel stats = Flex.row(16).align(Flex.Align.STRETCH).build(
                Flex.grow(online), Flex.grow(active), Flex.grow(open), Flex.grow(today));

        JPanel legend = Flex.row(8).align(Flex.Align.CENTER).build(
                UI.status("In use", Theme.ORANGE, Theme.ORANGE_SOFT, Theme.ORANGE),
                UI.status("Available", Theme.SUCCESS, Theme.SUCCESS_SOFT, Theme.SUCCESS_DOT),
                UI.status("Offline", Theme.TEXT_2, Theme.CANVAS, Theme.MUTED));
        UI.Card pcsCard = new UI.Card(Theme.PAGE, 16, Theme.BORDER);
        Flex.column(18).align(Flex.Align.STRETCH).pad(20).into(pcsCard,
                Flex.row(0).justify(Flex.Justify.BETWEEN).align(Flex.Align.CENTER).build(
                        Flex.column(2).build(
                                UI.label("Workstations", Theme.poppins(W.SEMIBOLD, 18), Theme.TEXT),
                                UI.label("Click a PC to see its session or end it.", Theme.inter(W.REGULAR, 13), Theme.MUTED)),
                        legend),
                grid);

        UI.Card latestCard = new UI.Card(Theme.PAGE, 16, Theme.BORDER);
        Flex.column(16).align(Flex.Align.STRETCH).pad(20).into(latestCard,
                Flex.row(0).justify(Flex.Justify.BETWEEN).align(Flex.Align.CENTER).build(
                        UI.label("Latest open reports", Theme.poppins(W.SEMIBOLD, 16), Theme.TEXT),
                        UI.link("View all", Theme.inter(W.SEMIBOLD, 13), Theme.ORANGE, viewAllReports)),
                latest);

        JPanel body = Flex.row(24).align(Flex.Align.START).build(Flex.grow(pcsCard), UI.fixedColumn(380, 16, latestCard));

        UI.ScrollPage page = new UI.ScrollPage(Theme.CANVAS);
        Flex.column(24).align(Flex.Align.STRETCH).pad(32, 40, 32, 40).into(page,
                AdminUI.header("LAB DASHBOARD", "Lab overview", "Live status of every workstation. Refreshes every 5 seconds.", null),
                stats, body);
        view = UI.scroll(page);
    }

    @Override public JComponent view() { return view; }

    @Override public Data load() throws Exception {
        return new Data(pcDao.stats(), pcDao.all(), reportDao.list("OPEN", ""));
    }

    @Override public void show(Data d) {
        Stats s = d.stats();
        online.set(s.pcsOnline() + " / " + s.pcsTotal(), s.pcsOnline() == 0 ? "No student app connected yet" : "Student app running", Theme.MUTED);
        active.set(String.valueOf(s.activeSessions()), s.activeSessions() == 1 ? "1 PC in use right now" : s.activeSessions() + " PCs in use right now", Theme.MUTED);
        open.set(String.valueOf(s.openReports()), s.highPriorityOpen() > 0 ? s.highPriorityOpen() + " high priority" : "None high priority",
                s.highPriorityOpen() > 0 ? Theme.DANGER : Theme.MUTED);
        today.set(String.valueOf(s.sessionsToday()), "Logins since midnight", Theme.MUTED);

        grid.removeAll();
        for (PcStatus pc : d.pcs()) grid.add(new PcTile(pc, onPcClick));
        grid.revalidate();
        grid.repaint();

        latest.removeAll();
        if (d.open().isEmpty()) latest.add(new UI.WrapText("No open reports. Every PC is healthy.", Theme.inter(W.REGULAR, 13), Theme.MUTED, 1.4f, 340));
        else d.open().stream().limit(6).forEach(r -> latest.add(WorkstationCheckPanel.reportRow(withPcInTitle(r))));
        latest.revalidate();
        latest.repaint();
    }

    private static Report withPcInTitle(Report r) {
        return new Report(r.reportId(), r.pcId(), r.studentNo(), r.studentName(), r.pcId() + " · " + r.category(),
                r.priority(), r.description(), r.status(), r.source(), r.createdAt(), r.hasPhoto());
    }

    /** One workstation on the map. */
    static class PcTile extends UI.Card {
        PcTile(PcStatus pc, Consumer<PcStatus> onClick) {
            super(Theme.PAGE, 12, Theme.BORDER);
            boolean inUse = pc.currentStudentNo() != null;
            Color dot;
            String state;
            if (inUse) { dot = Theme.ORANGE; state = "In use"; setColors(Theme.PAGE, Theme.ORANGE, 1.5f); }
            else if (pc.online()) { dot = Theme.SUCCESS_DOT; state = "Available"; }
            else { dot = Theme.MUTED; state = "Offline"; setColors(Theme.CANVAS, Theme.BORDER, 1f); }

            JComponent d = new JComponent() {
                @Override protected void paintComponent(Graphics g0) {
                    Graphics2D g = (Graphics2D) g0; Theme.hq(g);
                    g.setColor(dot); g.fill(new Ellipse2D.Double(0, 0, 8, 8));
                }
            };
            d.setPreferredSize(new Dimension(8, 8));
            JPanel top = Flex.row(0).justify(Flex.Justify.BETWEEN).align(Flex.Align.CENTER).build(
                    UI.label(pc.pcId(), Theme.poppins(W.SEMIBOLD, 15), pc.online() || inUse ? Theme.TEXT : Theme.MUTED), d);

            JPanel col = Flex.column(4).align(Flex.Align.STRETCH).build(top);
            if (inUse) {
                col.add(UI.label(pc.currentStudentName(), Theme.inter(W.MEDIUM, 13), Theme.TEXT));
                col.add(UI.label(Models.shortDuration(pc.sessionSeconds()) + " · since " + pc.sessionStart().format(Models.TIME),
                        Theme.inter(W.REGULAR, 12), Theme.MUTED));
            } else {
                col.add(UI.label(state, Theme.inter(W.MEDIUM, 13), pc.online() ? Theme.SUCCESS : Theme.MUTED));
                col.add(UI.label(pc.online() ? "Waiting for a student" : "App not running", Theme.inter(W.REGULAR, 12), Theme.MUTED));
            }
            if (pc.openReports() > 0) {
                col.add(Flex.space(2));
                col.add(Flex.hug(UI.status(pc.openReports() + (pc.openReports() == 1 ? " open issue" : " open issues"),
                        Theme.DANGER, Theme.DANGER_SOFT, null)));
            }
            Flex.column(0).align(Flex.Align.STRETCH).pad(14).into(this, col);
            setToolTipText(pc.pcId() + " · " + pc.location());
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { onClick.accept(pc); }
            });
        }
    }
}
