package ph.ue.comlab.ui;

import javax.swing.*;
import java.awt.*;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * A tiny flexbox layout so screens can be built the same way Figma auto-layout works:
 * direction (row/column), gap, padding, alignment and "fill" children.
 *
 * Example:  JPanel col = Flex.column(12).pad(24).build(title, subtitle, button);
 */
public class Flex implements LayoutManager2 {

    public enum Align { START, CENTER, END, STRETCH }
    public enum Justify { START, CENTER, END, BETWEEN }

    private final boolean row;
    private int gap;
    private Align align = Align.START;
    private Justify justify = Justify.START;
    private final Map<Component, Float> grow = new IdentityHashMap<>();

    private Flex(boolean row, int gap) { this.row = row; this.gap = gap; }

    public void setGap(int g) { gap = g; }

    // ---------------- builder ----------------
    public static Builder row(int gap)    { return new Builder(new Flex(true, gap)); }
    public static Builder column(int gap) { return new Builder(new Flex(false, gap)); }

    public static final class Builder {
        private final Flex f;
        private Insets pad = new Insets(0, 0, 0, 0);
        private Builder(Flex f) { this.f = f; }
        public Builder align(Align a) { f.align = a; return this; }
        public Builder justify(Justify j) { f.justify = j; return this; }
        public Builder pad(int all) { pad = new Insets(all, all, all, all); return this; }
        public Builder pad(int v, int h) { pad = new Insets(v, h, v, h); return this; }
        public Builder pad(int t, int l, int b, int r) { pad = new Insets(t, l, b, r); return this; }

        /** Plain transparent panel. */
        public JPanel build(Component... kids) { return into(transparent(), kids); }

        /** Use this layout on an existing (e.g. painted) panel. */
        public <T extends JComponent> T into(T panel, Component... kids) {
            panel.setLayout(f);
            if (pad.top + pad.left + pad.bottom + pad.right > 0) {
                panel.setBorder(BorderFactory.createEmptyBorder(pad.top, pad.left, pad.bottom, pad.right));
            }
            for (Component k : kids) if (k != null) panel.add(k);
            return panel;
        }
    }

