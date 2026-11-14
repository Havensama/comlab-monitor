package ph.ue.comlab.ui;

import ph.ue.comlab.ui.Theme.W;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Files;

/**
 * Figma "Other concerns" card (node 9:181): category, priority, description and an optional photo.
 * Used on the Workstation Check page and in the "Report issue" dialog during a session.
 */
public class ConcernForm extends UI.Card {

    public static final String[] CATEGORIES = {"Hardware", "Peripheral", "Software", "Network", "Other"};
    public static final long MAX_PHOTO = 5L * 1024 * 1024;

    /** What the student typed. */
    public record Concern(String category, String priority, String description, byte[] photo, String photoName) {}

    private final UI.Select category = new UI.Select("Select a category", CATEGORIES);
    private final UI.Segmented priority = new UI.Segmented(new String[]{"Low", "Medium", "High"}, 1);
    private final UI.TextBox description =
            new UI.TextBox("e.g. \"Headset jack not working\" or \"No internet connection since 8 PM\"", 500, 120);
    private final JLabel photoTitle = UI.label("Attach a photo", Theme.inter(W.MEDIUM, 14), Theme.TEXT);
    private final JLabel photoSub = UI.label("PNG or JPG, up to 5 MB", Theme.inter(W.REGULAR, 12), Theme.MUTED);
    private final JLabel removePhoto;
    private byte[] photo;
    private String photoName;

    public ConcernForm(String title, boolean optionalPill, String subtitle) {
        super(Theme.PAGE, 16, Theme.BORDER);

        JPanel titleRow = Flex.row(10).align(Flex.Align.CENTER).build(
                UI.label(title, Theme.poppins(W.SEMIBOLD, 18), Theme.TEXT));
        if (optionalPill) titleRow.add(new UI.Pill("Optional", Theme.inter(W.SEMIBOLD, 12), Theme.TEXT_2, Theme.CANVAS, 4, 10));

        JPanel head = Flex.column(4).build(titleRow,
                new UI.WrapText(subtitle, Theme.inter(W.REGULAR, 14), Theme.TEXT_2, 1.25f, 900));

        JPanel row = Flex.row(16).align(Flex.Align.START).build(
                Flex.grow(Flex.column(8).align(Flex.Align.STRETCH).build(
                        Flex.hug(UI.label("Issue category", Theme.inter(W.MEDIUM, 14), Theme.TEXT)), category)),
                Flex.grow(Flex.column(8).align(Flex.Align.STRETCH).build(
                        Flex.hug(UI.label("Priority", Theme.inter(W.MEDIUM, 14), Theme.TEXT)), priority)));

        JPanel describe = Flex.column(8).align(Flex.Align.STRETCH).build(
                Flex.hug(UI.label("Describe the problem", Theme.inter(W.MEDIUM, 14), Theme.TEXT)), description);

        // ---- attach photo (dashed tile) ----
        UI.Card tile = new UI.Card(Theme.CANVAS, 10, null);
        tile.setLayout(new GridBagLayout());
        tile.add(new JLabel(Icons.get("upload", 18, Theme.TEXT_2)));
        tile.setPreferredSize(new Dimension(36, 36));
        removePhoto = UI.link("Remove", Theme.inter(W.SEMIBOLD, 13), Theme.DANGER, this::clearPhoto);
        removePhoto.setVisible(false);
        UI.Card attach = new UI.Card(null, 12, Theme.BORDER).dashed();
        Flex.row(12).align(Flex.Align.CENTER).pad(14, 16, 14, 16).into(attach,
                tile, Flex.grow(Flex.column(2).build(photoTitle, photoSub)), removePhoto);
        attach.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        attach.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { pickPhoto(); }
        });

        Flex.column(20).align(Flex.Align.STRETCH).pad(24).into(this, head, row, describe, attach);
    }

    private void pickPhoto() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Attach a photo");
        fc.setFileFilter(new FileNameExtensionFilter("Images (PNG, JPG)", "png", "jpg", "jpeg"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        if (f.length() > MAX_PHOTO) {
            JOptionPane.showMessageDialog(this, "That photo is larger than 5 MB. Please pick a smaller one.",
                    "Photo too large", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            photo = shrink(Files.readAllBytes(f.toPath()));
            photoName = f.getName();
            photoTitle.setText(photoName);
            photoSub.setText(String.format("%d KB · click to change", Math.max(1, photo.length / 1024)));
            removePhoto.setVisible(true);
            revalidate();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Couldn't read that file as a PNG or JPG image.", "Attach a photo", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Phone photos are often 3–5 MB, and XAMPP's MariaDB refuses anything over 1 MB by default.
     * So photos are resized to max 1280 px and saved as JPEG (usually 150–400 KB).
     */
    static byte[] shrink(byte[] original) throws java.io.IOException {
        java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(original));
        if (img == null) throw new java.io.IOException("not an image");
        double scale = Math.min(1.0, 1280.0 / Math.max(img.getWidth(), img.getHeight()));
        int w = Math.max(1, (int) (img.getWidth() * scale)), h = Math.max(1, (int) (img.getHeight() * scale));
        java.awt.image.BufferedImage out = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.drawImage(img, 0, 0, w, h, null);
        g.dispose();
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageWriter writer = javax.imageio.ImageIO.getImageWritersByFormatName("jpg").next();
        javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.82f);
        try (javax.imageio.stream.ImageOutputStream ios = javax.imageio.ImageIO.createImageOutputStream(bos)) {
            writer.setOutput(ios);
            writer.write(null, new javax.imageio.IIOImage(out, null, null), param);
        } finally {
            writer.dispose();
        }
        return bos.toByteArray();
    }

    private void clearPhoto() {
        photo = null;
        photoName = null;
        photoTitle.setText("Attach a photo");
        photoSub.setText("PNG or JPG, up to 5 MB");
        removePhoto.setVisible(false);
        revalidate();
    }

    /** True if the student hasn't typed or picked anything. */
    public boolean isBlank() {
        return category.value() == null && description.text().isEmpty() && photo == null;
    }

    /** null if OK, otherwise what's missing. */
    public String validateInput() {
        if (category.value() == null) return "Please choose an issue category.";
        if (description.text().length() < 5) return "Please describe the problem in a few words.";
        return null;
    }

    public Concern value() {
        return new Concern(category.value(), priority.value(), description.text(), photo, photoName);
    }

    public void clear() {
        category.setValue(null);
        priority.setSelected(1);
        description.clear();
        clearPhoto();
    }
}
