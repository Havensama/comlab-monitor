package ph.ue.comlab.ui;

import ph.ue.comlab.ui.Theme.W;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/** A styled JTable for lists of records (sessions, reports, students). */
public class DataTable<T> extends UI.Card {

    /** How to show one column. If {@code pill} is set, the cell is drawn as a status pill. */
    public record Column<T>(String title, int width, Function<T, Object> value, Function<T, UI.Pill> pill) {
        public static <T> Column<T> text(String title, int width, Function<T, Object> value) {
            return new Column<>(title, width, value, null);
        }
        public static <T> Column<T> pill(String title, int width, Function<T, UI.Pill> pill) {
            return new Column<>(title, width, t -> pill.apply(t).getText(), pill);
        }
    }

    private final List<Column<T>> columns;
    private final Function<T, Object> key;
    private final List<T> rows = new ArrayList<>();
    private final JTable table;
    private final Model model = new Model();
    private final JLabel empty;
    private Consumer<T> onSelect = t -> {};

    public DataTable(List<Column<T>> columns, Function<T, Object> key, String emptyText) {
        super(Theme.PAGE, 16, Theme.BORDER);
        this.columns = columns;
        this.key = key;

        table = new JTable(model);
        table.setRowHeight(48);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(Theme.BORDER);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(Theme.ORANGE_SOFT);
        table.setSelectionForeground(Theme.TEXT);
        table.setFillsViewportHeight(true);
        table.setFocusable(false);
        table.setAutoCreateRowSorter(false);
        table.setBackground(Theme.PAGE);

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(10, 40));
        header.setDefaultRenderer(new HeaderRenderer());

        for (int i = 0; i < columns.size(); i++) {
            TableColumn tc = table.getColumnModel().getColumn(i);
            if (columns.get(i).width() > 0) tc.setPreferredWidth(columns.get(i).width());
            tc.setCellRenderer(columns.get(i).pill() != null ? new PillRenderer(i) : new TextRenderer());
        }

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onSelect.accept(selected());
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Theme.PAGE);
        sp.setOpaque(false);
        UI.styleScrollBar(sp.getVerticalScrollBar());

        empty = UI.label(emptyText, Theme.inter(W.REGULAR, 14), Theme.MUTED);
        empty.setHorizontalAlignment(SwingConstants.CENTER);
        empty.setVisible(false);

        JLayeredPane layers = new JLayeredPane() {
            @Override public void doLayout() {
                for (Component c : getComponents()) c.setBounds(0, 0, getWidth(), getHeight());
            }
        };
        layers.add(sp, JLayeredPane.DEFAULT_LAYER);
        layers.add(empty, JLayeredPane.PALETTE_LAYER);

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(1, 1, 8, 1));
        add(layers);
    }

    /** Clip the header/rows to the rounded corners, then redraw the border on top. */
    @Override protected void paintChildren(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.clip(new java.awt.geom.RoundRectangle2D.Double(1, 1, getWidth() - 2, getHeight() - 2, 30, 30));
        super.paintChildren(g);
        g.dispose();
        Graphics2D b = (Graphics2D) g0.create();
        Theme.hq(b);
        b.setColor(Theme.BORDER);
        b.draw(new java.awt.geom.RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, 32, 32));
        b.dispose();
    }

    public JTable table() { return table; }

    public void onSelect(Consumer<T> c) { onSelect = c; }

    public T selected() {
        int r = table.getSelectedRow();
        return r < 0 || r >= rows.size() ? null : rows.get(r);
    }

    public List<T> rows() { return rows; }

    /** Replace the data but keep the same row selected (so the 5-second refresh doesn't lose your place). */
    public void setRows(List<T> data) {
        T sel = selected();
        Object selKey = sel == null ? null : key.apply(sel);
        rows.clear();
        rows.addAll(data);
        model.fireTableDataChanged();
        empty.setVisible(rows.isEmpty());
        if (selKey != null) {
            for (int i = 0; i < rows.size(); i++) {
                if (Objects.equals(key.apply(rows.get(i)), selKey)) {
                    table.getSelectionModel().setSelectionInterval(i, i);
                    break;
                }
            }
        }
    }

    private class Model extends AbstractTableModel {
        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return columns.size(); }
        @Override public String getColumnName(int c) { return columns.get(c).title(); }
        @Override public Object getValueAt(int r, int c) { return columns.get(c).value().apply(rows.get(r)); }
    }

    private static class HeaderRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, s, f, r, c);
            l.setText(String.valueOf(v).toUpperCase());
            l.setFont(Theme.tracking(Theme.inter(W.SEMIBOLD, 11), 0.6f));
            l.setForeground(Theme.TEXT_2);
            l.setBackground(Theme.CANVAS);
            l.setOpaque(true);
            l.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                    BorderFactory.createEmptyBorder(0, 16, 0, 8)));
            return l;
        }
    }

    private static class TextRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v == null ? "—" : v, s, false, r, c);
            l.setFont(c == 0 ? Theme.inter(W.MEDIUM, 14) : Theme.inter(W.REGULAR, 14));
            l.setForeground(v == null ? Theme.MUTED : (c == 0 ? Theme.TEXT : Theme.TEXT_2));
            l.setBackground(s ? Theme.ORANGE_SOFT : Theme.PAGE);
            l.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 8));
            return l;
        }
    }

    private class PillRenderer implements TableCellRenderer {
        private final int col;
        private final JPanel holder = new JPanel(null) {
            @Override public void doLayout() {
                for (Component k : getComponents()) {
                    Dimension d = k.getPreferredSize();
                    k.setBounds(16, (getHeight() - d.height) / 2, d.width, d.height);
                }
            }
        };
        PillRenderer(int col) { this.col = col; }
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            holder.removeAll();
            holder.setBackground(s ? Theme.ORANGE_SOFT : Theme.PAGE);
            holder.add(columns.get(col).pill().apply(rows.get(r)));
            return holder;
        }
    }
}
