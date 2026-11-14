package ph.ue.comlab.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * Line icons drawn with Java2D on a 24x24 grid (same style as the Figma icons).
 *
 * Want the exact Figma icons instead? Export them from Figma as PNG @2x and save them as
 * src/main/resources/icons/&lt;name&gt;.png (e.g. lock.png, eye.png). If a PNG exists it is used.
 */
public final class Icons {
    private Icons() {}

    public static Icon get(String name, int size, Color color) {
        Image png = loadPng(name);
        if (png != null) return new ImageIcon(png.getScaledInstance(size, size, Image.SCALE_SMOOTH));
        return new Drawn(name, size, color, 2f);
    }

    public static Icon get(String name, int size, Color color, float stroke) {
        Image png = loadPng(name);
        if (png != null) return new ImageIcon(png.getScaledInstance(size, size, Image.SCALE_SMOOTH));
        return new Drawn(name, size, color, stroke);
    }

    private static Image loadPng(String name) {
        try (InputStream in = Icons.class.getResourceAsStream("/icons/" + name + ".png")) {
            if (in == null) return null;
            BufferedImage img = ImageIO.read(in);
            return img;
        } catch (Exception e) {
            return null;
        }
    }

    /** Paints a named icon directly (used by custom-painted components). */
    public static void paint(Graphics2D g0, String name, double x, double y, double size, Color color, float stroke) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.hq(g);
        g.translate(x, y);
        g.scale(size / 24.0, size / 24.0);
        g.setColor(color);
        g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        draw(g, name);
        g.dispose();
    }

    private static final class Drawn implements Icon {
        final String name; final int size; final Color color; final float stroke;
        Drawn(String n, int s, Color c, float st) { name = n; size = s; color = c; stroke = st; }
        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            paint((Graphics2D) g, name, x, y, size, color, stroke);
        }
    }

    private static void draw(Graphics2D g, String name) {
        switch (name) {
            case "idcard" -> {
                g.draw(rr(2, 5, 20, 14, 2.5));
                g.draw(rr(5.5, 9, 5, 5, 1));
                g.draw(line(13.5, 10, 18.5, 10));
                g.draw(line(13.5, 13.5, 17, 13.5));
            }
            case "user" -> {
                g.draw(new Ellipse2D.Double(8, 3, 8, 8));
                g.draw(arc(4, 14, 16, 14, 0, 180));
            }
            case "lock" -> {
                g.draw(rr(4, 11, 16, 10, 2.5));
                Path2D p = new Path2D.Double();
                p.moveTo(8, 11); p.lineTo(8, 7.5); p.curveTo(8, 2.5, 16, 2.5, 16, 7.5); p.lineTo(16, 11);
                g.draw(p);
            }
            case "eye" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(2, 12); p.curveTo(5, 5.5, 19, 5.5, 22, 12); p.curveTo(19, 18.5, 5, 18.5, 2, 12); p.closePath();
                g.draw(p);
                g.draw(new Ellipse2D.Double(9, 9, 6, 6));
            }
            case "eye-off" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(2, 12); p.curveTo(5, 5.5, 19, 5.5, 22, 12); p.curveTo(19, 18.5, 5, 18.5, 2, 12); p.closePath();
                g.draw(p);
                g.draw(new Ellipse2D.Double(9, 9, 6, 6));
                g.draw(line(3, 3, 21, 21));
            }
            case "help" -> {
                g.draw(new Ellipse2D.Double(2, 2, 20, 20));
                Path2D p = new Path2D.Double();
                p.moveTo(9.1, 9); p.curveTo(9.6, 6.2, 14.6, 6.4, 14.6, 9.4); p.curveTo(14.6, 11.4, 12, 11.6, 12, 13.6);
                g.draw(p);
                g.fill(new Ellipse2D.Double(11.1, 16.3, 1.8, 1.8));
            }
            case "check" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(5, 12.5); p.lineTo(10, 17.5); p.lineTo(19.5, 7);
                g.draw(p);
            }
            case "arrow-right" -> {
                g.draw(line(5, 12, 19, 12));
                Path2D p = new Path2D.Double();
                p.moveTo(13, 6); p.lineTo(19, 12); p.lineTo(13, 18);
                g.draw(p);
            }
            case "monitor" -> {
                g.draw(rr(2.5, 3.5, 19, 13, 2));
                g.draw(line(8, 20.5, 16, 20.5));
                g.draw(line(12, 16.5, 12, 20.5));
            }
            case "keyboard" -> {
                g.draw(rr(2, 6, 20, 12, 2));
                for (int i = 0; i < 5; i++) g.fill(new Ellipse2D.Double(5.4 + i * 3, 9.4, 1.3, 1.3));
                for (int i = 0; i < 4; i++) g.fill(new Ellipse2D.Double(6.9 + i * 3, 12.1, 1.3, 1.3));
                g.draw(line(8, 15, 16, 15));
            }
            case "mouse" -> {
                g.draw(rr(6, 3, 12, 18, 6));
                g.draw(line(12, 7, 12, 10));
            }
            case "chevron-down" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(6, 9); p.lineTo(12, 15); p.lineTo(18, 9);
                g.draw(p);
            }
            case "upload" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(3, 15); p.lineTo(3, 19); p.quadTo(3, 21, 5, 21); p.lineTo(19, 21); p.quadTo(21, 21, 21, 19); p.lineTo(21, 15);
                g.draw(p);
                Path2D a = new Path2D.Double();
                a.moveTo(7, 8); a.lineTo(12, 3); a.lineTo(17, 8);
                g.draw(a);
                g.draw(line(12, 3, 12, 15));
            }
            case "alert" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(10.3, 3.9); p.quadTo(12, 1.2, 13.7, 3.9); p.lineTo(21.6, 17.6); p.quadTo(23, 20.5, 19.9, 20.5);
                p.lineTo(4.1, 20.5); p.quadTo(1, 20.5, 2.4, 17.6); p.closePath();
                g.draw(p);
                g.draw(line(12, 9, 12, 13));
                g.fill(new Ellipse2D.Double(11.1, 16, 1.8, 1.8));
            }
            case "clock" -> {
                g.draw(new Ellipse2D.Double(2, 2, 20, 20));
                Path2D p = new Path2D.Double();
                p.moveTo(12, 6.5); p.lineTo(12, 12); p.lineTo(15.5, 14);
                g.draw(p);
            }
            case "logout" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(9, 21); p.lineTo(5, 21); p.quadTo(3, 21, 3, 19); p.lineTo(3, 5); p.quadTo(3, 3, 5, 3); p.lineTo(9, 3);
                g.draw(p);
                Path2D a = new Path2D.Double();
                a.moveTo(16, 7); a.lineTo(21, 12); a.lineTo(16, 17);
                g.draw(a);
                g.draw(line(21, 12, 9, 12));
            }
            case "send" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(22, 2); p.lineTo(15, 22); p.lineTo(11, 13); p.lineTo(2, 9); p.closePath();
                g.draw(p);
                g.draw(line(22, 2, 11, 13));
            }
            case "grid" -> {
                g.draw(rr(3, 3, 7.5, 7.5, 1.5)); g.draw(rr(13.5, 3, 7.5, 7.5, 1.5));
                g.draw(rr(3, 13.5, 7.5, 7.5, 1.5)); g.draw(rr(13.5, 13.5, 7.5, 7.5, 1.5));
            }
            case "list" -> {
                for (int i = 0; i < 3; i++) {
                    g.draw(line(9, 6 + i * 6, 21, 6 + i * 6));
                    g.fill(new Ellipse2D.Double(3.2, 5.2 + i * 6, 1.8, 1.8));
                }
            }
            case "users" -> {
                g.draw(new Ellipse2D.Double(5, 4, 7, 7));
                g.draw(arc(2, 14, 13, 12, 0, 180));
                Path2D p = new Path2D.Double();
                p.moveTo(15.5, 4.3); p.curveTo(19, 5, 19, 10, 15.5, 10.7);
                g.draw(p);
                Path2D q = new Path2D.Double();
                q.moveTo(17.5, 14.4); q.curveTo(20.3, 15.2, 22, 17, 22, 20);
                g.draw(q);
            }
            case "refresh" -> {
                g.draw(arc(4, 4, 16, 16, 30, 280));
                Path2D p = new Path2D.Double();
                p.moveTo(20, 3.5); p.lineTo(19.3, 8.4); p.lineTo(14.5, 7.4);
                g.draw(p);
            }
            case "search" -> {
                g.draw(new Ellipse2D.Double(3, 3, 14, 14));
                g.draw(line(15, 15, 21, 21));
            }
            case "plus" -> {
                g.draw(line(12, 5, 12, 19));
                g.draw(line(5, 12, 19, 12));
            }
            case "key" -> {
                g.draw(new Ellipse2D.Double(2.5, 11, 9, 9));
                g.draw(line(10, 12.5, 20, 2.5));
                g.draw(line(16, 6.5, 19, 9.5));
                g.draw(line(18.5, 4, 21, 6.5));
            }
            case "x" -> {
                g.draw(line(6, 6, 18, 18));
                g.draw(line(18, 6, 6, 18));
            }
            case "download" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(3, 15); p.lineTo(3, 19); p.quadTo(3, 21, 5, 21); p.lineTo(19, 21); p.quadTo(21, 21, 21, 19); p.lineTo(21, 15);
                g.draw(p);
                Path2D a = new Path2D.Double();
                a.moveTo(7, 10); a.lineTo(12, 15); a.lineTo(17, 10);
                g.draw(a);
                g.draw(line(12, 15, 12, 3));
            }
            case "image" -> {
                g.draw(rr(3, 3, 18, 18, 2.5));
                g.draw(new Ellipse2D.Double(7, 7, 3.5, 3.5));
                Path2D p = new Path2D.Double();
                p.moveTo(21, 15); p.lineTo(16, 10); p.lineTo(5, 21);
                g.draw(p);
            }
            case "shield" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(12, 22); p.curveTo(12, 22, 20, 18, 20, 12); p.lineTo(20, 5); p.lineTo(12, 2); p.lineTo(4, 5); p.lineTo(4, 12);
                p.curveTo(4, 18, 12, 22, 12, 22);
                g.draw(p);
            }
            default -> g.draw(new Ellipse2D.Double(4, 4, 16, 16));
        }
    }

    private static Shape rr(double x, double y, double w, double h, double r) {
        return new RoundRectangle2D.Double(x, y, w, h, r * 2, r * 2);
    }
    private static Shape line(double x1, double y1, double x2, double y2) { return new Line2D.Double(x1, y1, x2, y2); }
    private static Shape arc(double x, double y, double w, double h, double start, double ext) {
        return new Arc2D.Double(x, y, w, h, start, ext, Arc2D.OPEN);
    }
}
