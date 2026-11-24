package ph.ue.comlab;

import ph.ue.comlab.model.Models.*;
import ph.ue.comlab.student.WorkstationCheckPanel;
import ph.ue.comlab.ui.*;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Quick UI preview — opens the student screens with fake data, no database needed.
 * Run this class directly in IntelliJ to see the Frontend.
 */
public class UIPreview {
    public static void main(String[] args) {
        // Load config so AppConfig.pcLab() etc. work
        AppConfig.load();

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            String[] options = {"Login Screen", "Workstation Check", "Admin Login"};
            int choice = JOptionPane.showOptionDialog(null,
                    "Which screen do you want to preview?",
                    "UI Preview",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, options, options[0]);

            switch (choice) {
                case 0 -> showStudentLogin();
                case 1 -> showWorkstationCheck();
                case 2 -> showAdminLogin();
            }
        });
    }

    private static void showStudentLogin() {
        JFrame frame = new JFrame("Preview — Student Login");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1200, 760));

        LoginPanel login = new LoginPanel(LoginPanel.Copy.student());
        login.onLogin((id, pw) -> {
            JOptionPane.showMessageDialog(frame,
                    "Login clicked!\nStudent ID: " + id + "\nPassword: " + pw,
                    "Preview", JOptionPane.INFORMATION_MESSAGE);
        });

        frame.setContentPane(login);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void showWorkstationCheck() {
        JFrame frame = new JFrame("Preview — Workstation Check");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1200, 760));

        // Fake data — no database
        Student student = new Student("20241120810", "Jay Lord Gaspi", "BSCpE");
        LabSession session = new LabSession(1, LocalDateTime.now(), 0);
        List<Report> fakeReports = List.of(
                new Report(1, "PC-14", "20231234567", "Maria Santos",
                        "Monitor flickers on startup", "Medium",
                        "Monitor flickers when turning on", "IN_PROGRESS", "STUDENT",
                        LocalDateTime.now().minusDays(1), false)
        );

        WorkstationCheckPanel check = new WorkstationCheckPanel(student, session, fakeReports);
        check.setListener(new WorkstationCheckPanel.Listener() {
            @Override
            public void submitConcern(ConcernForm.Concern concern) {
                JOptionPane.showMessageDialog(frame, "Concern submitted!\n" + concern.category() + ": " + concern.description());
            }

            @Override
            public void startSession(boolean m, boolean k, boolean mouse, ConcernForm.Concern c) {
                JOptionPane.showMessageDialog(frame, "Session started!\nMonitor: " + m + "\nKeyboard: " + k + "\nMouse: " + mouse);
            }

            @Override
            public void logout() {
                JOptionPane.showMessageDialog(frame, "Logout clicked!");
            }
        });

        frame.setContentPane(check);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void showAdminLogin() {
        JFrame frame = new JFrame("Preview — Admin Login");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1200, 760));

        LoginPanel login = new LoginPanel(LoginPanel.Copy.admin());
        login.onLogin((user, pw) -> {
            JOptionPane.showMessageDialog(frame,
                    "Login clicked!\nUsername: " + user + "\nPassword: " + pw,
                    "Preview", JOptionPane.INFORMATION_MESSAGE);
        });

        frame.setContentPane(login);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
