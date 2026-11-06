package ph.ue.comlab.student;

import ph.ue.comlab.ui.*;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/** "Report issue" during a session — the same Other concerns form, in a small window. */
public class ReportDialog extends JDialog {

    private final ConcernForm form = new ConcernForm("Report an issue", false,
            "Tell the lab team what's wrong with this PC. A lab assistant will check it.");
    private final UI.Banner banner = new UI.Banner();
    private final UI.PrimaryButton send = new UI.PrimaryButton("Send report", "send");

    public ReportDialog(Consumer<ConcernForm.Concern> onSend) {
        super((Frame) null, "Report an issue", true);
        setAlwaysOnTop(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        UI.SecondaryButton cancel = new UI.SecondaryButton("Cancel", null);
        cancel.addActionListener(e -> dispose());
        send.addActionListener(e -> {
            String problem = form.validateInput();
            if (problem != null) { banner.error(problem); fit(); return; }
            send.setEnabled(false);
            onSend.accept(form.value());
        });

        JPanel buttons = Flex.row(12).justify(Flex.Justify.END).align(Flex.Align.START).build(cancel, send);
        JPanel root = Flex.column(16).align(Flex.Align.STRETCH).pad(20).build(banner, form, buttons);
        root.setOpaque(true);
        root.setBackground(Theme.CANVAS);
        setContentPane(root);
        fit();
        setLocationRelativeTo(null);
    }

    private void fit() {
        JComponent root = (JComponent) getContentPane();
        pack(); // creates the native window so insets are known
        Insets in = getInsets();
        int w = 720;
        setSize(w + in.left + in.right, ((Flex) root.getLayout()).sizeForWidth(root, w).height + in.top + in.bottom);
        validate();
    }

    public void failed(String msg) {
        send.setEnabled(true);
        banner.error(msg);
        fit();
    }
}
