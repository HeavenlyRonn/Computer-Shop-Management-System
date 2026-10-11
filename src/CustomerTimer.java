import java.sql.*;
import javax.swing.*;

public class CustomerTimer extends javax.swing.JFrame {

    private static final long serialVersionUID = 1L;
    private Integer pcId = null;
    private Integer lastSessionId = null;
    private boolean warned30 = false;
    private boolean warned5 = false;
    private boolean timesUpShown = false;
    private javax.swing.Timer refreshTimer;

    public CustomerTimer() {
        this("customer");
    }

    public CustomerTimer(String username) {
        initComponents();
        // initComponents() sets resizable(false) from the .form; override here
        // so a future Design-view save can't silently re-lock the window.
        setResizable(true);
        setMinimumSize(new java.awt.Dimension(340, 280));
        getContentPane().setBackground(new java.awt.Color(24, 20, 22));
        // Thin dark margin around the card.
        backgroundPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16));
        // Fill behavior: card grows with the window (frozen GroupLayout in
        // initComponents is superseded at runtime; .form untouched).
        backgroundPanel.setLayout(new java.awt.BorderLayout());
        backgroundPanel.removeAll();
        backgroundPanel.add(loginCard, java.awt.BorderLayout.CENTER);
        backButton.setFocusPainted(false);
        loginCard.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(217, 198, 206), 1));
        if (username != null && !username.trim().isEmpty()) {
            userLabel.setText("Logged in as " + username.trim());
        }
        // Snug fit to content, then center.
        pack();
        setLocationRelativeTo(null);
        resolvePcId(username);
        tick();
        refreshTimer = new javax.swing.Timer(1000, e -> tick());
        refreshTimer.start();
    }

    private void resolvePcId(String username) {
        if (username == null || username.trim().isEmpty()) {
            return;
        }
        String sql = "SELECT id FROM pcs WHERE UPPER(pc_name) = UPPER(?)";
        try {
            Connection c = connectDB.getConnection();
            if (c == null) {
                return;
            }
            try (PreparedStatement pst = c.prepareStatement(sql)) {
                pst.setString(1, username.trim());
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) {
                        pcId = rs.getInt("id");
                    }
                }
            } finally {
                try { c.close(); } catch (Exception ex) { }
            }
        } catch (SQLException e) {
            System.out.println("Resolve PC Error: " + e.getMessage());
        }
        if (pcId == null) {
            noteLabel.setText("No PC linked to this account.");
        }
    }

    private void tick() {
        if (pcId == null) {
            timerLabel.setText("0:00:00");
            return;
        }
        String sql = "SELECT id, end_time FROM sessions WHERE pc_id = ? AND status = 'active' AND is_archived = 0 ORDER BY id DESC LIMIT 1";
        try {
            Connection c = connectDB.getConnection();
            if (c == null) {
                return;
            }
            try (PreparedStatement pst = c.prepareStatement(sql)) {
                pst.setInt(1, pcId);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) {
                        int sessionId = rs.getInt("id");
                        Timestamp end = rs.getTimestamp("end_time");
                        long left = end.getTime() - System.currentTimeMillis();
                        if (left < 0) {
                            left = 0;
                        }
                        if (lastSessionId == null || lastSessionId != sessionId) {
                            lastSessionId = sessionId;
                            warned30 = false;
                            warned5 = false;
                            timesUpShown = false;
                        }
                        timerLabel.setText(formatHMS(left));
                        noteLabel.setText("Session in progress.");
                        if (left <= 0 && !timesUpShown) {
                            timesUpShown = true;
                            JOptionPane.showMessageDialog(this, "Time's Up! Please go to the counter.");
                        } else if (left <= 5 * 60 * 1000 && left > 0 && !warned5) {
                            warned5 = true;
                            JOptionPane.showMessageDialog(this, "5 minutes remaining. Please save your work.");
                        } else if (left <= 30 * 60 * 1000 && left > 5 * 60 * 1000 && !warned30) {
                            warned30 = true;
                            JOptionPane.showMessageDialog(this, "30 minutes remaining.");
                        }
                    } else {
                        lastSessionId = null;
                        warned30 = false;
                        warned5 = false;
                        timesUpShown = false;
                        timerLabel.setText("0:00:00");
                        noteLabel.setText("Waiting for counter to start session.");
                    }
                }
            } finally {
                try { c.close(); } catch (Exception ex) { }
            }
        } catch (SQLException e) {
            System.out.println("Timer Refresh Error: " + e.getMessage());
        }
        checkBroadcast();
    }

    private void checkBroadcast() {
        if (pcId == null) {
            return;
        }
        String sql = "SELECT broadcast_pending, broadcast_message FROM pcs WHERE id = ?";
        String message = null;
        try {
            Connection c = connectDB.getConnection();
            if (c == null) {
                return;
            }
            try (PreparedStatement pst = c.prepareStatement(sql)) {
                pst.setInt(1, pcId);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next() && rs.getInt("broadcast_pending") == 1) {
                        message = rs.getString("broadcast_message");
                    }
                }
            } finally {
                try { c.close(); } catch (Exception ex) { }
            }
        } catch (SQLException e) {
            System.out.println("Broadcast Error: " + e.getMessage());
            return;
        }
        if (message == null) {
            return;
        }
        JOptionPane.showMessageDialog(this, message, "Notice from Counter", JOptionPane.INFORMATION_MESSAGE);
        clearBroadcast();
    }

    private void clearBroadcast() {
        String sql = "UPDATE pcs SET broadcast_pending = 0, broadcast_message = '' WHERE id = ?";
        try {
            Connection c = connectDB.getConnection();
            if (c == null) {
                return;
            }
            try (PreparedStatement pst = c.prepareStatement(sql)) {
                pst.setInt(1, pcId);
                pst.executeUpdate();
            } finally {
                try { c.close(); } catch (Exception ex) { }
            }
        } catch (SQLException e) {
            System.out.println("Broadcast Error: " + e.getMessage());
        }
    }

    private String formatHMS(long millis) {
        long s = millis / 1000;
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        return String.format("%d:%02d:%02d", h, m, sec);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        backgroundPanel = new javax.swing.JPanel();
        loginCard = new javax.swing.JPanel();
        titleLabel = new javax.swing.JLabel();
        timerLabel = new javax.swing.JLabel();
        userLabel = new javax.swing.JLabel();
        noteLabel = new javax.swing.JLabel();
        backButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Customer PC");
        setResizable(false);

        backgroundPanel.setBackground(new java.awt.Color(24, 20, 22));

        loginCard.setBackground(new java.awt.Color(249, 241, 244));

        titleLabel.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        titleLabel.setForeground(new java.awt.Color(43, 34, 39));
        titleLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        titleLabel.setText("CUSTOMER");

        timerLabel.setFont(new java.awt.Font("Segoe UI", 1, 36)); // NOI18N
        timerLabel.setForeground(new java.awt.Color(186, 45, 90));
        timerLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        timerLabel.setText("0:00:00");

        userLabel.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        userLabel.setForeground(new java.awt.Color(122, 92, 104));
        userLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        userLabel.setText("Logged in");

        noteLabel.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        noteLabel.setForeground(new java.awt.Color(122, 92, 104));
        noteLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        noteLabel.setText("Waiting for counter to start session.");

        backButton.setBackground(new java.awt.Color(186, 45, 90));
        backButton.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        backButton.setForeground(new java.awt.Color(255, 255, 255));
        backButton.setText("Back to Login");
        backButton.addActionListener(this::backButtonActionPerformed);

        javax.swing.GroupLayout loginCardLayout = new javax.swing.GroupLayout(loginCard);
        loginCard.setLayout(loginCardLayout);
        loginCardLayout.setHorizontalGroup(
            loginCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(loginCardLayout.createSequentialGroup()
                .addGap(32, 32, 32)
                .addGroup(loginCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(titleLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(timerLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(userLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(noteLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(backButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(32, 32, 32))
        );
        loginCardLayout.setVerticalGroup(
            loginCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(loginCardLayout.createSequentialGroup()
                .addGap(28, 28, 28)
                .addComponent(titleLabel)
                .addGap(8, 8, 8)
                .addComponent(timerLabel)
                .addGap(4, 4, 4)
                .addComponent(userLabel)
                .addGap(4, 4, 4)
                .addComponent(noteLabel)
                .addGap(24, 24, 24)
                .addComponent(backButton, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(28, 28, 28))
        );

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(loginCard, javax.swing.GroupLayout.PREFERRED_SIZE, 360, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(loginCard, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void backButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backButtonActionPerformed
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        Login f = new Login();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_backButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton backButton;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JPanel loginCard;
    private javax.swing.JLabel noteLabel;
    private javax.swing.JLabel timerLabel;
    private javax.swing.JLabel titleLabel;
    private javax.swing.JLabel userLabel;
    // End of variables declaration//GEN-END:variables
}
