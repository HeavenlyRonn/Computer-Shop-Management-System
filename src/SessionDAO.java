import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SessionDAO {

    public static void startSession(int pcId, int hours, double amount, int customerId) {
        String sql = "INSERT INTO sessions (pc_id, customer_id, hours, amount, start_time, end_time, status, is_archived) VALUES (?, ?, ?, ?, NOW(), DATE_ADD(NOW(), INTERVAL ? HOUR), 'active', 0)";
        try (Connection conn = connectDB.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, pcId);
            if (customerId == -1) {
                pst.setNull(2, java.sql.Types.INTEGER);
            } else {
                pst.setInt(2, customerId);
            }
            pst.setInt(3, hours);
            pst.setDouble(4, amount);
            pst.setInt(5, hours);
            pst.executeUpdate();
            try (ResultSet keys = pst.getGeneratedKeys()) {
                if (keys.next()) {
                    logEvent(keys.getInt(1), "START", "Session started");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void stopSession(int sessionId) {
        String sql = "UPDATE sessions SET status='done', is_archived=1 WHERE id=?";
        try (Connection conn = connectDB.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, sessionId);
            pst.executeUpdate();
            logEvent(sessionId, "STOP", "Session stopped");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void extendSession(int sessionId, int extraHours, double extraAmount) {
        String sql = "UPDATE sessions SET end_time = DATE_ADD(end_time, INTERVAL ? HOUR), amount = amount + ?, hours = hours + ? WHERE id=?";
        try (Connection conn = connectDB.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, extraHours);
            pst.setDouble(2, extraAmount);
            pst.setInt(3, extraHours);
            pst.setInt(4, sessionId);
            pst.executeUpdate();
            logEvent(sessionId, "EXTEND", "Extended by " + extraHours + " hour(s)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Session getActiveSessionByPc(int pcId) {
        String sql = "SELECT * FROM sessions WHERE pc_id=? AND status='active' AND is_archived=0 ORDER BY id DESC LIMIT 1";
        try (Connection conn = connectDB.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, pcId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<Session> getAllSessions() {
        List<Session> sessions = new ArrayList<>();
        String sql = "SELECT * FROM sessions ORDER BY id DESC";
        try (Connection conn = connectDB.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                sessions.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sessions;
    }

    // Note: bulk expiry is not logged per-row to avoid N+1 queries.
    // A production system would use a DB trigger or scheduled job for this.
    public static void autoStopExpired() {
        String sql = "UPDATE sessions SET status='done', is_archived=1 WHERE status='active' AND is_archived=0 AND end_time <= NOW()";
        try (Connection conn = connectDB.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static double getTodaySalesTotal() {
        String sql = "SELECT COALESCE(SUM(amount), 0) AS total FROM sessions WHERE DATE(start_time) = CURDATE()";
        try (Connection conn = connectDB.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
                return rs.getDouble("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public static void revertLastExtend(int sessionId, int extraHours, double extraAmount) {
        String sql = "UPDATE sessions SET end_time = DATE_SUB(end_time, INTERVAL ? HOUR), hours = hours - ?, amount = amount - ? WHERE id = ?";
        try (Connection conn = connectDB.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, extraHours);
            pst.setInt(2, extraHours);
            pst.setDouble(3, extraAmount);
            pst.setInt(4, sessionId);
            pst.executeUpdate();
            logEvent(sessionId, "UNDO_EXTEND", "Reverted " + extraHours + " hour(s)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static int registerOrGetCustomer(String name, String email, String contact) {
        if (name == null || name.trim().isEmpty()) {
            return -1;
        }
        name = name.trim();
        // Dedup rule: only when email is provided.
        if (email != null && !email.trim().isEmpty()) {
            email = email.trim();
            String findSql = "SELECT customer_id FROM customers WHERE email = ?";
            try (Connection conn = connectDB.getConnection();
                 PreparedStatement pst = conn.prepareStatement(findSql)) {
                pst.setString(1, email);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("customer_id");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        // Insert new customer
        String insSql = "INSERT INTO customers (customer_name, contact_number, email) VALUES (?, ?, ?)";
        try (Connection conn = connectDB.getConnection();
             PreparedStatement pst = conn.prepareStatement(insSql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, name);
            if (contact == null || contact.trim().isEmpty()) {
                pst.setNull(2, java.sql.Types.VARCHAR);
            } else {
                pst.setString(2, contact.trim());
            }
            if (email == null || email.trim().isEmpty()) {
                pst.setNull(3, java.sql.Types.VARCHAR);
            } else {
                pst.setString(3, email.trim());
            }
            pst.executeUpdate();
            try (ResultSet keys = pst.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static List<String> getRecentLogs() {
        List<String> logs = new ArrayList<>();
        String sql = "SELECT log_id, session_id, action, details, log_time FROM session_log ORDER BY log_time DESC LIMIT 20";
        try (Connection conn = connectDB.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                String line = "#" + rs.getInt("log_id")
                    + " | S" + rs.getInt("session_id")
                    + " | " + rs.getString("action")
                    + " | " + (rs.getString("details") == null ? "" : rs.getString("details"))
                    + " | " + String.valueOf(rs.getTimestamp("log_time"));
                logs.add(line);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }

    public static String getCustomerName(int customerId) {
        if (customerId <= 0) {
            return null;
        }
        String sql = "SELECT customer_name FROM customers WHERE customer_id = ?";
        try (Connection conn = connectDB.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, customerId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("customer_name");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void logEvent(int sessionId, String action, String details) {
        String sql = "INSERT INTO session_log (session_id, action, details) VALUES (?, ?, ?)";
        try (Connection conn = connectDB.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, sessionId);
            pst.setString(2, action);
            pst.setString(3, details);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static Session mapRow(ResultSet rs) throws SQLException {
        Session s = new Session();
        s.setId(rs.getInt("id"));
        s.setPcId(rs.getInt("pc_id"));
        s.setHours(rs.getInt("hours"));
        s.setAmount(rs.getDouble("amount"));
        s.setStartTime(String.valueOf(rs.getTimestamp("start_time")));
        s.setEndTime(String.valueOf(rs.getTimestamp("end_time")));
        s.setStatus(rs.getString("status"));
        s.setArchived(rs.getInt("is_archived") == 1);
        int cid = rs.getInt("customer_id");
        s.setCustomerId(rs.wasNull() ? -1 : cid);
        return s;
    }
}
