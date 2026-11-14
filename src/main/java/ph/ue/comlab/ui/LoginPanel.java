package ph.ue.comlab.ui;

import ph.ue.comlab.AppConfig;
import ph.ue.comlab.ui.Theme.W;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.prefs.Preferences;

/**
 * Figma frame "Login – Desktop" (node 3:49).
 * Left: orange/red hero. Right: the login form. Used by both the student kiosk and the admin app.
 */
public class LoginPanel extends JPanel {

    /** All the wording that differs between the student and admin login. */
    public record Copy(String badge, String title, String subtitle, String idLabel, String idPlaceholder,
                       String idIcon, String headline, String body, String[] features, String note,
                       boolean rememberOption) {
        public static Copy student() {
            return new Copy("Student Portal", "Welcome back",
                    "Sign in with your student credentials to start your lab session.",
                    "Student ID", "e.g. 20231234567", "idcard",
                    "Computer\nLaboratory",
                    "Log in to access your workstation, track your lab sessions, and report PC issues — all in one place.",
                    new String[]{"Secure sign-in", "Session logging", "Issue reporting"},
                    "Using a lab PC? Your session start and end times are logged automatically for lab monitoring.",
                    true);
        }
        public static Copy admin() {
            return new Copy("Admin Console", "Lab admin sign in",
                    "Monitor workstations, review session logs, and resolve reported PC issues.",
                    "Username", "e.g. admin", "user",
                    "Computer\nLaboratory",
                    "See who is using every PC in real time, and fix problems before the next class walks in.",
                    new String[]{"Live PC status", "Session logs", "Issue tracking"},
                    "Keep this window open during lab hours — the dashboard refreshes every few seconds.",
                    false);
        }
    }

    public interface Handler { void login(String id, String password); }

    private static final Preferences PREFS = Preferences.userNodeForPackage(LoginPanel.class);

    private final UI.InputField idField, pwField;
    private final UI.PrimaryButton loginBtn;
    private final UI.Banner error = new UI.Banner();
    private final UI.Banner server = new UI.Banner();
    private final UI.Check remember;
    private final Copy copy;
    private JComponent note, formPanel, form;
    private String serverText, shownServerText;
    private Handler handler = (a, b) -> {};

