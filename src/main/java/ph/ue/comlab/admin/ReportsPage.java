package ph.ue.comlab.admin;

import ph.ue.comlab.db.Database;
import ph.ue.comlab.db.ReportDao;
import ph.ue.comlab.model.Models;
import ph.ue.comlab.model.Models.Report;
import ph.ue.comlab.student.WorkstationCheckPanel;
import ph.ue.comlab.ui.*;
import ph.ue.comlab.ui.DataTable.Column;
import ph.ue.comlab.ui.Theme.W;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;

/** Admin "Reports": every PC problem, with a detail panel to mark it in progress / resolved. */
class ReportsPage implements AdminUI.Page<List<Report>> {

    private final ReportDao dao = new ReportDao();
    private final DataTable<Report> table;
    private final JComponent view;
    private final Runnable refresh;
    private final UI.Card detail = Flex.column(16).align(Flex.Align.STRETCH).pad(20)
            .into(new UI.Card(Theme.PAGE, 16, Theme.BORDER));
    private volatile String search = "";
    private volatile String status = "OPEN";
    private Integer shownId;
    private String shownStatus;

    ReportsPage(Runnable refresh) {
        this.refresh = refresh;
        table = new DataTable<>(List.of(
                Column.text("Problem", 230, Report::title),
                Column.text("PC", 72, Report::pcId),
                Column.text("Category", 100, Report::category),
                Column.pill("Priority", 100, r -> AdminUI.priorityPill(r.priority())),
                Column.text("Reported", 135, r -> r.createdAt().format(Models.DATE_TIME)),
                Column.pill("Status", 115, r -> WorkstationCheckPanel.statusPill(r.status()))
        ), Report::reportId, "No reports here. Nice!");
        table.onSelect(this::showDetail);

        JPanel toolbar = Flex.row(12).align(Flex.Align.CENTER).build(
                AdminUI.search("Search PC, student or problem", 320, q -> { search = q; refresh.run(); }),
                AdminUI.filter(new String[]{"Open", "Pending", "In progress", "Resolved", "All"}, 0, 470, f -> {
                    status = switch (f) {
                        case "Open" -> "OPEN";
                        case "Pending" -> "PENDING";
                        case "In progress" -> "IN_PROGRESS";
                        case "Resolved" -> "RESOLVED";
                        default -> null;
                    };
                    refresh.run();
                }));

        showDetail(null);
        JPanel side = UI.fixedColumn(340, 0, detail);

        JPanel body = Flex.row(24).align(Flex.Align.START).build(Flex.grow(Flex.fill(table)), side);

        JPanel page = Flex.column(20).align(Flex.Align.STRETCH).pad(32, 40, 32, 40).build(
                AdminUI.header("PC ISSUE REPORTS", "Reported problems", "From the workstation pre-check and from students during their session.", null),
                toolbar, Flex.grow(body));
        page.setOpaque(true);
        page.setBackground(Theme.CANVAS);
        view = page;
    }

    @Override public JComponent view() { return view; }

    @Override public List<Report> load() throws Exception { return dao.list(status, search); }

    @Override public void show(List<Report> rows) {
        table.setRows(rows);
        Report sel = table.selected();
        // refresh the detail only if something about the selected report changed
        if (sel == null) { if (shownId != null) showDetail(null); }
        else if (shownId == null || sel.reportId() != shownId || !sel.status().equals(shownStatus)) showDetail(sel);
    }

