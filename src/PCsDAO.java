import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class PCsDAO {

    public static Map<Integer, String> getAllPCs() {
        Map<Integer, String> pcs = new HashMap<>();
        String sql = "SELECT id, pc_name FROM pcs ORDER BY pc_name";
        try (Connection conn = connectDB.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                pcs.put(rs.getInt("id"), rs.getString("pc_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return pcs;
    }

    public static int getPcIdByName(String pcName) {
        String sql = "SELECT id FROM pcs WHERE UPPER(pc_name) = UPPER(?)";
        try (Connection conn = connectDB.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, pcName);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static int broadcastToActivePCs(String message) {
        String sql = "UPDATE pcs SET broadcast_pending = 1, broadcast_message = ? WHERE id IN (SELECT pc_id FROM sessions WHERE status = 'active' AND is_archived = 0)";
        try (Connection conn = connectDB.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, message);
            return pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static String getPendingBroadcast(int pcId) {
        String sql = "SELECT broadcast_pending, broadcast_message FROM pcs WHERE id = ?";
        try (Connection conn = connectDB.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, pcId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next() && rs.getInt("broadcast_pending") == 1) {
                    return rs.getString("broadcast_message");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void clearBroadcast(int pcId) {
        String sql = "UPDATE pcs SET broadcast_pending = 0, broadcast_message = '' WHERE id = ?";
        try (Connection conn = connectDB.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, pcId);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
