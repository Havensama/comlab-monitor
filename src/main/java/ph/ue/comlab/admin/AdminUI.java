package ph.ue.comlab.admin;

import ph.ue.comlab.ui.*;
import ph.ue.comlab.ui.Theme.W;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.function.Consumer;

/** Small building blocks shared by the admin pages (same style as the student screens). */
final class AdminUI {
    private AdminUI() {}

    /** A page of the admin console: load() runs in the background, show() updates the screen. */
    interface Page<D> {
        JComponent view();
        D load() throws Exception;
        void show(D data);
    }

    static JPanel header(String eyebrow, String title, String subtitle, JComponent right) {
        JPanel titles = Flex.column(6).build(
                UI.label(eyebrow, Theme.tracking(Theme.inter(W.SEMIBOLD, 12), 0.96f), Theme.ORANGE),
                UI.label(title, Theme.tracking(Theme.poppins(W.SEMIBOLD, 28), -0.28f), Theme.TEXT),
                UI.label(subtitle, Theme.inter(W.REGULAR, 15), Theme.TEXT_2));
        return Flex.row(16).justify(Flex.Justify.BETWEEN).align(Flex.Align.END).build(titles, right == null ? Flex.space(1) : right);
    }

    /** Search box that calls back 300 ms after the user stops typing. */
    static JComponent search(String placeholder, int width, Consumer<String> onChange) {
        UI.TextInput in = new UI.TextInput(placeholder);
        in.setFont(Theme.inter(W.REGULAR, 14));
        UI.Card box = new UI.Card(Theme.PAGE, 12, Theme.BORDER);
        box.setPreferredSize(new Dimension(width, 44));
        Flex.row(10).align(Flex.Align.CENTER).pad(0, 14, 0, 14).into(box,
                new JLabel(Icons.get("search", 18, Theme.MUTED)), Flex.grow(in));
        in.setPreferredSize(new Dimension(10, 22));
        UI.focusRing(in, box);
        Timer debounce = new Timer(300, e -> onChange.accept(in.getText().trim()));
        debounce.setRepeats(false);
        in.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { debounce.restart(); }
            public void removeUpdate(DocumentEvent e) { debounce.restart(); }
            public void changedUpdate(DocumentEvent e) { debounce.restart(); }
        });
        return box;
    }

    /** Segmented filter sized to its labels. */
    static UI.Segmented filter(String[] options, int selected, int width, Consumer<String> onChange) {
        UI.Segmented s = new UI.Segmented(options, selected) {
            @Override public Dimension getPreferredSize() { return new Dimension(width, 44); }
        };
        s.onChange(onChange);
        return s;
    }

    /** Dashboard number tile. */
    static final class Stat extends UI.Card {
        private final JLabel value = UI.label("—", Theme.tracking(Theme.poppins(W.SEMIBOLD, 30), -0.3f), Theme.TEXT);
        private final JLabel sub = UI.label(" ", Theme.inter(W.REGULAR, 13), Theme.MUTED);
        Stat(String icon, String label, Color tint, Color tintSoft) {
            super(Theme.PAGE, 16, Theme.BORDER);
            UI.Card tile = new UI.Card(tintSoft, 12, null);
            tile.setLayout(new GridBagLayout());
            tile.add(new JLabel(Icons.get(icon, 22, tint, 1.8f)));
            tile.setPreferredSize(new Dimension(44, 44));
            Flex.column(14).align(Flex.Align.STRETCH).pad(20).into(this,
                    Flex.row(12).align(Flex.Align.CENTER).build(tile, UI.label(label, Theme.inter(W.MEDIUM, 14), Theme.TEXT_2)),
                    Flex.column(2).build(value, sub));
        }
        void set(String v, String s, Color subColor) {
            value.setText(v);
            sub.setText(s);
            sub.setForeground(subColor);
        }
    }

    static UI.Pill priorityPill(String p) {
        return switch (p == null ? "" : p) {
            case "High" -> UI.status("High", Theme.DANGER, Theme.DANGER_SOFT, Theme.DANGER);
            case "Low" -> UI.status("Low", Theme.TEXT_2, Theme.CANVAS, Theme.MUTED);
            default -> UI.status("Medium", Theme.WARNING, Theme.WARNING_SOFT, Theme.WARNING_DOT);
        };
    }

    static JComponent empty(String text) {
        JLabel l = UI.label(text, Theme.inter(W.REGULAR, 14), Theme.MUTED);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        return Flex.row(0).justify(Flex.Justify.CENTER).pad(24).build(l);
    }

    static void info(Component parent, String title, String msg) {
        JOptionPane.showMessageDialog(parent, msg, title, JOptionPane.INFORMATION_MESSAGE);
    }

    static void error(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Something went wrong", JOptionPane.ERROR_MESSAGE);
    }
}