    private void showDetail(Report r) {
        detail.removeAll();
        shownId = r == null ? null : r.reportId();
        shownStatus = r == null ? null : r.status();
        if (r == null) {
            detail.add(UI.label("Report details", Theme.poppins(W.SEMIBOLD, 16), Theme.TEXT));
            detail.add(new UI.WrapText("Select a report on the left to see the details and update its status.",
                    Theme.inter(W.REGULAR, 14), Theme.MUTED, 1.5f, 320));
        } else {
            detail.add(Flex.row(8).align(Flex.Align.CENTER).build(
                    WorkstationCheckPanel.statusPill(r.status()), AdminUI.priorityPill(r.priority()),
                    "PRECHECK".equals(r.source()) ? UI.status("Pre-check", Theme.TEXT_2, Theme.CANVAS, null) : null));
            detail.add(new UI.WrapText(r.title(), Theme.poppins(W.SEMIBOLD, 18), Theme.TEXT, 1.3f, 320));
            detail.add(Flex.column(10).align(Flex.Align.STRETCH).build(
                    kv("Workstation", r.pcId()),
                    kv("Category", r.category()),
                    kv("Reported by", r.studentName() == null ? "—" : r.studentName()),
                    kv("Student no.", r.studentNo() == null ? "—" : r.studentNo()),
                    kv("When", r.createdAt().format(Models.DATE_TIME))));
            if (r.hasPhoto()) detail.add(photo(r.reportId()));

            UI.SecondaryButton progress = new UI.SecondaryButton("Mark in progress", "clock", 14, 14);
            UI.PrimaryButton resolve = new UI.PrimaryButton("Mark resolved", "check").compact();
            progress.setEnabled(!"IN_PROGRESS".equals(r.status()) && !"RESOLVED".equals(r.status()));
            resolve.setEnabled(!"RESOLVED".equals(r.status()));
            progress.addActionListener(e -> setStatus(r, "IN_PROGRESS"));
            resolve.addActionListener(e -> setStatus(r, "RESOLVED"));
            JPanel actions = Flex.column(10).align(Flex.Align.STRETCH).build(resolve, progress);
            if ("RESOLVED".equals(r.status())) {
                UI.SecondaryButton reopen = new UI.SecondaryButton("Reopen", "refresh", 14, 14);
                reopen.addActionListener(e -> setStatus(r, "PENDING"));
                actions.add(reopen);
            }
            detail.add(actions);
        }
        detail.revalidate();
        detail.repaint();
    }

    private static JPanel kv(String k, String v) {
        return Flex.row(12).justify(Flex.Justify.BETWEEN).align(Flex.Align.CENTER).build(
                UI.label(k, Theme.inter(W.REGULAR, 14), Theme.TEXT_2),
                UI.label(v, Theme.inter(W.MEDIUM, 14), Theme.TEXT));
    }

    /** Loads the attached photo in the background and shows a thumbnail. Click to open it larger. */
    private JComponent photo(int reportId) {
        JLabel holder = UI.label("Loading photo…", Theme.inter(W.REGULAR, 13), Theme.MUTED);
        holder.setHorizontalAlignment(SwingConstants.CENTER);
        UI.Card box = new UI.Card(Theme.CANVAS, 12, null);
        box.setLayout(new BorderLayout());
        box.add(holder);
        box.setPreferredSize(new Dimension(300, 180));
        new SwingWorker<BufferedImage, Void>() {
            @Override protected BufferedImage doInBackground() throws Exception {
                byte[] b = dao.photo(reportId);
                return b == null ? null : ImageIO.read(new ByteArrayInputStream(b));
            }
            @Override protected void done() {
                try {
                    BufferedImage img = get();
                    if (img == null) { holder.setText("Photo can't be shown"); return; }
                    double s = Math.min(298.0 / img.getWidth(), 178.0 / img.getHeight());
                    holder.setText(null);
                    holder.setIcon(new ImageIcon(img.getScaledInstance((int) (img.getWidth() * s), (int) (img.getHeight() * s), Image.SCALE_SMOOTH)));
                    holder.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    holder.setToolTipText("Click to view full size");
                    holder.addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                            Dimension scr = Toolkit.getDefaultToolkit().getScreenSize();
                            double f = Math.min(1.0, Math.min(scr.width * 0.8 / img.getWidth(), scr.height * 0.8 / img.getHeight()));
                            JOptionPane.showMessageDialog(view, new JLabel(new ImageIcon(img.getScaledInstance(
                                    (int) (img.getWidth() * f), (int) (img.getHeight() * f), Image.SCALE_SMOOTH))),
                                    "Attached photo", JOptionPane.PLAIN_MESSAGE);
                        }
                    });
                } catch (Exception e) {
                    holder.setText("Couldn't load photo");
                }
            }
        }.execute();
        return box;
    }

    private void setStatus(Report r, String newStatus) {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception { dao.setStatus(r.reportId(), newStatus); return null; }
            @Override protected void done() {
                try { get(); refresh.run(); }
                catch (Exception e) { AdminUI.error(view, Database.explain(e)); }
            }
        }.execute();
    }
}
