package ph.ue.comlab.db;

import ph.ue.comlab.model.Models.Admin;
import ph.ue.comlab.model.Models.Student;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Accounts: student login, admin login, and the admin "Students" tab. */
public class StudentDao {

    public Optional<Student> login(String studentNo, String password) throws SQLException {
        String sql = "SELECT student_no, full_name, course FROM students WHERE student_no = ? AND password_hash = ?";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentNo);
            ps.setString(2, Database.sha256(password));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(new Student(rs.getString(1), rs.getString(2), rs.getString(3)));
            }
        }
        return Optional.empty();
    }

    public Optional<Admin> adminLogin(String username, String password) throws SQLException {
        String sql = "SELECT username, full_name FROM admins WHERE username = ? AND password_hash = ?";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, Database.sha256(password));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(new Admin(rs.getString(1), rs.getString(2)));
            }
        }
        return Optional.empty();
    }

    public List<Student> all(String search) throws SQLException {
        String sql = "SELECT student_no, full_name, course FROM students "
                + "WHERE student_no LIKE ? OR full_name LIKE ? ORDER BY full_name";
        List<Student> out = new ArrayList<>();
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            String q = "%" + (search == null ? "" : search.trim()) + "%";
            ps.setString(1, q);
            ps.setString(2, q);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new Student(rs.getString(1), rs.getString(2), rs.getString(3)));
            }
        }
        return out;
    }

    public void add(String studentNo, String name, String course, String password) throws SQLException {
        String sql = "INSERT INTO students (student_no, full_name, course, password_hash) VALUES (?,?,?,?)";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentNo);
            ps.setString(2, name);
            ps.setString(3, course);
            ps.setString(4, Database.sha256(password));
            ps.executeUpdate();
        }
    }

    public void resetPassword(String studentNo, String newPassword) throws SQLException {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("UPDATE students SET password_hash = ? WHERE student_no = ?")) {
            ps.setString(1, Database.sha256(newPassword));
            ps.setString(2, studentNo);
            ps.executeUpdate();
        }
    }
}
