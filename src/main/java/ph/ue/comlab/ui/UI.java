package ph.ue.comlab.ui;

import ph.ue.comlab.ui.Theme.W;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Reusable components that match the Figma components (Input Field, Button / Primary, pills, cards...). */
public final class UI {
    private UI() {}

    // =====================================================================
    //  Text
    // =====================================================================
    private static final java.awt.font.FontRenderContext FRC =
            new java.awt.font.FontRenderContext(null, true, true);

    /** Width of text exactly as it's painted (anti-aliased, fractional metrics). */
    public static int textWidth(Font f, String s) {
        if (s == null || s.isEmpty()) return 0;
        return (int) Math.ceil(f.getStringBounds(s, FRC).getWidth());
    }

    public static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    public static JLabel label(String text, Font font, Color color, Icon icon, int gap) {
        JLabel l = label(text, font, color);
        l.setIcon(icon);
        l.setIconTextGap(gap);
        return l;
    }

    /** Clickable text (e.g. "Forgot password?"). */
    public static JLabel link(String text, Font font, Color color, Runnable onClick) {
        JLabel l = label(text, font, color);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        l.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { onClick.run(); }
        });
        return l;
    }

    /** Multi-line text that wraps at a max width, with a line-height multiplier (Figma "line height"). */
    public static class WrapText extends JComponent implements Flex.WidthAware {
        private String text;
        private final float lineHeight;
        private final int maxWidth;
        private int align = SwingConstants.LEFT;

        public WrapText(String text, Font font, Color color, float lineHeight, int maxWidth) {
            this.text = text;
            this.lineHeight = lineHeight;
            this.maxWidth = maxWidth;
            setFont(font);
            setForeground(color);
        }

        public void setText(String t) { text = t; revalidate(); repaint(); }
        public String getText() { return text; }
        public WrapText alignRight() { align = SwingConstants.RIGHT; return this; }

        private List<String> lines(int width) {
            List<String> out = new ArrayList<>();
            Font f = getFont();
            for (String para : text.split("\n", -1)) {
                StringBuilder line = new StringBuilder();
                for (String word : para.split(" ")) {
                    String trial = line.length() == 0 ? word : line + " " + word;
                    if (textWidth(f, trial) > width && line.length() > 0) {
                        out.add(line.toString());
                        line = new StringBuilder(word);
                    } else line = new StringBuilder(trial);
                }
                out.add(line.toString());
            }
            return out;
        }

        private int lh() { return Math.round(getFont().getSize2D() * lineHeight); }

        @Override public int heightForWidth(int width) { return lines(width).size() * lh(); }

        @Override public Dimension getPreferredSize() {
            int natural = 0;
            for (String p : text.split("\n")) natural = Math.max(natural, textWidth(getFont(), p) + 1);
            int w = Math.min(natural, maxWidth);
            return new Dimension(w, heightForWidth(w));
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            Theme.hq(g);
            g.setFont(getFont());
            g.setColor(getForeground());
            FontMetrics fm = g.getFontMetrics();
            int lh = lh();
            int y = (lh - fm.getHeight()) / 2 + fm.getAscent();
            for (String l : lines(getWidth())) {
                int x = align == SwingConstants.RIGHT ? getWidth() - textWidth(getFont(), l) : 0;
                g.drawString(l, x, y);
                y += lh;
            }
        }
    }

    /** Big multi-line heading with tight line height (e.g. "Computer / Laboratory"). */
    public static WrapText heading(String text, Font font, Color color, float lineHeight) {
        return new WrapText(text, font, color, lineHeight, 2000);
    }

    // =====================================================================
    //  Card / rounded panel
    // =====================================================================
    public static class Card extends JPanel {
        private Color bg, border;
        private float borderWidth = 1f;
        private final int radius;
        private boolean dashed;
        private Color shadow;
        private int shadowY, shadowBlur;

        public Card(Color bg, int radius, Color border) {
            this.bg = bg; this.radius = radius; this.border = border;
            setOpaque(false);
        }
        public Card dashed() { dashed = true; borderWidth = 1.5f; return this; }
        public Card borderWidth(float w) { borderWidth = w; return this; }
        public Card shadow(Color c, int y, int blur) { shadow = c; shadowY = y; shadowBlur = blur; return this; }
        public void setColors(Color bg, Color border, float bw) { this.bg = bg; this.border = border; this.borderWidth = bw; repaint(); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.hq(g);
            int w = getWidth(), h = getHeight();
            if (shadow != null) paintShadow(g, 0, 0, w, h, radius, shadow, shadowY, shadowBlur);
            if (bg != null) {
                g.setColor(bg);
                g.fill(new RoundRectangle2D.Double(0, 0, w, h, radius * 2, radius * 2));
            }
            if (border != null) {
                float bw = borderWidth;
                g.setColor(border);
                g.setStroke(dashed
                        ? new BasicStroke(bw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10, new float[]{6, 5}, 0)
                        : new BasicStroke(bw));
                g.draw(new RoundRectangle2D.Double(bw / 2, bw / 2, w - bw, h - bw, radius * 2 - bw, radius * 2 - bw));
            }
            g.dispose();
        }
    }

    /** Soft drop shadow (Figma "drop shadow"): a blurred round-rect, cached per size. */
    static void paintShadow(Graphics2D g, int x, int y, int w, int h, int r, Color c, int dy, int blur) {
        if (w <= 0 || h <= 0) return;
        String key = w + "x" + h + "r" + r + "c" + c.getRGB() + "a" + c.getAlpha() + "b" + blur;
        java.awt.image.BufferedImage img = SHADOWS.get(key);
        if (img == null) {
            int pad = blur * 2;
            img = new java.awt.image.BufferedImage(w + pad * 2, h + pad * 2, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D sg = img.createGraphics();
            Theme.hq(sg);
            sg.setColor(c);
            sg.fill(new RoundRectangle2D.Double(pad, pad, w, h, r * 2, r * 2));
            sg.dispose();
            img = blur(img, Math.max(1, blur / 2));
            if (SHADOWS.size() > 200) SHADOWS.clear();
            SHADOWS.put(key, img);
        }
        int pad = blur * 2;
        g.drawImage(img, x - pad, y + dy - pad, null);
    }

    private static final java.util.Map<String, java.awt.image.BufferedImage> SHADOWS = new java.util.HashMap<>();

    /** Box blur done 3 times ≈ Gaussian blur. */
    private static java.awt.image.BufferedImage blur(java.awt.image.BufferedImage src, int radius) {
        int size = radius * 2 + 1;
        float[] k = new float[size];
        java.util.Arrays.fill(k, 1f / size);
        java.awt.image.ConvolveOp h = new java.awt.image.ConvolveOp(new java.awt.image.Kernel(size, 1, k), java.awt.image.ConvolveOp.EDGE_NO_OP, null);
        java.awt.image.ConvolveOp v = new java.awt.image.ConvolveOp(new java.awt.image.Kernel(1, size, k), java.awt.image.ConvolveOp.EDGE_NO_OP, null);
        java.awt.image.BufferedImage out = src;
        for (int i = 0; i < 3; i++) out = v.filter(h.filter(out, null), null);
        return out;
    }

    // =====================================================================
    //  Pills / chips / badges
    // =====================================================================
    public static class Pill extends JComponent {
        private String text;
        private Color fg, bg, borderColor, dot;
        private Icon icon;
        private final int padV, padH, gap;

        public Pill(String text, Font font, Color fg, Color bg, int padV, int padH) {
            this(text, font, fg, bg, padV, padH, 6);
        }
        public Pill(String text, Font font, Color fg, Color bg, int padV, int padH, int gap) {
            this.text = text; this.fg = fg; this.bg = bg; this.padV = padV; this.padH = padH; this.gap = gap;
            setFont(font);
        }
        public Pill dot(Color c) { dot = c; revalidate(); return this; }
        public Pill icon(Icon i) { icon = i; revalidate(); return this; }
        public Pill border(Color c) { borderColor = c; return this; }
        public void set(String text, Color fg, Color bg, Color dot) {
            this.text = text; this.fg = fg; this.bg = bg; this.dot = dot;
            revalidate(); repaint();
        }
        public String getText() { return text; }

        private int lead() {
            if (icon != null) return icon.getIconWidth() + gap;
            if (dot != null) return dotSize() + gap;
            return 0;
        }
        private int dotSize() { return getFont().getSize() >= 13 ? 8 : 6; }

        @Override public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            int h = Math.max(fm.getAscent() + fm.getDescent(), icon != null ? icon.getIconHeight() : 0);
            return new Dimension(padH * 2 + lead() + textWidth(getFont(), text) + 1, padV * 2 + h);
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.hq(g);
            int w = getWidth(), h = getHeight();
            if (bg != null) { g.setColor(bg); g.fill(new RoundRectangle2D.Double(0, 0, w, h, h, h)); }
            if (borderColor != null) {
                g.setColor(borderColor);
                g.draw(new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, h - 1, h - 1));
            }
            int x = padH;
            if (icon != null) {
                icon.paintIcon(this, g, x, (h - icon.getIconHeight()) / 2);
                x += icon.getIconWidth() + gap;
            } else if (dot != null) {
                int d = dotSize();
                g.setColor(dot);
                g.fill(new Ellipse2D.Double(x, (h - d) / 2.0, d, d));
                x += d + gap;
            }
            g.setFont(getFont());
            g.setColor(fg);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(text, x, (h - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }
    }

    /** Status pill used in cards and tables ("Working", "Resolved", "In progress"...). */
    public static Pill status(String text, Color fg, Color bg, Color dot) {
        Pill p = new Pill(text, Theme.inter(W.SEMIBOLD, 12), fg, bg, 4, 10);
        if (dot != null) p.dot(dot);
        return p;
    }

    // =====================================================================
    //  Buttons
    // =====================================================================
    public static class PrimaryButton extends JButton {
        private final Icon trailing;
        private int visH = 51;
        /** 44px tall, 14px text — for toolbars and side panels. */
        public PrimaryButton compact() { visH = 44; setFont(Theme.inter(W.SEMIBOLD, 14)); return this; }
        public PrimaryButton(String text, String trailingIcon) {
            super(text);
            this.trailing = trailingIcon == null ? null : Icons.get(trailingIcon, 18, Color.WHITE);
            setFont(Theme.inter(W.SEMIBOLD, 16));
            setForeground(Color.WHITE);
            setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false); setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setRolloverEnabled(true);
        }
        @Override public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            int w = 24 * 2 + textWidth(getFont(), getText()) + (trailing != null ? 8 + 18 : 0);
            return new Dimension(w, visH + 10); // +10 room for the drop shadow
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.hq(g);
            int w = getWidth(), h = getHeight() - 10;
            boolean on = isEnabled();
            if (on) paintShadow(g, 8, 0, w - 16, h - 4, 12, Theme.alpha(Theme.ORANGE, 0.45), 6, 10);
            Color c = !on ? Theme.alpha(Theme.ORANGE, 0.5)
                    : getModel().isPressed() ? Theme.ORANGE_HOVER.darker()
                    : getModel().isRollover() ? Theme.ORANGE_HOVER : Theme.ORANGE;
            g.setColor(c);
            g.fill(new RoundRectangle2D.Double(0, 0, w, h, 24, 24));
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            int tw = textWidth(getFont(), getText()) + (trailing != null ? 8 + 18 : 0);
            int x = (w - tw) / 2;
            g.setColor(Color.WHITE);
            g.drawString(getText(), x, (h - fm.getHeight()) / 2 + fm.getAscent());
            if (trailing != null) trailing.paintIcon(this, g, x + textWidth(getFont(), getText()) + 8, (h - 18) / 2);
            g.dispose();
        }
    }

    public static class SecondaryButton extends JButton {
        private final Icon leading;
        private final int padH;
        public SecondaryButton(String text, String leadingIcon) { this(text, leadingIcon, 16, 22); }
        public SecondaryButton(String text, String leadingIcon, float fontSize, int padH) {
            super(text);
            this.padH = padH;
            this.leading = leadingIcon == null ? null : Icons.get(leadingIcon, Math.round(fontSize + 2), Theme.TEXT);
            setFont(Theme.inter(W.SEMIBOLD, fontSize));
            setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false); setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setRolloverEnabled(true);
        }
        @Override public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            int iw = leading != null ? leading.getIconWidth() + 8 : 0;
            int h = Math.round(getFont().getSize2D() * 1.2f) + (getFont().getSize() >= 16 ? 32 : 27);
            return new Dimension(padH * 2 + iw + textWidth(getFont(), getText()) + 2, h);
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.hq(g);
            int w = getWidth(), h = getHeight();
            g.setColor(getModel().isRollover() && isEnabled() ? Theme.CANVAS : Theme.PAGE);
            g.fill(new RoundRectangle2D.Double(0, 0, w, h, 24, 24));
            g.setColor(Theme.BORDER);
            g.draw(new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, 24, 24));
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            int iw = leading != null ? leading.getIconWidth() + 8 : 0;
            int x = (w - iw - textWidth(getFont(), getText())) / 2;
            if (leading != null) { leading.paintIcon(this, g, x, (h - leading.getIconHeight()) / 2); x += iw; }
            g.setColor(isEnabled() ? Theme.TEXT : Theme.MUTED);
            g.drawString(getText(), x, (h - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }
    }

    /** Small square icon button (e.g. log out in the top bar). */
    public static JButton iconButton(String icon, int size, Color color, String tooltip, Runnable action) {
        JButton b = new JButton() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Theme.hq(g);
                if (getModel().isRollover()) {
                    g.setColor(Theme.CANVAS);
                    g.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));
                }
                Icons.paint(g, icon, (getWidth() - size) / 2.0, (getHeight() - size) / 2.0, size, color, 2f);
                g.dispose();
            }
        };
        b.setPreferredSize(new Dimension(size + 16, size + 16));
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false); b.setOpaque(false);
        b.setRolloverEnabled(true);
        b.setToolTipText(tooltip);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> action.run());
        return b;
    }

    // =====================================================================
    //  Inputs
    // =====================================================================
    public static class TextInput extends JTextField {
        private final String placeholder;
        public TextInput(String placeholder) {
            this.placeholder = placeholder;
            styleInput(this);
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            paintPlaceholder(this, g, placeholder);
        }
    }

    public static class PasswordInput extends JPasswordField {
        private final String placeholder;
        public PasswordInput(String placeholder) {
            this.placeholder = placeholder;
            styleInput(this);
            setEchoChar('•');
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            paintPlaceholder(this, g, placeholder);
        }
    }

    static void styleInput(JTextComponent t) {
        t.setFont(Theme.inter(W.REGULAR, 15));
        t.setForeground(Theme.TEXT);
        t.setCaretColor(Theme.ORANGE);
        t.setSelectionColor(Theme.alpha(Theme.ORANGE, 0.25));
        t.setOpaque(false);
        t.setBorder(BorderFactory.createEmptyBorder());
    }

    static void paintPlaceholder(JTextComponent t, Graphics g0, String placeholder) {
        if (placeholder == null || !t.getText().isEmpty()) return;
        Graphics2D g = (Graphics2D) g0.create();
        Theme.hq(g);
        g.setFont(t.getFont());
        g.setColor(Theme.MUTED);
        Insets in = t.getInsets();
        FontMetrics fm = g.getFontMetrics();
        if (t instanceof JTextArea) g.drawString(placeholder, in.left, in.top + fm.getAscent());
        else g.drawString(placeholder, in.left, (t.getHeight() - fm.getHeight()) / 2 + fm.getAscent());
        g.dispose();
    }

    /** The Figma "Input Field" component: label + rounded field with leading/trailing icon. */
    public static class InputField extends JPanel {
        public final JTextComponent input;
        private final Card box;

        public InputField(String labelText, String leadingIcon, JTextComponent input, JComponent trailing) {
            this.input = input;
            setOpaque(false);
            box = new Card(Theme.INPUT, 12, Theme.BORDER);
            JLabel icon = new JLabel(Icons.get(leadingIcon, 20, Theme.MUTED));
            Flex.row(12).align(Flex.Align.CENTER).pad(14, 16, 14, 16)
                    .into(box, icon, Flex.grow(input), trailing);
            input.setPreferredSize(new Dimension(10, 22));
            focusRing(input, box);
            Flex.column(8).align(Flex.Align.STRETCH).into(this,
                    Flex.hug(label(labelText, Theme.inter(W.MEDIUM, 14), Theme.TEXT)), box);
        }

        public String text() { return input.getText().trim(); }
    }

    /** Orange border while a field has focus (and red when invalid). */
    public static void focusRing(JComponent input, Card box) {
        input.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { box.setColors(Theme.PAGE, Theme.ORANGE, 1.5f); }
            @Override public void focusLost(FocusEvent e) { box.setColors(Theme.INPUT, Theme.BORDER, 1f); }
        });
    }

    /** Eye button that shows / hides the password. */
    public static JComponent eyeToggle(JPasswordField pf) {
        JLabel eye = new JLabel(Icons.get("eye", 20, Theme.TEXT_2));
        eye.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        eye.setToolTipText("Show password");
        eye.addMouseListener(new MouseAdapter() {
            boolean shown;
            @Override public void mouseClicked(MouseEvent e) {
                shown = !shown;
                pf.setEchoChar(shown ? (char) 0 : '•');
                eye.setIcon(Icons.get(shown ? "eye-off" : "eye", 20, Theme.TEXT_2));
                eye.setToolTipText(shown ? "Hide password" : "Show password");
                pf.requestFocusInWindow();
            }
        });
        return eye;
    }

    // =====================================================================
    //  Checkbox
    // =====================================================================
    public static class Check extends JComponent {
        private boolean checked;
        private final int size, radius;
        private final List<Consumer<Boolean>> listeners = new ArrayList<>();
        public Check(int size, int radius, boolean checked) {
            this.size = size; this.radius = radius; this.checked = checked;
            setPreferredSize(new Dimension(size, size));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { toggle(); }
            });
        }
        public boolean isChecked() { return checked; }
        public void setChecked(boolean c) { checked = c; repaint(); }
        public void toggle() { checked = !checked; repaint(); listeners.forEach(l -> l.accept(checked)); }
        public void onChange(Consumer<Boolean> l) { listeners.add(l); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.hq(g);
            if (checked) {
                g.setColor(Theme.ORANGE);
                g.fill(new RoundRectangle2D.Double(0, 0, size, size, radius * 2, radius * 2));
                double is = size * 0.64;
                Icons.paint(g, "check", (size - is) / 2, (size - is) / 2, is, Color.WHITE, 2.6f);
            } else {
                g.setColor(Theme.PAGE);
                g.fill(new RoundRectangle2D.Double(0, 0, size, size, radius * 2, radius * 2));
                g.setColor(Theme.BORDER);
                g.setStroke(new BasicStroke(1.5f));
                g.draw(new RoundRectangle2D.Double(0.75, 0.75, size - 1.5, size - 1.5, radius * 2, radius * 2));
            }
            g.dispose();
        }
    }

    // =====================================================================
    //  Segmented control (Low / Medium / High)
    // =====================================================================
    public static class Segmented extends JComponent {
        private final String[] options;
        private int selected;
        private final List<Consumer<String>> listeners = new ArrayList<>();
        public Segmented(String[] options, int selected) {
            this.options = options; this.selected = selected;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    int seg = (getWidth() - 8) / options.length;
                    int i = Math.max(0, Math.min(options.length - 1, (e.getX() - 4) / Math.max(1, seg)));
                    setSelected(i);
                    listeners.forEach(l -> l.accept(options[i]));
                }
            });
        }
        public String value() { return options[selected]; }
        public void setSelected(int i) { selected = i; repaint(); }
        public void onChange(Consumer<String> l) { listeners.add(l); }
        @Override public Dimension getPreferredSize() { return new Dimension(300, 45); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.hq(g);
            int w = getWidth(), h = getHeight();
            g.setColor(Theme.CANVAS);
            g.fill(new RoundRectangle2D.Double(0, 0, w, h, 24, 24));
            double seg = (w - 8 - (options.length - 1) * 4) / (double) options.length;
            for (int i = 0; i < options.length; i++) {
                double x = 4 + i * (seg + 4);
                boolean sel = i == selected;
                if (sel) {
                    g.setColor(new Color(0, 0, 0, 14));
                    g.fill(new RoundRectangle2D.Double(x, 5, seg, h - 8, 18, 18));
                    g.setColor(Theme.PAGE);
                    g.fill(new RoundRectangle2D.Double(x, 4, seg, h - 8, 18, 18));
                }
                g.setFont(sel ? Theme.inter(W.SEMIBOLD, 14) : Theme.inter(W.MEDIUM, 14));
                g.setColor(sel ? Theme.ORANGE : Theme.TEXT_2);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(options[i], (int) (x + (seg - fm.stringWidth(options[i])) / 2),
                        (h - fm.getHeight()) / 2 + fm.getAscent());
            }
            g.dispose();
        }
    }

    // =====================================================================
    //  Select (dropdown that looks like the input fields)
    // =====================================================================
    public static class Select extends Card {
        private final String placeholder;
        private String value;
        private final JLabel text;
        private final List<Consumer<String>> listeners = new ArrayList<>();

        public Select(String placeholder, String[] options) {
            super(Theme.INPUT, 12, Theme.BORDER);
            this.placeholder = placeholder;
            text = label(placeholder, Theme.inter(W.REGULAR, 15), Theme.MUTED);
            JLabel chev = new JLabel(Icons.get("chevron-down", 20, Theme.TEXT_2));
            Flex.row(12).align(Flex.Align.CENTER).pad(14, 16, 14, 16).into(this, Flex.grow(text), chev);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    JPopupMenu menu = new JPopupMenu();
                    menu.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
                    for (String o : options) {
                        JMenuItem mi = new JMenuItem(o);
                        mi.setFont(Theme.inter(W.REGULAR, 14));
                        mi.setPreferredSize(new Dimension(getWidth() - 2, 38));
                        mi.addActionListener(a -> setValue(o));
                        menu.add(mi);
                    }
                    menu.show(Select.this, 0, getHeight() + 4);
                }
            });
        }
        public String value() { return value; }
        public void setValue(String v) {
            value = v;
            text.setText(v == null ? placeholder : v);
            text.setForeground(v == null ? Theme.MUTED : Theme.TEXT);
            listeners.forEach(l -> l.accept(v));
        }
        public void onChange(Consumer<String> l) { listeners.add(l); }
    }

    /** Multi-line text box with placeholder and "0 / 500" counter. */
    public static class TextBox extends Card {
        public final JTextArea area;
        private final JLabel counter;
        private final int max;
        public TextBox(String placeholder, int max, int height) {
            super(Theme.INPUT, 12, Theme.BORDER);
            this.max = max;
            area = new JTextArea() {
                @Override protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    paintPlaceholder(this, g, placeholder);
                }
            };
            styleInput(area);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setDocument(new javax.swing.text.PlainDocument() {
                @Override public void insertString(int offs, String str, javax.swing.text.AttributeSet a)
                        throws javax.swing.text.BadLocationException {
                    if (str == null) return;
                    int room = max - getLength();
                    if (room <= 0) { Toolkit.getDefaultToolkit().beep(); return; }
                    super.insertString(offs, str.length() > room ? str.substring(0, room) : str, a);
                }
            });
            counter = label("0 / " + max, Theme.inter(W.REGULAR, 12), Theme.MUTED);
            counter.setHorizontalAlignment(SwingConstants.RIGHT);
            area.getDocument().addDocumentListener(new DocumentListener() {
                void u() { counter.setText(area.getText().length() + " / " + max); }
                public void insertUpdate(DocumentEvent e) { u(); }
                public void removeUpdate(DocumentEvent e) { u(); }
                public void changedUpdate(DocumentEvent e) { u(); }
            });
            JScrollPane sp = new JScrollPane(area);
            sp.setBorder(BorderFactory.createEmptyBorder());
            sp.setOpaque(false); sp.getViewport().setOpaque(false);
            setLayout(new BorderLayout(0, 6));
            setBorder(BorderFactory.createEmptyBorder(14, 16, 12, 16));
            add(sp, BorderLayout.CENTER);
            add(counter, BorderLayout.SOUTH);
            setPreferredSize(new Dimension(300, height));
            focusRing(area, this);
        }
        public String text() { return area.getText().trim(); }
        public void clear() { area.setText(""); }
    }

    // =====================================================================
    //  Misc
    // =====================================================================
    /** Circle with initials (top bar avatar). */
    public static JComponent avatar(String initials, int size) {
        JComponent c = new JComponent() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Theme.hq(g);
                g.setColor(Theme.ORANGE_SOFT);
                g.fill(new Ellipse2D.Double(0, 0, size, size));
                g.setFont(Theme.inter(W.SEMIBOLD, 13));
                g.setColor(Theme.ORANGE);
                FontMetrics fm = g.getFontMetrics();
                String t = (String) getClientProperty("initials");
                g.drawString(t, (size - fm.stringWidth(t)) / 2, (size - fm.getHeight()) / 2 + fm.getAscent());
                g.dispose();
            }
        };
        c.putClientProperty("initials", initials);
        c.setPreferredSize(new Dimension(size, size));
        return c;
    }

    /** The round "UE" seal (replace with the real logo by putting icons/ue-logo.png in resources). */
    public static JComponent seal(int size, Color bg, Color fg, float fontSize) {
        Image logo = null;
        try (var in = UI.class.getResourceAsStream("/icons/ue-logo.png")) {
            if (in != null) logo = javax.imageio.ImageIO.read(in);
        } catch (Exception ignored) {}
        final Image img = logo;
        JComponent c = new JComponent() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Theme.hq(g);
                if (img != null) {
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g.setClip(new Ellipse2D.Double(0, 0, size, size));
                    g.drawImage(img, 0, 0, size, size, null);
                } else {
                    g.setColor(bg);
                    g.fill(new Ellipse2D.Double(0, 0, size, size));
                    g.setFont(Theme.poppins(W.BOLD, fontSize));
                    g.setColor(fg);
                    FontMetrics fm = g.getFontMetrics();
                    g.drawString("UE", (size - fm.stringWidth("UE")) / 2f, (size - fm.getHeight()) / 2f + fm.getAscent());
                }
                g.dispose();
            }
        };
        c.setPreferredSize(new Dimension(size, size));
        return c;
    }

    /** Thin vertical divider. */
    public static JComponent vDivider(int height) {
        JComponent c = new JComponent() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(Theme.BORDER);
                g.fillRect(0, 0, 1, getHeight());
            }
        };
        c.setPreferredSize(new Dimension(1, height));
        return c;
    }

    /** Inline message bar (success / error) that can auto-hide. */
    public static class Banner extends Card {
        private final JLabel icon = new JLabel();
        private final WrapText text = new WrapText("", Theme.inter(W.MEDIUM, 14), Theme.SUCCESS, 1.4f, 2000);
        private Timer hide;
        public Banner() {
            super(Theme.SUCCESS_SOFT, 12, null);
            Flex.row(10).align(Flex.Align.START).pad(12, 16, 12, 16).into(this, icon, Flex.grow(text));
            setVisible(false);
        }
        public void success(String msg) { show(msg, Theme.SUCCESS, Theme.SUCCESS_SOFT, "check", 4000); }
        public void error(String msg)   { show(msg, Theme.DANGER, Theme.DANGER_SOFT, "alert", 0); }
        public void info(String msg)    { show(msg, Theme.WARNING, Theme.WARNING_SOFT, "alert", 0); }
        public void clear() { setVisible(false); revalidateUp(); }

        private void show(String msg, Color fg, Color bg, String iconName, int autoHideMs) {
            setColors(bg, null, 0);
            text.setForeground(fg);
            icon.setIcon(Icons.get(iconName, 18, fg));
            text.setText(msg);
            setVisible(true);
            revalidateUp();
            if (hide != null) hide.stop();
            if (autoHideMs > 0) {
                hide = new Timer(autoHideMs, e -> clear());
                hide.setRepeats(false);
                hide.start();
            }
        }
        private void revalidateUp() {
            Container p = getParent();
            while (p != null) { p.invalidate(); p = p.getParent(); }
            if (getTopLevelAncestor() != null) { getTopLevelAncestor().validate(); getTopLevelAncestor().repaint(); }
        }
    }

    /** Panel that fills a scroll pane's width (used for pages that may be taller than the screen). */
    public static class ScrollPage extends JPanel implements Scrollable {
        public ScrollPage(Color bg) { setBackground(bg); setOpaque(true); }
        @Override public Dimension getPreferredSize() {
            if (getParent() instanceof JViewport vp && getLayout() instanceof Flex f) {
                Dimension d = f.sizeForWidth(this, vp.getWidth());
                return new Dimension(vp.getWidth(), Math.max(d.height, vp.getHeight()));
            }
            return super.getPreferredSize();
        }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height - 48; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport vp && getLayout() instanceof Flex f
                    && f.sizeForWidth(this, vp.getWidth()).height <= vp.getHeight();
        }
    }

    public static JScrollPane scroll(JComponent page) {
        JScrollPane sp = new JScrollPane(page);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getVerticalScrollBar().setUnitIncrement(24);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        styleScrollBar(sp.getVerticalScrollBar());
        return sp;
    }

    /**
     * Password prompt whose text box has focus right away and submits on Enter.
     * Returns the password, or null if cancelled.
     */
    public static String askPassword(Component parent, String title, String message) {
        JPasswordField pf = new JPasswordField(18);
        pf.setFont(Theme.inter(W.REGULAR, 14));
        JOptionPane pane = new JOptionPane(new Object[]{message, pf}, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        JDialog d = pane.createDialog(parent, title);
        pf.addActionListener(e -> pane.setValue(JOptionPane.OK_OPTION));
        d.addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e) { pf.requestFocusInWindow(); }
        });
        d.addWindowFocusListener(new WindowAdapter() {
            @Override public void windowGainedFocus(WindowEvent e) { pf.requestFocusInWindow(); }
        });
        d.setAlwaysOnTop(true);
        d.setVisible(true);
        d.dispose();
        Object v = pane.getValue();
        return (v instanceof Integer i && i == JOptionPane.OK_OPTION) ? new String(pf.getPassword()) : null;
    }

    /** Thin, rounded, arrow-less scroll bar. */
    public static void styleScrollBar(JScrollBar bar) {
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(10, 10));
        bar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() { thumbColor = new Color(0xD4D4D8); trackColor = new Color(0, 0, 0, 0); }
            private JButton none() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b; }
            @Override protected JButton createDecreaseButton(int o) { return none(); }
            @Override protected JButton createIncreaseButton(int o) { return none(); }
            @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {}
            @Override protected void paintThumb(Graphics g0, JComponent c, Rectangle r) {
                if (r.isEmpty()) return;
                Graphics2D g = (Graphics2D) g0.create();
                Theme.hq(g);
                g.setColor(isDragging ? new Color(0xA1A1AA) : thumbColor);
                g.fill(new RoundRectangle2D.Double(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 6, 6));
                g.dispose();
            }
        });
    }

    /** Fixed width wrapper (e.g. the 400px login form, the 360px side panel). */
    public static <T extends JComponent> T width(T c, int w) {
        c.putClientProperty("fixedWidth", w);
        return c;
    }

    /** A Flex column with a fixed width. */
    public static JPanel fixedColumn(int width, int gap, Component... kids) {
        JPanel p = new JPanel() {
            @Override public Dimension getPreferredSize() {
                return new Dimension(width, ((Flex) getLayout()).sizeForWidth(this, width).height);
            }
        };
        p.setOpaque(false);
        return Flex.column(gap).align(Flex.Align.STRETCH).into(p, kids);
    }

    /** Every app window uses the same 1920x1080 design size: opened at 1920x1080 (or the screen, if smaller), then maximized. */
    public static final int DESIGN_W = 1920, DESIGN_H = 1080;

    public static void sizeWindow(java.awt.Frame frame) {
        java.awt.Rectangle usable = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        frame.setSize(Math.min(DESIGN_W, usable.width), Math.min(DESIGN_H, usable.height));
        frame.setLocationRelativeTo(null);
        frame.setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);
    }
}