    public LoginPanel(Copy copy) {
        this.copy = copy;
        setBackground(Theme.PAGE);

        // ---------------- form ----------------
        UI.TextInput idInput = new UI.TextInput(copy.idPlaceholder());
        UI.PasswordInput pwInput = new UI.PasswordInput("Enter your password");
        idField = new UI.InputField(copy.idLabel(), copy.idIcon(), idInput, null);
        pwField = new UI.InputField("Password", "lock", pwInput, UI.eyeToggle(pwInput));

        remember = new UI.Check(18, 5, true);
        JLabel rememberLbl = UI.label("Remember this PC", Theme.inter(W.REGULAR, 14), Theme.TEXT_2);
        rememberLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        rememberLbl.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { remember.toggle(); }
        });
        JLabel forgot = UI.link("Forgot password?", Theme.inter(W.SEMIBOLD, 14), Theme.ORANGE,
                () -> message("Forgot your password?",
                        "Please ask the lab assistant at the front desk to reset it for you."));
        JPanel options = Flex.row(0).justify(Flex.Justify.BETWEEN).align(Flex.Align.CENTER).build(
                copy.rememberOption() ? Flex.row(8).align(Flex.Align.CENTER).build(remember, rememberLbl) : Flex.space(1),
                forgot);

        loginBtn = new UI.PrimaryButton("Log in", "arrow-right");
        loginBtn.addActionListener(e -> submit());
        idInput.addActionListener(e -> pwInput.requestFocusInWindow());
        pwInput.addActionListener(e -> submit());

        UI.Card note = new UI.Card(Theme.ORANGE_SOFT, 12, null);
        this.note = note;
        Flex.row(12).align(Flex.Align.START).pad(14, 16, 14, 16).into(note,
                new JLabel(Icons.get("monitor", 20, Theme.ORANGE)),
                Flex.grow(new UI.WrapText(copy.note(), Theme.inter(W.REGULAR, 13), Theme.TEXT_2, 1.5f, 336)));

        JPanel header = Flex.column(12).align(Flex.Align.STRETCH).build(
                Flex.hug(new UI.Pill(copy.badge(), Theme.inter(W.SEMIBOLD, 12), Theme.ORANGE, Theme.ORANGE_SOFT, 6, 12)),
                Flex.hug(UI.label(copy.title(), Theme.tracking(Theme.poppins(W.SEMIBOLD, 36), -0.36f), Theme.TEXT)),
                Flex.fill(new UI.WrapText(copy.subtitle(), Theme.inter(W.REGULAR, 16), Theme.TEXT_2, 1.5f, 400)));

        JPanel fields = Flex.column(18).align(Flex.Align.STRETCH).build(idField, pwField, options);

        JPanel form = UI.fixedColumn(400, 28, server, header, fields, error, loginBtn, note);
        this.form = form;

        JLabel help = UI.label("Need help?", Theme.inter(W.MEDIUM, 14), Theme.TEXT_2, Icons.get("help", 20, Theme.TEXT_2), 6);
        help.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        help.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                message("Need help?", "Can't log in or something isn't working?\nPlease approach the lab assistant on duty.");
            }
        });

        JPanel footer = Flex.row(0).justify(Flex.Justify.BETWEEN).build(
                UI.label("© 2026 University of the East · College of Engineering", Theme.inter(W.REGULAR, 12), Theme.MUTED),
                UI.label("Privacy Policy  ·  Terms", Theme.inter(W.MEDIUM, 12), Theme.TEXT_2));

        JPanel formPanel = Flex.column(0).justify(Flex.Justify.BETWEEN).align(Flex.Align.STRETCH).pad(24, 48, 24, 48).build(
                Flex.row(0).justify(Flex.Justify.END).build(help),
                Flex.row(0).justify(Flex.Justify.CENTER).build(form),
                footer);
        this.formPanel = formPanel;

        Flex.row(0).align(Flex.Align.STRETCH).pad(16).into(this,
                Flex.grow(new Hero(copy)), Flex.grow(formPanel));

        if (copy.rememberOption()) {
            String last = PREFS.get("lastId", "");
            if (!last.isEmpty()) idInput.setText(last);
        }
    }

    /** Small screens (e.g. 1366x768 laptops): drop the lab note when the form wouldn't fit. */
    @Override public void doLayout() {
        int room = getHeight() - 32 - 48 - 20 - 15 - 24;
        Flex f = (Flex) form.getLayout();
        f.setGap(28);
        note.setVisible(true);
        if (f.sizeForWidth(form, 400).height > room) note.setVisible(false);
        if (f.sizeForWidth(form, 400).height > room) f.setGap(18);
        super.doLayout();
    }

    public void onLogin(Handler h) { handler = h; }

    private void submit() {
        if (!loginBtn.isEnabled()) return;
        String id = idField.text();
        String pw = new String(((JPasswordField) pwField.input).getPassword());
        if (id.isEmpty() || pw.isEmpty()) {
            error.error("Please enter your " + copy.idLabel().toLowerCase(Locale.ROOT) + " and password.");
            (id.isEmpty() ? idField.input : pwField.input).requestFocusInWindow();
            return;
        }
        error.clear();
        if (copy.rememberOption()) {
            if (remember.isChecked()) PREFS.put("lastId", id); else PREFS.remove("lastId");
        }
        setBusy(true);
        handler.login(id, pw);
    }

    public void setBusy(boolean busy) {
        loginBtn.setEnabled(!busy);
        loginBtn.setText(busy ? "Signing in…" : "Log in");
        setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : null);
    }

    public void showError(String msg) {
        setBusy(false);
        // don't repeat the "can't reach server" warning that is already shown at the top
        boolean same = server.isVisible() && msg.equals(serverText);
        error.error(same ? "Couldn't sign in — the lab server can't be reached (see above)." : msg);
        pwField.input.setText("");
        pwField.input.requestFocusInWindow();
    }

    /** Shown at the top of the form when the lab server can't be reached. */
    public void setServerProblem(String msgOrNull) {
        serverText = msgOrNull;
        if (msgOrNull == null) { if (server.isVisible()) server.clear(); }
        else if (!server.isVisible() || !msgOrNull.equals(shownServerText)) { server.info(msgOrNull); shownServerText = msgOrNull; }
    }

    /** Clear the form for the next student. */
    public void reset() {
        setBusy(false);
        error.clear();
        pwField.input.setText("");
        idField.input.setText("");   // next student starts with an empty form
        SwingUtilities.invokeLater(() -> (idField.input.getText().isEmpty() ? idField.input : pwField.input).requestFocusInWindow());
    }

    public void focusFirst() {
        SwingUtilities.invokeLater(() -> (idField.input.getText().isEmpty() ? idField.input : pwField.input).requestFocusInWindow());
    }

    private void message(String title, String text) {
        JOptionPane.showMessageDialog(this, text, title, JOptionPane.INFORMATION_MESSAGE);
    }

    // =====================================================================
    //  Hero (left side)
    // =====================================================================
    static class Hero extends JPanel {
        Hero(Copy copy) {
            setOpaque(false);
            JPanel brand = Flex.row(14).align(Flex.Align.CENTER).build(
                    UI.seal(52, Color.WHITE, Theme.BRAND_RED, 16),
                    Flex.column(2).build(
                            UI.label("UNIVERSITY OF THE EAST", Theme.tracking(Theme.inter(W.SEMIBOLD, 12), 1.44f), Color.WHITE),
                            UI.label("College of Engineering", Theme.poppins(W.SEMIBOLD, 18), Color.WHITE)));

            String hours = AppConfig.get("lab.hours", "7:00 AM – 9:00 PM");
            boolean open = isOpen(hours);
            UI.Pill chip = new UI.Pill((open ? "Lab open · " : "Lab closed · ") + hours, Theme.inter(W.MEDIUM, 13),
                    Color.WHITE, Theme.alpha(Color.WHITE, 0.14), 8, 14, 8)
                    .dot(open ? Theme.SUCCESS_DOT : Theme.MUTED).border(Theme.alpha(Color.WHITE, 0.28));

            JPanel features = Flex.row(24).build();
            for (String f : copy.features()) {
                features.add(UI.label(f, Theme.inter(W.MEDIUM, 14), Color.WHITE, Icons.get("check", 16, Color.WHITE), 8));
            }

            JPanel content = Flex.column(24).build(
                    chip,
                    UI.heading(copy.headline(), Theme.tracking(Theme.poppins(W.BOLD, 64), -1.28f), Color.WHITE, 1.05f),
                    new UI.WrapText(copy.body(), Theme.inter(W.REGULAR, 17), Color.WHITE, 1.5f, 480),
                    features);

            Flex.column(0).justify(Flex.Justify.BETWEEN).pad(56).into(this, brand, content);
        }

        static boolean isOpen(String hours) {
            try {
                String[] p = hours.split("[–-]");
                DateTimeFormatter f = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
                LocalTime a = LocalTime.parse(p[0].trim().toUpperCase(Locale.ROOT), f);
                LocalTime b = LocalTime.parse(p[1].trim().toUpperCase(Locale.ROOT), f);
                LocalTime now = LocalTime.now();
                return !now.isBefore(a) && now.isBefore(b);
            } catch (Exception e) {
                return true;
            }
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.hq(g);
            int w = getWidth(), h = getHeight();
            RoundRectangle2D shape = new RoundRectangle2D.Double(0, 0, w, h, 56, 56);
            g.setClip(shape);
            // "Photo — replace with building image": put icons/hero.jpg in resources to use a real photo
            g.setColor(Theme.HERO_BASE);
            g.fillRect(0, 0, w, h);
            if (PHOTO != null) {
                double s = Math.max(w / (double) PHOTO.getWidth(null), h / (double) PHOTO.getHeight(null));
                int iw = (int) (PHOTO.getWidth(null) * s), ih = (int) (PHOTO.getHeight(null) * s);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(PHOTO, (w - iw) / 2, (h - ih) / 2, iw, ih, null);
            }
            // Brand overlay gradient (Figma "Brand overlay"; violet in the admin console)
            g.setPaint(new LinearGradientPaint(new Point2D.Float(0, 0), new Point2D.Float(0, h),
                    new float[]{0f, 0.55f, 1f},
                    Theme.HERO_GRADIENT));
            g.fillRect(0, 0, w, h);
            // Deco rings
            double s = w / 704.0;
            g.setColor(Theme.alpha(Color.WHITE, 0.16));
            g.setStroke(new BasicStroke(1.2f));
            g.draw(new Ellipse2D.Double(360 * s, -220 * s, 560 * s, 560 * s));
            g.draw(new Ellipse2D.Double(-300 * s, h - 408 * s, 780 * s, 780 * s));
            g.dispose();
        }

        private static final Image PHOTO = loadPhoto();
        private static Image loadPhoto() {
            for (String n : new String[]{"/icons/hero.jpg", "/icons/hero.png"}) {
                try (var in = Hero.class.getResourceAsStream(n)) {
                    if (in != null) return javax.imageio.ImageIO.read(in);
                } catch (Exception ignored) {}
            }
            return null;
        }
    }
}
