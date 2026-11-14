package ph.ue.comlab.student;

import ph.ue.comlab.AppConfig;
import ph.ue.comlab.db.*;
import ph.ue.comlab.model.Models.LabSession;
import ph.ue.comlab.model.Models.Report;
import ph.ue.comlab.model.Models.Student;
import ph.ue.comlab.ui.ConcernForm;
import ph.ue.comlab.ui.LoginPanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The app that runs on the STUDENT laptop.
 *
 * Flow (matches the system flowchart):
 *   Windows starts -> this app starts -> fullscreen login (locked)
 *   -> valid login? -> save time_in -> Workstation Check (Monitor / Keyboard / Mouse + Other concerns)
 *   -> "Start session": reports go to the admin, screen unlocks, floating session bar appears
 *   -> "Log out": save time_out -> back to the locked login for the next student
 */
public class StudentApp {

    /** Exit code the start-student.bat loop uses to know the exit was intentional (don't relaunch). */
    public static final int EXIT_CODE_ADMIN = 99;

    private final StudentDao students = new StudentDao();
    private final SessionDao sessions = new SessionDao();
    private final ReportDao reports = new ReportDao();
    private final PcDao pcs = new PcDao();

    private final String pcId = AppConfig.pcId();
    private final String pcLocation = AppConfig.pcLocation();

