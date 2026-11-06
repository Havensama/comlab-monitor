package ph.ue.comlab.ui;

import ph.ue.comlab.ui.Theme.W;

import javax.swing.*;
import java.awt.*;

/** Figma "Top nav" (node 9:89): white bar with the UE seal on the left and session/user info on the right. */
public class TopNav extends JPanel {
    public TopNav(String title, String subtitle, JComponent right) {
        setBackground(Theme.PAGE);
        JPanel brand = Flex.row(12).align(Flex.Align.CENTER).build(
                UI.seal(40, Theme.BRAND_RED, Color.WHITE, 13),
                Flex.column(0).build(
                        UI.label(title, Theme.poppins(W.SEMIBOLD, 16), Theme.TEXT),
                        UI.label(subtitle, Theme.inter(W.REGULAR, 12), Theme.MUTED)));
        Flex.row(0).justify(Flex.Justify.BETWEEN).align(Flex.Align.CENTER).pad(16, 40, 17, 40).into(this, brand, right);
    }

    @Override protected void paintComponent(Graphics g) {
        g.setColor(Theme.PAGE);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Theme.BORDER);
        g.fillRect(0, getHeight() - 1, getWidth(), 1);
    }

    /** Grey rounded chip ("PC-14 · Lab 6", "Session 00:12:45"). */
    public static UI.Pill chip(String text, String icon) {
        UI.Pill p = new UI.Pill(text, Theme.inter(W.MEDIUM, 13), Theme.TEXT_2, Theme.CANVAS, 8, 14, 8);
        if (icon != null) p.icon(Icons.get(icon, 16, Theme.TEXT_2));
        return p;
    }

    /** Avatar + name + id. */
    public static JPanel user(String initials, String name, String sub) {
        return Flex.row(10).align(Flex.Align.CENTER).build(
                UI.avatar(initials, 36),
                Flex.column(0).build(
                        UI.label(name, Theme.inter(W.MEDIUM, 14), Theme.TEXT),
                        UI.label(sub, Theme.inter(W.REGULAR, 12), Theme.MUTED)));
    }
}
