package ph.ue.comlab;

import ph.ue.comlab.admin.AdminApp;
import ph.ue.comlab.student.StudentApp;
import ph.ue.comlab.ui.Theme;

import javax.swing.*;

/**
 * Entry point. Runs the student kiosk or the admin console:
 *   java -jar comlab.jar student
 *   java -jar comlab.jar admin
 * Without an argument it uses app.mode from config.properties.
 */
public class Launcher {
    public static void main(String[] args) {
        AppConfig.load();
        String mode = args.length > 0 ? args[0] : AppConfig.get("app.mode", "student");
        Theme.init();
        if (mode.equalsIgnoreCase("admin")) Theme.useAdminTheme();
        SwingUtilities.invokeLater(() -> {
            if (mode.equalsIgnoreCase("admin")) new AdminApp().start();
            else new StudentApp().start();
        });
    }
}
