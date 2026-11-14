package ph.ue.comlab.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Design tokens copied from the Figma file "UE CompLab – Login (Modern)".
 * Every color and font in the app comes from here, so a change in Figma
 * only needs to be updated in one place.
 */
public final class Theme {
    private Theme() {}

    // ---- brand ----
    // Accent colors. Orange for the student kiosk; useAdminTheme() switches them to violet
    // for the admin console (each app runs in its own JVM, so this never mixes).
    public static Color BRAND_RED     = new Color(0xC8102E);
    public static Color ORANGE        = new Color(0xF47A20);
    public static Color ORANGE_HOVER  = new Color(0xE06A12);
    public static Color ORANGE_SOFT   = new Color(0xFFF4EC);
    public static Color HERO_BASE     = new Color(0x4A1410);
    /** Login hero overlay gradient: top, middle (55%), bottom. */
    public static Color[] HERO_GRADIENT = {new Color(200, 16, 46, 184), new Color(220, 74, 30, 209), new Color(244, 122, 32, 245)};

    /** Violet theme for the admin console (matches the "Admin" screens in Figma). */
    public static void useAdminTheme() {
        BRAND_RED    = new Color(0x5B21B6);
        ORANGE       = new Color(0x7C3AED);
        ORANGE_HOVER = new Color(0x6D28D9);
        ORANGE_SOFT  = new Color(0xF3EEFF);
        HERO_BASE    = new Color(0x2E1065);
        HERO_GRADIENT = new Color[]{new Color(0x4C, 0x1D, 0x95, 217), new Color(0x6D, 0x28, 0xD9, 219), new Color(0x9F, 0x7A, 0xEA, 245)};
    }

    // ---- text ----
    public static final Color TEXT          = new Color(0x18181B);
    public static final Color TEXT_2        = new Color(0x52525B);
    public static final Color MUTED         = new Color(0xA1A1AA);
    public static final Color ON_BRAND      = Color.WHITE;

    // ---- surfaces ----
    public static final Color PAGE          = Color.WHITE;
    public static final Color CANVAS        = new Color(0xF6F6F7);
    public static final Color INPUT         = new Color(0xFAFAFA);
    public static final Color BORDER        = new Color(0xE4E4E7);

    // ---- status ----
    public static final Color SUCCESS       = new Color(0x16A34A);
    public static final Color SUCCESS_SOFT  = new Color(0xDCFCE7);
    public static final Color SUCCESS_DOT   = new Color(0x22C55E);
    public static final Color WARNING       = new Color(0xB45309);
    public static final Color WARNING_SOFT  = new Color(0xFEF3C7);
    public static final Color WARNING_DOT   = new Color(0xF59E0B);
    public static final Color DANGER        = new Color(0xDC2626);
    public static final Color DANGER_SOFT   = new Color(0xFEE2E2);

    // ---- fonts ----
    public enum W { REGULAR, MEDIUM, SEMIBOLD, BOLD }

    private static Font poppinsBold, poppinsMedium, interRegular, interMedium, interSemi;
    private static final Map<String, Font> cache = new HashMap<>();

    /** Load bundled fonts (Poppins + Inter, both SIL Open Font License). Falls back to Segoe UI. */
    public static void init() {
        poppinsBold   = load("/fonts/Poppins-Bold.ttf");
        poppinsMedium = load("/fonts/Poppins-Medium.ttf");
        interRegular  = load("/fonts/Inter-Regular.otf");
        interMedium   = load("/fonts/Inter-Medium.otf");
        interSemi     = load("/fonts/Inter-SemiBold.otf");

        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        UIManager.put("OptionPane.messageFont", inter(W.REGULAR, 14));
        UIManager.put("OptionPane.buttonFont", inter(W.MEDIUM, 13));
        UIManager.put("ToolTip.font", inter(W.REGULAR, 12));
    }

    private static Font load(String path) {
        try (InputStream in = Theme.class.getResourceAsStream(path)) {
            if (in == null) return null;
            Font f = Font.createFont(Font.TRUETYPE_FONT, in);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(f);
            return f;
        } catch (Exception e) {
            return null;
        }
    }

    private static Font fallback(int style, float size) {
        return new Font("Segoe UI", style, Math.round(size)).deriveFont(size);
    }

    /** Headings — Poppins (SemiBold is drawn with the bundled Medium weight). */
    public static Font poppins(W w, float size) {
        return cached("p" + w + size, () -> {
            Font base = (w == W.BOLD) ? poppinsBold : poppinsMedium;
            if (base == null) return fallback(w == W.REGULAR ? Font.PLAIN : Font.BOLD, size);
            return base.deriveFont(size);
        });
    }

    /** Body — Inter. */
    public static Font inter(W w, float size) {
        return cached("i" + w + size, () -> {
            Font base = switch (w) {
                case REGULAR -> interRegular;
                case MEDIUM -> interMedium;
                default -> interSemi;
            };
            if (base == null) return fallback(w == W.REGULAR || w == W.MEDIUM ? Font.PLAIN : Font.BOLD, size);
            return base.deriveFont(size);
        });
    }

    /** Letter-spacing in px, like Figma's "letter spacing". */
    public static Font tracking(Font f, float px) {
        Map<TextAttribute, Object> a = new HashMap<>();
        a.put(TextAttribute.TRACKING, px / f.getSize2D());
        return f.deriveFont(a);
    }

    private static Font cached(String key, java.util.function.Supplier<Font> s) {
        return cache.computeIfAbsent(key, k -> s.get());
    }

    public static Color alpha(Color c, double a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) Math.round(a * 255));
    }

    public static void hq(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }
}