    public static JPanel transparent() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        return p;
    }

    /** Make a child take the remaining space along the main axis. */
    public static <T extends Component> T grow(T c) { return grow(c, 1f); }
    public static <T extends Component> T grow(T c, float weight) {
        Container parent = c.getParent();
        if (parent != null && parent.getLayout() instanceof Flex fl) fl.grow.put(c, weight);
        else ((JComponent) c).putClientProperty("flex.grow", weight);
        return c;
    }

    /** Fixed-size empty space. */
    public static Component space(int px) {
        return Box.createRigidArea(new Dimension(px, px));
    }

    /** Expanding spacer. */
    public static JComponent spring() {
        JPanel p = transparent();
        p.putClientProperty("flex.grow", 1f);
        p.setPreferredSize(new Dimension(0, 0));
        return p;
    }

    // ---------------- LayoutManager2 ----------------
    @Override public void addLayoutComponent(Component comp, Object constraints) {
        if (constraints instanceof Number n) grow.put(comp, n.floatValue());
        else if (comp instanceof JComponent jc && jc.getClientProperty("flex.grow") instanceof Float w) grow.put(comp, w);
    }
    @Override public void addLayoutComponent(String name, Component comp) { addLayoutComponent(comp, null); }
    @Override public void removeLayoutComponent(Component comp) { grow.remove(comp); }
    @Override public Dimension maximumLayoutSize(Container t) { return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE); }
    @Override public float getLayoutAlignmentX(Container t) { return 0f; }
    @Override public float getLayoutAlignmentY(Container t) { return 0f; }
    @Override public void invalidateLayout(Container t) {}

    private float growOf(Component c) {
        Float w = grow.get(c);
        if (w == null && c instanceof JComponent jc && jc.getClientProperty("flex.grow") instanceof Float f) {
            grow.put(c, f);
            w = f;
        }
        return w == null ? 0f : w;
    }

    private int main(Dimension d)  { return row ? d.width : d.height; }
    private int cross(Dimension d) { return row ? d.height : d.width; }

    /** Preferred size of a child when it is given a certain width (handles wrapping text). */
    static Dimension prefFor(Component c, int width) {
        if (c.isPreferredSizeSet()) return c.getPreferredSize();
        if (width > 0) {
            if (c instanceof WidthAware wa) {
                int w = Math.min(width, c.getPreferredSize().width);
                if (c instanceof JComponent jc && Boolean.TRUE.equals(jc.getClientProperty("flex.fillWidth"))) w = width;
                return new Dimension(w, wa.heightForWidth(w));
            }
            if (c instanceof Container ct && ct.getLayout() instanceof Flex fl) {
                return fl.sizeForWidth(ct, width);
            }
        }
        return c.getPreferredSize();
    }


    /** Natural size of this container if it were given {@code width} pixels wide (width<=0 = unlimited). */
    public Dimension sizeForWidth(Container parent, int width) {
        Insets in = parent.getInsets();
        Component[] kids = visible(parent);
        if (kids.length == 0) return new Dimension(in.left + in.right, in.top + in.bottom);
        int inner = width > 0 ? width - in.left - in.right : 0;
        int gaps = (kids.length - 1) * gap;
        if (!row) {
            int h = gaps, w = 0;
            for (Component c : kids) {
                int cw = inner;
                if (inner > 0 && alignOf(c) != Align.STRETCH) cw = Math.min(inner, c.getPreferredSize().width);
                Dimension d = inner > 0 ? prefFor(c, cw) : c.getPreferredSize();
                h += d.height;
                w = Math.max(w, d.width);
            }
            int outW = width > 0 && hasGrowOrWide(kids, inner) ? width : w + in.left + in.right;
            return new Dimension(Math.min(outW, width > 0 ? width : Integer.MAX_VALUE), h + in.top + in.bottom);
        } else {
            int fixed = gaps; float tg = 0;
            for (Component c : kids) { float g = growOf(c); if (g > 0) tg += g; else fixed += c.getPreferredSize().width; }
            int free = inner > 0 ? Math.max(0, inner - fixed) : 0;
            int w = gaps, h = 0;
            for (Component c : kids) {
                float g = growOf(c);
                int cw = (g > 0 && inner > 0) ? Math.round(free * g / tg) : c.getPreferredSize().width;
                Dimension d = prefFor(c, cw);
                if (g > 0 && inner <= 0) cw = d.width;
                w += cw;
                h = Math.max(h, d.height);
            }
            int outW = (tg > 0 && width > 0) ? width : w + in.left + in.right;
            return new Dimension(outW, h + in.top + in.bottom);
        }
    }

    private boolean hasGrowOrWide(Component[] kids, int inner) {
        for (Component c : kids) if (alignOf(c) == Align.STRETCH) return true;
        return false;
    }

    private Align alignOf(Component c) {
        if (c instanceof JComponent jc && jc.getClientProperty("flex.self") instanceof Align a) return a;
        return align;
    }

    /** Child keeps its own size on the cross axis (like Figma "hug"). */
    public static <T extends JComponent> T hug(T c) { c.putClientProperty("flex.self", Align.START); return c; }
    /** Child fills the cross axis (like Figma "fill container"). */
    public static <T extends JComponent> T fill(T c) { c.putClientProperty("flex.self", Align.STRETCH); return c; }
    public static <T extends JComponent> T self(T c, Align a) { c.putClientProperty("flex.self", a); return c; }

    private static Component[] visible(Container p) {
        return java.util.Arrays.stream(p.getComponents()).filter(Component::isVisible).toArray(Component[]::new);
    }

    @Override public Dimension preferredLayoutSize(Container parent) {
        return sizeForWidth(parent, 0);
    }

    @Override public Dimension minimumLayoutSize(Container parent) { return preferredLayoutSize(parent); }

    @Override public void layoutContainer(Container parent) {
        Insets in = parent.getInsets();
        int W = parent.getWidth() - in.left - in.right;
        int H = parent.getHeight() - in.top - in.bottom;
        int mainAvail = row ? W : H, crossAvail = row ? H : W;

        Component[] kids = parent.getComponents();
        int n = 0, used = 0;
        float totalGrow = 0;
        Dimension[] ds = new Dimension[kids.length];
        for (int i = 0; i < kids.length; i++) {
            if (!kids[i].isVisible()) continue;
            ds[i] = row ? kids[i].getPreferredSize() : prefFor(kids[i], alignOf(kids[i]) == Align.STRETCH ? crossAvail : Math.min(crossAvail, kids[i].getPreferredSize().width));
            float gw = growOf(kids[i]);
            totalGrow += gw;
            used += gw > 0 ? 0 : main(ds[i]);
            n++;
        }
        used += Math.max(0, n - 1) * gap;
        int free = Math.max(0, mainAvail - used);

        // grown children with width-aware content: re-measure at their final width
        int pos = 0, extraGap = 0;
        if (totalGrow == 0) {
            int content = 0;
            for (int i = 0; i < kids.length; i++) if (ds[i] != null) content += main(ds[i]);
            content += Math.max(0, n - 1) * gap;
            int slack = Math.max(0, mainAvail - content);
            switch (justify) {
                case CENTER -> pos = slack / 2;
                case END -> pos = slack;
                case BETWEEN -> extraGap = n > 1 ? slack / (n - 1) : 0;
                default -> {}
            }
        }
        float growLeft = free;
        float growRemaining = totalGrow;

        for (int i = 0; i < kids.length; i++) {
            Component c = kids[i];
            if (ds[i] == null) continue;
            float gw = growOf(c);
            int len;
            if (gw > 0) {
                len = Math.round(growLeft * (gw / growRemaining));
                growLeft -= len;
                growRemaining -= gw;
            } else len = main(ds[i]);

            if (row) ds[i] = prefFor(c, len);
            int cs = Math.min(cross(ds[i]), crossAvail);
            int cpos = 0;
            switch (alignOf(c)) {
                case STRETCH -> cs = crossAvail;
                case CENTER -> cpos = (crossAvail - cs) / 2;
                case END -> cpos = crossAvail - cs;
                default -> {}
            }
            if (row) c.setBounds(in.left + pos, in.top + cpos, len, cs);
            else c.setBounds(in.left + cpos, in.top + pos, cs, len);
            pos += len + gap + extraGap;
        }
    }

    /** Components whose preferred height depends on the width they get (wrapping text). */
    public interface WidthAware { int heightForWidth(int width); }
}
