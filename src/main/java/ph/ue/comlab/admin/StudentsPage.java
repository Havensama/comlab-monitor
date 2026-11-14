package ph.ue.comlab.admin;

import ph.ue.comlab.db.Database;
import ph.ue.comlab.db.StudentDao;
import ph.ue.comlab.model.Models.Student;
import ph.ue.comlab.ui.*;
import ph.ue.comlab.ui.DataTable.Column;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/** Admin "Students": who can log in to the lab PCs. Add students and reset passwords. */
class StudentsPage implements AdminUI.Page<List<Student>> {

    private final StudentDao dao = new StudentDao();
    private final DataTable<Student> table;
    private final UI.SecondaryButton reset = new UI.SecondaryButton("Reset password", "key", 14, 16);
    private final JComponent view;
    private final Runnable refresh;
    private volatile String search = "";

    StudentsPage(Runnable refresh) {
        this.refresh = refresh;
        table = new DataTable<>(List.of(
                Column.text("Name", 260, Student::fullName),
                Column.text("Student no.", 160, Student::studentNo),
                Column.text("Course", 120, Student::course)
        ), Student::studentNo, "No students found.");

        reset.setEnabled(false);
        table.onSelect(s -> reset.setEnabled(s != null));
        reset.addActionListener(e -> resetPassword());

        UI.PrimaryButton add = new UI.PrimaryButton("Add student", "plus").compact();
        add.addActionListener(e -> addStudent());

        JPanel toolbar = Flex.row(12).align(Flex.Align.START).build(
                AdminUI.search("Search name or student no.", 320, q -> { search = q; refresh.run(); }),
                Flex.spring(), reset, add);

        JPanel page = Flex.column(20).align(Flex.Align.STRETCH).pad(32, 40, 32, 40).build(
                AdminUI.header("ACCOUNTS", "Students", "Students listed here can log in to any lab PC.", null),
                toolbar, Flex.grow(table));
        page.setOpaque(true);
        page.setBackground(Theme.CANVAS);
        view = page;
    }

    @Override public JComponent view() { return view; }
    @Override public List<Student> load() throws Exception { return dao.all(search); }
    @Override public void show(List<Student> rows) { table.setRows(rows); }

    private void addStudent() {
        UI.InputField no = new UI.InputField("Student no.", "idcard", new UI.TextInput("e.g. 20241234567"), null);
        UI.InputField name = new UI.InputField("Full name", "user", new UI.TextInput("e.g. Juan Dela Cruz"), null);
        UI.InputField course = new UI.InputField("Course", "list", new UI.TextInput("e.g. BSCpE"), null);
        UI.PasswordInput pwIn = new UI.PasswordInput("At least 4 characters");
        UI.InputField pw = new UI.InputField("Password", "lock", pwIn, UI.eyeToggle(pwIn));
        JPanel form = UI.fixedColumn(380, 14, no, name, course, pw);
        form.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        while (true) {
            int ok = JOptionPane.showConfirmDialog(view, form, "Add student", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (ok != JOptionPane.OK_OPTION) return;
            String password = new String(pwIn.getPassword());
            String problem = no.text().isEmpty() ? "Student no. is required."
                    : name.text().isEmpty() ? "Full name is required."
                    : password.length() < 4 ? "Password must be at least 4 characters." : null;
            if (problem != null) { AdminUI.error(view, problem); continue; }
            try {
                dao.add(no.text(), name.text(), course.text().isEmpty() ? null : course.text(), password);
                refresh.run();
                AdminUI.info(view, "Add student", name.text() + " can now log in with student no. " + no.text() + ".");
                return;
            } catch (java.sql.SQLIntegrityConstraintViolationException dup) {
                AdminUI.error(view, "A student with that number already exists.");
            } catch (Exception e) {
                AdminUI.error(view, Database.explain(e));
                return;
            }
        }
    }

    private void resetPassword() {
        Student s = table.selected();
        if (s == null) return;
        String pw = UI.askPassword(view, "Reset password", "New password for " + s.fullName() + ":");
        if (pw == null) return;
        if (pw.length() < 4) { AdminUI.error(view, "Password must be at least 4 characters."); return; }
        try {
            dao.resetPassword(s.studentNo(), pw);
            AdminUI.info(view, "Reset password", "Password updated for " + s.fullName() + ".");
        } catch (Exception e) {
            AdminUI.error(view, Database.explain(e));
        }
    }
}
