package ph.ue.comlab.admin;

import ph.ue.comlab.db.Database;
import ph.ue.comlab.db.StudentDao;
import ph.ue.comlab.model.Models;
import ph.ue.comlab.model.Models.Admin;
import ph.ue.comlab.model.Models.PcStatus;
import ph.ue.comlab.ui.*;
import ph.ue.comlab.ui.Theme.W;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The app that runs on the ADMIN laptop (the same laptop that runs MySQL).
 * Login -> Dashboard / Sessions / Reports / Students. The current page refreshes every 5 seconds.
 */
public class AdminApp {

    private static final int REFRESH_MS = 5000;

    private final JFrame frame = new JFrame("UE CompLab · Admin");
    private final CardLayout rootCards = new CardLayout();
    private final JPanel root = new JPanel(rootCards);
    private final LoginPanel login = new LoginPanel(LoginPanel.Copy.admin());

    private final CardLayout pageCards = new CardLayout();
    private final JPanel pages = new JPanel(pageCards);
    private final Map<String, AdminUI.Page<?>> pageMap = new LinkedHashMap<>();
    private final List<NavItem> nav = new ArrayList<>();
    private String current = "Dashboard";
    private final UI.Pill live = TopNav.chip("Connecting…", null).dot(Theme.MUTED);
    private final Timer refresher = new Timer(REFRESH_MS, e -> refresh());
    private long lastOk;
    private boolean loading;
    private JPanel shell;

