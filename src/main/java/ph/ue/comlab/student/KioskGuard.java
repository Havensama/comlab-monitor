package ph.ue.comlab.student;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Keeps the login window covering the whole screen so the PC can't be used without logging in.
 *
 * What pure Java CAN do (all done here):
 *   - fullscreen, no title bar, no close button, always on top, covers the taskbar
 *   - Alt+F4 / the window close request is ignored
 *   - if the student Alt+Tabs, presses the Windows key or clicks away, the window is pulled back
 *     to the front within 0.25 s (Windows itself shows the switcher / Start menu for a moment)
 *   - minimising is undone immediately
 *
 * What NO normal app can block: Ctrl+Alt+Del (Windows reserves it). Cover that with the
 * Group Policy steps in the README (disable Task Manager, Lock, Switch user, Sign out).
 * The start-student script also relaunches the app if it is ever killed.
 */
public class KioskGuard {

    private final JFrame frame;
    private final boolean lock;
    private final Timer watchdog;
    private boolean engaged;

    public KioskGuard(JFrame frame, boolean lock, Runnable secretExit) {
        this.frame = frame;
        this.lock = lock;

        // locked: closing is ignored. Dev mode (kiosk.lock=false): normal window you can close.
        frame.setDefaultCloseOperation(lock ? WindowConstants.DO_NOTHING_ON_CLOSE : WindowConstants.EXIT_ON_CLOSE);
        if (lock) {
            frame.setUndecorated(true);
            frame.setType(Window.Type.NORMAL);
            frame.setAutoRequestFocus(true);
        }

        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowIconified(WindowEvent e) {
                if (engaged && lock) SwingUtilities.invokeLater(() -> frame.setExtendedState(JFrame.NORMAL));
            }
            @Override public void windowDeactivated(WindowEvent e) {
                // focus moving to one of our own dialogs (message box, file chooser, dropdown) is fine
                Window to = e.getOppositeWindow();
                if (to != null && isOurs(to)) return;
                if (engaged && lock) SwingUtilities.invokeLater(KioskGuard.this::pullToFront);
            }
        });

        // Swallow keys that try to escape, and listen for the secret exit (Ctrl+Shift+F12).
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_F12
                    && e.isControlDown() && e.isShiftDown()) {
                SwingUtilities.invokeLater(secretExit);
                return true;
            }
            if (!engaged || !lock) return false;
            int k = e.getKeyCode();
            if (k == KeyEvent.VK_WINDOWS || k == KeyEvent.VK_CONTEXT_MENU) return true;
            if (k == KeyEvent.VK_F4 && e.isAltDown()) return true;
            if (k == KeyEvent.VK_ESCAPE && e.isControlDown()) return true;
            if (k == KeyEvent.VK_TAB && e.isAltDown()) return true;
            return false;
        });

        watchdog = new Timer(250, e -> {
            if (!engaged || !lock) return;
            // our own dialogs (message boxes, file chooser) are allowed to have focus
            Window active = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
            if (active == null || (active != frame && !isOurs(active))) pullToFront();
            if ((frame.getExtendedState() & Frame.ICONIFIED) != 0) frame.setExtendedState(JFrame.NORMAL);
            Rectangle screen = screenBounds();
            if (!frame.getBounds().equals(screen)) frame.setBounds(screen);
        });
    }

    private boolean isOurs(Window w) {
        for (Window o = w; o != null; o = o.getOwner()) if (o == frame) return true;
        return false;
    }

    private void pullToFront() {
        if (!engaged) return;
        Window active = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        if (active != null && active != frame && isOurs(active)) return;
        if (!frame.isVisible()) frame.setVisible(true);
        // toggling always-on-top helps Windows let us take focus back
        frame.setAlwaysOnTop(false);
        frame.setAlwaysOnTop(true);
        frame.toFront();
        frame.requestFocus();
    }

    private static Rectangle screenBounds() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                .getDefaultConfiguration().getBounds();
    }

    /** Lock the screen (login / workstation check). */
    public void engage() {
        engaged = true;
        if (lock) {
            frame.setBounds(screenBounds());
            frame.setAlwaysOnTop(true);
        } else {
            frame.setSize(1440, 900);
            frame.setLocationRelativeTo(null);
        }
        frame.setVisible(true);
        frame.toFront();
        watchdog.start();
    }

    /** Unlock (student is using the PC). */
    public void release() {
        engaged = false;
        watchdog.stop();
        frame.setAlwaysOnTop(false);
        frame.setVisible(false);
    }

    public boolean isEngaged() { return engaged; }
}