    private final JFrame frame = new JFrame("UE CompLab");
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final LoginPanel login = new LoginPanel(LoginPanel.Copy.student());
    private final KioskGuard guard;
    private final ScheduledExecutorService background = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "heartbeat");
        t.setDaemon(true);
        return t;
    });

    // current session state (EDT only)
    private Student student;
    private LabSession session;
    private WorkstationCheckPanel check;
    private SessionBar bar;
    private long sessionLoadedAt;
    private volatile Integer activeSessionId;
    private volatile Integer pendingLogoutId;   // logout that failed because the network was down

    public StudentApp() {
        guard = new KioskGuard(frame, AppConfig.getBool("kiosk.lock", true), this::secretExit);
        root.add(login, "login");
        frame.setContentPane(root);
        login.onLogin(this::doLogin);
    }

    public void start() {
        cards.show(root, "login");
        guard.engage();
        login.focusFirst();
        background.scheduleWithFixedDelay(this::heartbeat, 0, 10, TimeUnit.SECONDS);
    }

    // =====================================================================
    //  Heartbeat (every 10 s, background thread)
    // =====================================================================
    private void heartbeat() {
        try {
            pcs.heartbeat(pcId, pcLocation);
            Integer pending = pendingLogoutId;
            if (pending != null) { sessions.end(pending, "LOGOUT"); pendingLogoutId = null; }
            Integer id = activeSessionId;
            if (id != null && sessions.isClosed(id)) {
                SwingUtilities.invokeLater(() -> {
                    if (id.equals(activeSessionId)) {
                        lockForNextStudent();
                        JOptionPane.showMessageDialog(frame, "Your session was ended by the lab administrator.",
                                "Session ended", JOptionPane.INFORMATION_MESSAGE);
                    }
                });
            }
            SwingUtilities.invokeLater(() -> login.setServerProblem(null));
        } catch (Exception e) {
            String why = Database.explain(e);
            SwingUtilities.invokeLater(() -> login.setServerProblem(why));
        }
    }

    // =====================================================================
    //  Login
    // =====================================================================
    private void doLogin(String id, String password) {
        new SwingWorker<Object[], Void>() {
            @Override protected Object[] doInBackground() throws Exception {
                Optional<Student> s = students.login(id, password);
                if (s.isEmpty()) return new Object[]{"Incorrect student ID or password."};
                Optional<String> other = sessions.activeElsewhere(id, pcId);
                if (other.isPresent())
                    return new Object[]{"You're still logged in on " + other.get() + ". Please log out there first."};
                pcs.heartbeat(pcId, pcLocation);
                LabSession ls = sessions.start(id, pcId);
                List<Report> recent = reports.recentForPc(pcId, 3);
                return new Object[]{null, s.get(), ls, recent};
            }
            @SuppressWarnings("unchecked")
            @Override protected void done() {
                try {
                    Object[] r = get();
                    if (r[0] != null) { login.showError((String) r[0]); return; }
                    student = (Student) r[1];
                    session = (LabSession) r[2];
                    sessionLoadedAt = System.currentTimeMillis();
                    activeSessionId = session.sessionId();
                    showCheck((List<Report>) r[3]);
                } catch (Exception e) {
                    login.showError(Database.explain(rootCause(e)));
                }
            }
        }.execute();
    }

    private void showCheck(List<Report> recent) {
        if (check != null) { check.stopTimer(); root.remove(check); }
        check = new WorkstationCheckPanel(student, session, recent);
        check.setListener(new WorkstationCheckPanel.Listener() {
            @Override public void submitConcern(ConcernForm.Concern c) { sendConcern(c); }
            @Override public void startSession(boolean m, boolean k, boolean mouse, ConcernForm.Concern c) { beginUse(m, k, mouse, c); }
            @Override public void logout() { confirmLogout(); }
        });
        root.add(check, "check");
        cards.show(root, "check");
        root.revalidate();
    }

    // =====================================================================
    //  Workstation check
    // =====================================================================
    private void sendConcern(ConcernForm.Concern c) {
        new SwingWorker<List<Report>, Void>() {
            @Override protected List<Report> doInBackground() throws Exception {
                saveConcern(c);
                return reports.recentForPc(pcId, 3);
            }
            @Override protected void done() {
                try { check.concernSent(get()); }
                catch (Exception e) { check.showError("Couldn't send the report. " + Database.explain(rootCause(e))); }
            }
        }.execute();
    }

    private void saveConcern(ConcernForm.Concern c) throws Exception {
        reports.create(session.sessionId(), pcId, student.studentNo(), c.category(), c.priority(),
                c.description(), "STUDENT", c.photo(), c.photoName());
    }

    private void beginUse(boolean monitorOk, boolean keyboardOk, boolean mouseOk, ConcernForm.Concern concern) {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                sessions.saveEquipmentCheck(session.sessionId(), monitorOk, keyboardOk, mouseOk);
                if (!monitorOk) precheckReport("Hardware", "Pre-check: Monitor not working");
                if (!keyboardOk) precheckReport("Peripheral", "Pre-check: Keyboard not working");
                if (!mouseOk) precheckReport("Peripheral", "Pre-check: Mouse not working");
                if (concern != null) saveConcern(concern);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    unlock();
                } catch (Exception e) {
                    check.showError("Couldn't save your check. " + Database.explain(rootCause(e)));
                }
            }
        }.execute();
    }

    private void precheckReport(String category, String text) throws Exception {
        reports.create(session.sessionId(), pcId, student.studentNo(), category, "Medium", text, "PRECHECK", null, null);
    }

    /** Student may now use the PC: hide the lock screen, show the floating session bar. */
    private void unlock() {
        long elapsed = session.elapsedSecondsAtLoad()
                + (System.currentTimeMillis() - sessionLoadedAt) / 1000;
        check.stopTimer();
        guard.release();
        bar = new SessionBar(student, elapsed, this::reportDuringSession, this::confirmLogout);
        bar.open();
    }

    private void reportDuringSession() {
        ReportDialog[] dlg = new ReportDialog[1];
        dlg[0] = new ReportDialog(c -> new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception { saveConcern(c); return null; }
            @Override protected void done() {
                try {
                    get();
                    dlg[0].dispose();
                    JOptionPane.showMessageDialog(null, "Report sent. Thank you!", "Report an issue",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    dlg[0].failed("Couldn't send: " + Database.explain(rootCause(e)));
                }
            }
        }.execute());
        dlg[0].setVisible(true);
    }

    // =====================================================================
    //  Logout
    // =====================================================================
    private void confirmLogout() {
        Component parent = guard.isEngaged() ? frame : bar;
        int ans = JOptionPane.showConfirmDialog(parent, "Log out and end your lab session?", "Log out",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (ans != JOptionPane.OK_OPTION) return;
        int id = session.sessionId();
        lockForNextStudent();
        background.execute(() -> {
            try { sessions.end(id, "LOGOUT"); }
            catch (Exception e) { pendingLogoutId = id; }    // retried by the heartbeat
        });
    }

    /** Back to the locked login screen. */
    private void lockForNextStudent() {
        activeSessionId = null;
        student = null;
        session = null;
        if (bar != null) { bar.close(); bar = null; }
        if (check != null) { check.stopTimer(); root.remove(check); check = null; }
        login.reset();
        cards.show(root, "login");
        guard.engage();
        login.focusFirst();
    }

    // =====================================================================
    //  Secret exit for the lab admin / for you during the demo
    // =====================================================================
    private void secretExit() {
        Component parent = guard.isEngaged() ? frame : bar;
        String pw = ph.ue.comlab.ui.UI.askPassword(parent, "Close kiosk", "Admin password to close the lab app:");
        if (pw == null) return;
        if (!pw.equals(AppConfig.get("kiosk.exitPassword", "letmeout"))) {
            JOptionPane.showMessageDialog(parent, "Wrong password.", "Close kiosk", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Integer id = activeSessionId;
        try {
            if (id != null) sessions.end(id, "ADMIN");
            pcs.markOffline(pcId);
        } catch (Exception ignored) {}
        System.exit(EXIT_CODE_ADMIN);
    }

    static Throwable rootCause(Throwable t) {
        while (t.getCause() != null && t.getCause() != t) t = t.getCause();
        return t;
    }
    static Exception rootCause(Exception e) {
        Throwable t = rootCause((Throwable) e);
        return t instanceof Exception ex ? ex : e;
    }
}