    public AdminApp() {
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1200, 760));
        root.add(login, "login");
        frame.setContentPane(root);
        login.onLogin(this::doLogin);
        new Timer(1000, e -> updateLiveChip()).start();
    }

    public void start() {
        UI.sizeWindow(frame);
        frame.setVisible(true);
        login.focusFirst();
        // Starts XAMPP's MySQL and creates the database if needed, so opening this app is all it takes.
        new SwingWorker<String, String>() {
            @Override protected String doInBackground() {
                String problem = ph.ue.comlab.db.LocalServer.prepare(this::publish);
                if (problem == null && !Database.ping())
                    problem = "Can't reach the database at " + ph.ue.comlab.AppConfig.get("db.host", "localhost") + ".";
                return problem;
            }
            @Override protected void process(java.util.List<String> chunks) {
                login.setServerProblem(chunks.get(chunks.size() - 1));
            }
            @Override protected void done() {
                try { login.setServerProblem(get()); } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void doLogin(String user, String password) {
        new SwingWorker<Optional<Admin>, Void>() {
            @Override protected Optional<Admin> doInBackground() throws Exception { return new StudentDao().adminLogin(user, password); }
            @Override protected void done() {
                try {
                    Optional<Admin> a = get();
                    if (a.isEmpty()) { login.showError("Incorrect username or password."); return; }
                    login.setServerProblem(null);
                    openShell(a.get());
                } catch (Exception e) {
                    Throwable t = e.getCause() == null ? e : e.getCause();
                    login.showError(Database.explain(t instanceof Exception ex ? ex : e));
                }
            }
        }.execute();
    }

    // =====================================================================
    //  Shell: top nav + sidebar + pages
    // =====================================================================
    private void openShell(Admin admin) {
        buildShell(admin);
        rootCards.show(root, "shell");
        go("Dashboard");
        refresher.start();
    }

    /** Builds the console once (package-private so it can be previewed without a database). */
    JPanel buildShell(Admin admin) {
        if (shell == null) {
            pageMap.put("Dashboard", new DashboardPage(() -> go("Reports"), this::pcClicked));
            pageMap.put("Sessions", new SessionsPage(this::refresh));
            pageMap.put("Reports", new ReportsPage(this::refresh));
            pageMap.put("Students", new StudentsPage(this::refresh));
            pageMap.forEach((name, p) -> pages.add(p.view(), name));

            JPanel right = Flex.row(12).align(Flex.Align.CENTER).build(
                    live, UI.vDivider(28),
                    TopNav.user(initials(admin.fullName()), admin.fullName(), "Administrator"),
                    UI.iconButton("logout", 18, Theme.TEXT_2, "Log out", this::logout));

            shell = new JPanel(new BorderLayout());
            shell.add(new TopNav("Computer Laboratory", "Admin console · College of Engineering", right), BorderLayout.NORTH);
            shell.add(sidebar(), BorderLayout.WEST);
            shell.add(pages, BorderLayout.CENTER);
            root.add(shell, "shell");
        }
        return shell;
    }

    AdminUI.Page<?> page(String name) { return pageMap.get(name); }

    void showPage(String name) {
        current = name;
        pageCards.show(pages, name);
        nav.forEach(n -> n.setActive(n.name.equals(name)));
        lastOk = System.currentTimeMillis();
        updateLiveChip();
    }

    private JComponent sidebar() {
        JPanel side = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(Theme.PAGE);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Theme.BORDER);
                g.fillRect(getWidth() - 1, 0, 1, getHeight());
            }
            @Override public Dimension getPreferredSize() { return new Dimension(240, 100); }
        };
        String[][] items = {{"Dashboard", "grid"}, {"Sessions", "list"}, {"Reports", "alert"}, {"Students", "users"}};
        List<Component> kids = new ArrayList<>();
        kids.add(UI.label("MENU", Theme.tracking(Theme.inter(W.SEMIBOLD, 11), 0.8f), Theme.MUTED));
        for (String[] it : items) {
            NavItem n = new NavItem(it[0], it[1]);
            nav.add(n);
            kids.add(n);
        }
        kids.add(Flex.spring());
        UI.Card tip = new UI.Card(Theme.ORANGE_SOFT, 12, null);
        Flex.column(4).align(Flex.Align.STRETCH).pad(14).into(tip,
                UI.label("Demo tip", Theme.inter(W.SEMIBOLD, 13), Theme.ORANGE),
                new UI.WrapText("Log in on the student laptop and watch its tile turn violet here within 5 seconds.",
                        Theme.inter(W.REGULAR, 12), Theme.TEXT_2, 1.45f, 190));
        kids.add(tip);
        return Flex.column(6).align(Flex.Align.STRETCH).pad(28, 16, 20, 17).into(side, kids.toArray(Component[]::new));
    }

    private void go(String page) {
        current = page;
        pageCards.show(pages, page);
        nav.forEach(n -> n.setActive(n.name.equals(page)));
        refresh();
    }

    // =====================================================================
    //  Refresh (every 5 s, and right after any change)
    // =====================================================================
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void refresh() {
        if (loading || shell == null) return;
        AdminUI.Page page = pageMap.get(current);
        loading = true;
        new SwingWorker<Object, Void>() {
            @Override protected Object doInBackground() throws Exception { return page.load(); }
            @Override protected void done() {
                loading = false;
                try {
                    page.show(get());
                    lastOk = System.currentTimeMillis();
                } catch (Exception e) {
                    lastOk = -1;
                }
                updateLiveChip();
            }
        }.execute();
    }

    private void updateLiveChip() {
        if (lastOk == 0) return;
        if (lastOk < 0) {
            live.set("Database offline", Theme.DANGER, Theme.DANGER_SOFT, Theme.DANGER);
            return;
        }
        long s = (System.currentTimeMillis() - lastOk) / 1000;
        live.set("Live · updated " + (s < 2 ? "just now" : s + "s ago"), Theme.TEXT_2, Theme.CANVAS, Theme.SUCCESS_DOT);
    }

    private void pcClicked(PcStatus pc) {
        if (pc.currentStudentNo() == null) {
            String[] opts = {"See reports", "Close"};
            int c = JOptionPane.showOptionDialog(frame, pc.pcId() + " · " + pc.location() + "\n"
                            + (pc.online() ? "Online — waiting for a student." : "Offline — the student app is not running.")
                            + (pc.openReports() > 0 ? "\n" + pc.openReports() + " open issue(s)." : ""),
                    pc.pcId(), JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, opts, opts[1]);
            if (c == 0) go("Reports");
            return;
        }
        String[] opts = {"End session", "See reports", "Close"};
        int c = JOptionPane.showOptionDialog(frame,
                pc.currentStudentName() + " (" + pc.currentStudentNo() + ")\nLogged in at "
                        + pc.sessionStart().format(Models.TIME) + " · " + Models.hms(pc.sessionSeconds()),
                pc.pcId(), JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, opts, opts[2]);
        if (c == 0) {
            new SwingWorker<Integer, Void>() {
                @Override protected Integer doInBackground() throws Exception {
                    return new ph.ue.comlab.db.SessionDao().log(pc.currentStudentNo(), true, false).stream()
                            .filter(r -> r.pcId().equals(pc.pcId())).findFirst().map(Models.SessionRow::sessionId).orElse(null);
                }
                @Override protected void done() {
                    try {
                        Integer id = get();
                        if (id != null) SessionsPage.endSession((JComponent) frame.getContentPane(), id, AdminApp.this::refresh);
                    } catch (Exception e) { AdminUI.error(frame, Database.explain(e)); }
                }
            }.execute();
        } else if (c == 1) go("Reports");
    }

    private void logout() {
        refresher.stop();
        login.reset();
        rootCards.show(root, "login");
        login.focusFirst();
    }

    private static String initials(String name) { return new Models.Student("", name, "").initials(); }

    // =====================================================================
    //  Sidebar item
    // =====================================================================
    private class NavItem extends JComponent {
        final String name, icon;
        boolean active, hover;
        NavItem(String name, String icon) {
            this.name = name; this.icon = icon;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { go(name); }
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }
        void setActive(boolean a) { active = a; repaint(); }
        @Override public Dimension getPreferredSize() { return new Dimension(200, 44); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.hq(g);
            if (active || hover) {
                g.setColor(active ? Theme.ORANGE_SOFT : Theme.CANVAS);
                g.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));
            }
            Color c = active ? Theme.ORANGE : Theme.TEXT_2;
            Icons.paint(g, icon, 14, (getHeight() - 20) / 2.0, 20, c, 2f);
            g.setFont(active ? Theme.inter(W.SEMIBOLD, 14) : Theme.inter(W.MEDIUM, 14));
            g.setColor(active ? Theme.ORANGE : Theme.TEXT);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(name, 46, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }
    }
}
