package ph.ue.comlab.student;

import ph.ue.comlab.AppConfig;
import ph.ue.comlab.model.Models;
import ph.ue.comlab.model.Models.Student;
import ph.ue.comlab.ui.*;
import ph.ue.comlab.ui.Theme.W;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;

/**
 * Small floating bar shown while the student is using the PC (bottom-right, always on top).
 * Shows the running session time and has "Report issue" and "Log out".
 * Drag it anywhere if it's covering something.
 */
public class SessionBar extends JFrame {

    private final JLabel timer = UI.label("00:00:00", Theme.inter(W.SEMIBOLD, 14), Theme.TEXT);
    private final Timer ticker;
    private final long startedAt = System.currentTimeMillis();
    private final long elapsedAtStart;

    public SessionBar(Student s, long elapsedSeconds, Runnable onReport, Runnable onLogout) {
        this.elapsedAtStart = elapsedSeconds;
        setUndecorated(true);
        setType(Window.Type.UTILITY);          // no taskbar button
        setAlwaysOnTop(true);
        setFocusableWindowState(false);        // never steals focus from what the student is doing
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        try { setBackground(new Color(0, 0, 0, 0)); } catch (Exception ignored) {}

        JComponent dot = new JComponent() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0;
                Theme.hq(g);
                g.setColor(Theme.SUCCESS_DOT);
                g.fill(new Ellipse2D.Double(0, 0, 8, 8));
            }
        };
        dot.setPreferredSize(new Dimension(8, 8));

        JPanel info = Flex.column(0).build(
                Flex.row(8).align(Flex.Align.CENTER).build(dot, timer),
                UI.label(AppConfig.pcId() + " · " + s.fullName(), Theme.inter(W.REGULAR, 12), Theme.MUTED));

        UI.SecondaryButton report = new UI.SecondaryButton("Report issue", "alert", 13, 12);
        report.addActionListener(e -> onReport.run());
        UI.SecondaryButton logout = new UI.SecondaryButton("Log out", "logout", 13, 12);
        logout.addActionListener(e -> onLogout.run());

        UI.Card card = new UI.Card(Theme.PAGE, 16, Theme.BORDER);
        Flex.row(14).align(Flex.Align.CENTER).pad(10, 16, 10, 12).into(card,
                UI.seal(28, Theme.BRAND_RED, Color.WHITE, 10), info, UI.vDivider(28), report, logout);

        JPanel root = new JPanel(new BorderLayout());
        root.setOpaque(false);
        root.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        root.add(card);
        setContentPane(root);
        pack();
        dockBottomRight();
        enableDrag(card);

        ticker = new Timer(1000, e -> tick());
        tick();
    }

    private void tick() {
        timer.setText(Models.hms(elapsedAtStart + (System.currentTimeMillis() - startedAt) / 1000));
    }

    private void dockBottomRight() {
        Rectangle usable = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setLocation(usable.x + usable.width - getWidth() - 16, usable.y + usable.height - getHeight() - 16);
    }

    private void enableDrag(JComponent c) {
        MouseAdapter drag = new MouseAdapter() {
            Point start;
            @Override public void mousePressed(MouseEvent e) { start = e.getPoint(); }
            @Override public void mouseDragged(MouseEvent e) {
                Point p = getLocation();
                setLocation(p.x + e.getX() - start.x, p.y + e.getY() - start.y);
            }
        };
        c.addMouseListener(drag);
        c.addMouseMotionListener(drag);
        c.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
    }

    public void open() { ticker.start(); setVisible(true); }

    public void close() { ticker.stop(); dispose(); }
}
