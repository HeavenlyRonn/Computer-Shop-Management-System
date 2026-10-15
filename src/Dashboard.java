import javax.swing.*;
import java.util.List;

public class Dashboard extends javax.swing.JFrame {

    // Single rate for all 3 PCs (per approved change)
    private static final double HOURLY_RATE = 25.0;
    private static final long serialVersionUID = 1L;

    private final java.util.HashMap<String, Integer> pcIds = new java.util.HashMap<>();
    private final java.util.HashMap<String, Integer> activeSessionIds = new java.util.HashMap<>();
    private javax.swing.Timer refreshTimer;
    private javax.swing.JButton broadcastButton;
    private transient DataStructureManager dsm = new DataStructureManager();

    public Dashboard() {
        initComponents();
        styleExtras();
        addDataStructureButtons();
        setLocationRelativeTo(null);
        initHourBoxes();
        loadPCs();
        refreshState();
        startAutoRefresh();
    }

    private void styleExtras() {
        // initComponents() sets resizable(false) from the .form; override here
        // so a future Design-view save can't silently re-lock the window.
        setResizable(true);
        setMinimumSize(new java.awt.Dimension(900, 560));
        getContentPane().setBackground(new java.awt.Color(24, 20, 22));
        // Thin dark margin around the card.
        backgroundPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16));
        // Fill behavior: card grows with the window (frozen GroupLayout in
        // initComponents is superseded at runtime; .form untouched).
        backgroundPanel.setLayout(new java.awt.BorderLayout());
        backgroundPanel.removeAll();
        backgroundPanel.add(mainCard, java.awt.BorderLayout.CENTER);
        // Stretch behavior: header on top, 3 PC cards share the width evenly.
        java.awt.Color cardBg = mainCard.getBackground();
        javax.swing.JPanel headerPanel = new javax.swing.JPanel(new java.awt.BorderLayout(16, 0));
        headerPanel.setBackground(cardBg);
        headerPanel.add(titleLabel, java.awt.BorderLayout.WEST);
        javax.swing.JPanel headerRight = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 16, 0));
        headerRight.setBackground(cardBg);
        headerRight.add(salesLabel);
        broadcastButton = new javax.swing.JButton("Broadcast");
        broadcastButton.setBackground(new java.awt.Color(186, 45, 90));
        broadcastButton.setFont(new java.awt.Font("Segoe UI", 1, 13));
        broadcastButton.setForeground(new java.awt.Color(255, 255, 255));
        broadcastButton.setFocusPainted(false);
        broadcastButton.addActionListener(this::broadcastButtonActionPerformed);
        headerRight.add(broadcastButton);
        headerRight.add(logoutButton);
        headerPanel.add(headerRight, java.awt.BorderLayout.EAST);
        javax.swing.JPanel pcRow = new javax.swing.JPanel(new java.awt.GridLayout(1, 3, 12, 12));
        pcRow.setBackground(cardBg);
        pcRow.add(pc1Panel);
        pcRow.add(pc2Panel);
        pcRow.add(pc3Panel);
        mainCard.setLayout(new java.awt.BorderLayout(0, 16));
        mainCard.removeAll();
        mainCard.add(headerPanel, java.awt.BorderLayout.NORTH);
        mainCard.add(pcRow, java.awt.BorderLayout.CENTER);
        mainCard.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(new java.awt.Color(217, 198, 206), 1),
                javax.swing.BorderFactory.createEmptyBorder(20, 24, 20, 24)));
        pc1Panel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(217, 198, 206), 1));
        pc2Panel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(217, 198, 206), 1));
        pc3Panel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(217, 198, 206), 1));
        logoutButton.setFocusPainted(false);
        pc1Start.setFocusPainted(false);
        pc1Extend.setFocusPainted(false);
        pc1Stop.setFocusPainted(false);
        pc2Start.setFocusPainted(false);
        pc2Extend.setFocusPainted(false);
        pc2Stop.setFocusPainted(false);
        pc3Start.setFocusPainted(false);
        pc3Extend.setFocusPainted(false);
        pc3Stop.setFocusPainted(false);
        // Same rate shown on all cards
        String rateText = "P" + (int) HOURLY_RATE + "/hr";
        pc1Rate.setText(rateText);
        pc2Rate.setText(rateText);
        pc3Rate.setText(rateText);
        // Snug fit to content; constructor centers afterward.
        pack();
    }

    private void initHourBoxes() {
        String[] hours = {"1", "2", "3", "4", "5", "6"};
        pc1Hours.setModel(new javax.swing.DefaultComboBoxModel<>(hours));
        pc2Hours.setModel(new javax.swing.DefaultComboBoxModel<>(hours));
        pc3Hours.setModel(new javax.swing.DefaultComboBoxModel<>(hours));
    }

    private void loadPCs() {
        java.util.Map<Integer, String> idToName = PCsDAO.getAllPCs();
        pcIds.clear();
        for (java.util.Map.Entry<Integer, String> e : idToName.entrySet()) {
            pcIds.put(e.getValue(), e.getKey());
        }
        if (pcIds.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Database Error: cannot load PCs. Check XAMPP and setup SQL.");
        } else if (!pcIds.containsKey("PC1") || !pcIds.containsKey("PC2") || !pcIds.containsKey("PC3")) {
            JOptionPane.showMessageDialog(this, "pcs table is missing PC1-PC3. Run the setup SQL first.");
        }
    }

    private void startAutoRefresh() {
        refreshTimer = new javax.swing.Timer(1000, e -> refreshState());
        refreshTimer.start();
    }

    private void refreshState() {
        autoStopExpired();
        refreshOne("PC1", pc1Status, pc1Remaining, pc1Start, pc1Extend, pc1Stop);
        refreshOne("PC2", pc2Status, pc2Remaining, pc2Start, pc2Extend, pc2Stop);
        refreshOne("PC3", pc3Status, pc3Remaining, pc3Start, pc3Extend, pc3Stop);
        updateSalesTotal();
    }

    private void autoStopExpired() {
        SessionDAO.autoStopExpired();
    }

    private void refreshOne(String pcName, javax.swing.JLabel statusLabel,
            javax.swing.JLabel remainingLabel, javax.swing.JButton startBtn,
            javax.swing.JButton extendBtn, javax.swing.JButton stopBtn) {
        Integer pcId = pcIds.get(pcName);
        if (pcId == null) {
            statusLabel.setText("NO DB");
            statusLabel.setForeground(new java.awt.Color(122, 92, 104));
            remainingLabel.setText("0:00:00");
            startBtn.setEnabled(false);
            extendBtn.setEnabled(false);
            stopBtn.setEnabled(false);
            return;
        }
        Session s = SessionDAO.getActiveSessionByPc(pcId);
        if (s != null) {
            int sessionId = s.getId();
            long left;
            try {
                left = java.sql.Timestamp.valueOf(s.getEndTime()).getTime() - System.currentTimeMillis();
            } catch (Exception ex) {
                left = 0;
            }
            if (left < 0) {
                left = 0;
            }
            activeSessionIds.put(pcName, sessionId);
            String customerName = null;
            if (s.getCustomerId() != -1) {
                customerName = SessionDAO.getCustomerName(s.getCustomerId());
            }
            if (customerName == null || customerName.trim().isEmpty()) {
                statusLabel.setText("IN USE");
            } else {
                statusLabel.setText("IN USE - " + customerName.trim());
            }
            statusLabel.setForeground(new java.awt.Color(186, 45, 90));
            remainingLabel.setText(formatHMS(left));
            startBtn.setEnabled(false);
            extendBtn.setEnabled(true);
            stopBtn.setEnabled(true);
        } else {
            activeSessionIds.remove(pcName);
            statusLabel.setText("AVAILABLE");
            statusLabel.setForeground(new java.awt.Color(122, 92, 104));
            remainingLabel.setText("0:00:00");
            startBtn.setEnabled(true);
            extendBtn.setEnabled(false);
            stopBtn.setEnabled(false);
        }
    }

    private void updateSalesTotal() {
        double total = SessionDAO.getTodaySalesTotal();
        salesLabel.setText(String.format("Today's Sales: P%.2f", total));
    }

    private String formatHMS(long millis) {
        long s = millis / 1000;
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        return String.format("%d:%02d:%02d", h, m, sec);
    }

    private int selectedHours(String pcName) {
        try {
            String v;
            if ("PC1".equals(pcName)) {
                v = (String) pc1Hours.getSelectedItem();
            } else if ("PC2".equals(pcName)) {
                v = (String) pc2Hours.getSelectedItem();
            } else {
                v = (String) pc3Hours.getSelectedItem();
            }
            int h = Integer.parseInt(v);
            if (h < 1 || h > 6) {
                return 1;
            }
            return h;
        } catch (Exception e) {
            return 1;
        }
    }

    private void doStart(String pcName) {
        Integer pcId = pcIds.get(pcName);
        if (pcId == null) {
            JOptionPane.showMessageDialog(this, "Database Error: " + pcName + " not found. Run setup SQL.");
            return;
        }
        if (activeSessionIds.containsKey(pcName)) {
            JOptionPane.showMessageDialog(this, pcName + " is already in use.");
            return;
        }
        int hours = selectedHours(pcName);
        double amount = hours * HOURLY_RATE;
        String custName = JOptionPane.showInputDialog(this, "Customer name (optional):");
        String custEmail = "";
        if (custName != null && !custName.trim().isEmpty()) {
            custEmail = JOptionPane.showInputDialog(this, "Customer email (optional):");
            if (custEmail == null) {
                custEmail = "";
            }
        }
        int customerId = -1;
        if (custName != null && !custName.trim().isEmpty()) {
            customerId = SessionDAO.registerOrGetCustomer(custName, custEmail, "");
        }
        SessionDAO.startSession(pcId, hours, amount, customerId);
        JOptionPane.showMessageDialog(this, pcName + " started: " + hours + " hr (P" + String.format("%.2f", amount) + ").");
        refreshState();
    }

    private void doExtend(String pcName) {
        Integer sessionId = activeSessionIds.get(pcName);
        if (sessionId == null) {
            JOptionPane.showMessageDialog(this, pcName + " has no active session.");
            return;
        }
        int add = selectedHours(pcName);
        double addAmount = add * HOURLY_RATE;
        dsm.pushExtension(sessionId, add, addAmount);
        SessionDAO.extendSession(sessionId, add, addAmount);
        JOptionPane.showMessageDialog(this, pcName + " extended: +" + add + " hr (P" + String.format("%.2f", addAmount) + ").");
        refreshState();
    }

    private void doStop(String pcName) {
        Integer sessionId = activeSessionIds.get(pcName);
        if (sessionId == null) {
            JOptionPane.showMessageDialog(this, pcName + " has no active session.");
            return;
        }
        SessionDAO.stopSession(sessionId);
        JOptionPane.showMessageDialog(this, pcName + " stopped. Now available.");
        refreshState();
    }

    private void broadcastButtonActionPerformed(java.awt.event.ActionEvent evt) {
        doBroadcast();
    }

    private void doBroadcast() {
        String message = JOptionPane.showInputDialog(this, "Enter message for all active PCs:", "Broadcast Message", JOptionPane.PLAIN_MESSAGE);
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        int count = PCsDAO.broadcastToActivePCs(message.trim());
        if (count == 0) {
            JOptionPane.showMessageDialog(this, "No active PCs to broadcast to.");
        } else {
            JOptionPane.showMessageDialog(this, "Broadcast sent to " + count + " PC(s).");
        }
    }

    private void addDataStructureButtons() {
        // Create a JPanel at the top of the content pane with two buttons
        JPanel topPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        JButton btnSort = new JButton("Sort by Amount");
        JButton btnUndo = new JButton("Undo Last Extend");
        JButton btnLogs = new JButton("View Logs");
        JButton btnFind = new JButton("Find Session");

        btnSort.addActionListener(e -> doSortByAmount());
        btnUndo.addActionListener(e -> doUndoLastExtend());
        btnFind.addActionListener(e -> doFindSession());
        btnLogs.addActionListener(e -> {
            List<String> logs = SessionDAO.getRecentLogs();
            if (logs.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No log entries yet.");
                return;
            }
            StringBuilder sb = new StringBuilder();
            for (String line : logs) {
                sb.append(line).append("\n");
            }
            JTextArea area = new JTextArea(sb.toString());
            area.setEditable(false);
            JScrollPane scroll = new JScrollPane(area);
            scroll.setPreferredSize(new java.awt.Dimension(500, 300));
            JOptionPane.showMessageDialog(this, scroll, "Recent Session Logs", JOptionPane.PLAIN_MESSAGE);
        });

        topPanel.add(btnUndo);
        topPanel.add(btnSort);
        topPanel.add(btnLogs);
        topPanel.add(btnFind);

        // Add to the top of the content pane using BorderLayout.NORTH
        getContentPane().setLayout(new java.awt.BorderLayout());
        getContentPane().add(topPanel, java.awt.BorderLayout.NORTH);
        getContentPane().add(backgroundPanel, java.awt.BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void doSortByAmount() {
        List<Session> all = SessionDAO.getAllSessions();
        if (all.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No sessions to sort.");
            return;
        }
        List<Session> sorted = dsm.sortSessionsByAmount(all);
        StringBuilder sb = new StringBuilder("Sessions sorted by amount (descending):\n\n");
        for (Session s : sorted) {
            sb.append("ID: ").append(s.getId())
              .append(" | PC: ").append(s.getPcId())
              .append(" | Amount: ").append(s.getAmount())
              .append(" | Status: ").append(s.getStatus()).append("\n");
        }
        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new java.awt.Dimension(400, 300));
        JOptionPane.showMessageDialog(this, scroll, "Sorted Sessions", JOptionPane.PLAIN_MESSAGE);
    }

    private void doFindSession() {
        String input = JOptionPane.showInputDialog(this, "Enter Session ID to find:");
        if (input == null || input.trim().isEmpty()) {
            return;
        }
        int targetId;
        try {
            targetId = Integer.parseInt(input.trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number.");
            return;
        }
        List<Session> all = SessionDAO.getAllSessions();
        if (all.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No sessions in database.");
            return;
        }
        List<Session> sorted = dsm.sortSessionsById(all);
        Session found = dsm.binarySearchById(sorted, targetId);
        if (found == null) {
            JOptionPane.showMessageDialog(this, "Session ID " + targetId + " not found.");
            return;
        }
        String msg = "Session found:\n\n"
            + "ID: " + found.getId() + "\n"
            + "PC ID: " + found.getPcId() + "\n"
            + "Customer ID: " + found.getCustomerId() + "\n"
            + "Hours: " + found.getHours() + "\n"
            + "Amount: P" + String.format("%.2f", found.getAmount()) + "\n"
            + "Status: " + found.getStatus() + "\n"
            + "Start: " + found.getStartTime() + "\n"
            + "End: " + found.getEndTime();
        JOptionPane.showMessageDialog(this, msg, "Session Found", JOptionPane.INFORMATION_MESSAGE);
    }

    private void doUndoLastExtend() {
        DataStructureManager.ExtendRecord rec = dsm.undoLastExtension();
        if (rec == null) {
            JOptionPane.showMessageDialog(this, "Nothing to undo.");
            return;
        }
        SessionDAO.revertLastExtend(rec.sessionId, rec.extraHours, rec.extraAmount);
        JOptionPane.showMessageDialog(this, "Undo successful for session ID: " + rec.sessionId);
        refreshState();
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
        mainCard = new javax.swing.JPanel();
        titleLabel = new javax.swing.JLabel();
        salesLabel = new javax.swing.JLabel();
        logoutButton = new javax.swing.JButton();
        pc1Panel = new javax.swing.JPanel();
        pc1Name = new javax.swing.JLabel();
        pc1Rate = new javax.swing.JLabel();
        pc1Status = new javax.swing.JLabel();
        pc1Remaining = new javax.swing.JLabel();
        pc1Hours = new javax.swing.JComboBox<>();
        pc1Start = new javax.swing.JButton();
        pc1Extend = new javax.swing.JButton();
        pc1Stop = new javax.swing.JButton();
        pc2Panel = new javax.swing.JPanel();
        pc2Name = new javax.swing.JLabel();
        pc2Rate = new javax.swing.JLabel();
        pc2Status = new javax.swing.JLabel();
        pc2Remaining = new javax.swing.JLabel();
        pc2Hours = new javax.swing.JComboBox<>();
        pc2Start = new javax.swing.JButton();
        pc2Extend = new javax.swing.JButton();
        pc2Stop = new javax.swing.JButton();
        pc3Panel = new javax.swing.JPanel();
        pc3Name = new javax.swing.JLabel();
        pc3Rate = new javax.swing.JLabel();
        pc3Status = new javax.swing.JLabel();
        pc3Remaining = new javax.swing.JLabel();
        pc3Hours = new javax.swing.JComboBox<>();
        pc3Start = new javax.swing.JButton();
        pc3Extend = new javax.swing.JButton();
        pc3Stop = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Counter Dashboard");
        setResizable(false);

        backgroundPanel.setBackground(new java.awt.Color(24, 20, 22));

        mainCard.setBackground(new java.awt.Color(249, 241, 244));

        titleLabel.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        titleLabel.setForeground(new java.awt.Color(43, 34, 39));
        titleLabel.setText("COUNTER");

        salesLabel.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        salesLabel.setForeground(new java.awt.Color(186, 45, 90));
        salesLabel.setText("Today's Sales: P0.00");

        logoutButton.setBackground(new java.awt.Color(186, 45, 90));
        logoutButton.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        logoutButton.setForeground(new java.awt.Color(255, 255, 255));
        logoutButton.setText("Logout");
        logoutButton.addActionListener(this::logoutButtonActionPerformed);

        pc1Panel.setBackground(new java.awt.Color(255, 255, 255));

        pc1Name.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        pc1Name.setForeground(new java.awt.Color(43, 34, 39));
        pc1Name.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        pc1Name.setText("PC1");

        pc1Rate.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        pc1Rate.setForeground(new java.awt.Color(122, 92, 104));
        pc1Rate.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        pc1Rate.setText("P25/hr");

        pc1Status.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        pc1Status.setForeground(new java.awt.Color(122, 92, 104));
        pc1Status.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        pc1Status.setText("AVAILABLE");

        pc1Remaining.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        pc1Remaining.setForeground(new java.awt.Color(43, 34, 39));
        pc1Remaining.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        pc1Remaining.setText("0:00:00");

        pc1Hours.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N

        pc1Start.setBackground(new java.awt.Color(186, 45, 90));
        pc1Start.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        pc1Start.setForeground(new java.awt.Color(255, 255, 255));
        pc1Start.setText("Start");
        pc1Start.addActionListener(this::pc1StartActionPerformed);

        pc1Extend.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        pc1Extend.setText("Extend");
        pc1Extend.addActionListener(this::pc1ExtendActionPerformed);

        pc1Stop.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        pc1Stop.setText("Stop");
        pc1Stop.addActionListener(this::pc1StopActionPerformed);

        javax.swing.GroupLayout pc1PanelLayout = new javax.swing.GroupLayout(pc1Panel);
        pc1Panel.setLayout(pc1PanelLayout);
        pc1PanelLayout.setHorizontalGroup(
            pc1PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pc1PanelLayout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(pc1PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pc1Name, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pc1Rate, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pc1Status, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pc1Remaining, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pc1Hours, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pc1Start, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pc1Extend, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pc1Stop, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(16, 16, 16))
        );
        pc1PanelLayout.setVerticalGroup(
            pc1PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pc1PanelLayout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(pc1Name)
                .addGap(4, 4, 4)
                .addComponent(pc1Rate)
                .addGap(8, 8, 8)
                .addComponent(pc1Status)
                .addGap(4, 4, 4)
                .addComponent(pc1Remaining)
                .addGap(12, 12, 12)
                .addComponent(pc1Hours, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(8, 8, 8)
                .addComponent(pc1Start, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(8, 8, 8)
                .addComponent(pc1Extend, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(8, 8, 8)
                .addComponent(pc1Stop, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(16, 16, 16))
        );

        pc2Panel.setBackground(new java.awt.Color(255, 255, 255));

        pc2Name.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        pc2Name.setForeground(new java.awt.Color(43, 34, 39));
        pc2Name.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        pc2Name.setText("PC2");

        pc2Rate.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        pc2Rate.setForeground(new java.awt.Color(122, 92, 104));
        pc2Rate.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        pc2Rate.setText("P25/hr");

        pc2Status.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        pc2Status.setForeground(new java.awt.Color(122, 92, 104));
        pc2Status.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        pc2Status.setText("AVAILABLE");

        pc2Remaining.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        pc2Remaining.setForeground(new java.awt.Color(43, 34, 39));
        pc2Remaining.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        pc2Remaining.setText("0:00:00");

        pc2Hours.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N

        pc2Start.setBackground(new java.awt.Color(186, 45, 90));
        pc2Start.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        pc2Start.setForeground(new java.awt.Color(255, 255, 255));
        pc2Start.setText("Start");
        pc2Start.addActionListener(this::pc2StartActionPerformed);

        pc2Extend.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        pc2Extend.setText("Extend");
        pc2Extend.addActionListener(this::pc2ExtendActionPerformed);

        pc2Stop.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        pc2Stop.setText("Stop");
        pc2Stop.addActionListener(this::pc2StopActionPerformed);

        javax.swing.GroupLayout pc2PanelLayout = new javax.swing.GroupLayout(pc2Panel);
        pc2Panel.setLayout(pc2PanelLayout);
        pc2PanelLayout.setHorizontalGroup(
            pc2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pc2PanelLayout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(pc2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pc2Name, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pc2Rate, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pc2Status, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pc2Remaining, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pc2Hours, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pc2Start, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pc2Extend, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pc2Stop, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(16, 16, 16))
        );
        pc2PanelLayout.setVerticalGroup(
            pc2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pc2PanelLayout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(pc2Name)
                .addGap(4, 4, 4)
                .addComponent(pc2Rate)
                .addGap(8, 8, 8)
                .addComponent(pc2Status)
                .addGap(4, 4, 4)
                .addComponent(pc2Remaining)
                .addGap(12, 12, 12)
                .addComponent(pc2Hours, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(8, 8, 8)
                .addComponent(pc2Start, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(8, 8, 8)
                .addComponent(pc2Extend, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(8, 8, 8)
                .addComponent(pc2Stop, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(16, 16, 16))
        );

        pc3Panel.setBackground(new java.awt.Color(255, 255, 255));

        pc3Name.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        pc3Name.setForeground(new java.awt.Color(43, 34, 39));
        pc3Name.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        pc3Name.setText("PC3");

        pc3Rate.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        pc3Rate.setForeground(new java.awt.Color(122, 92, 104));
        pc3Rate.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        pc3Rate.setText("P25/hr");

        pc3Status.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        pc3Status.setForeground(new java.awt.Color(122, 92, 104));
        pc3Status.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        pc3Status.setText("AVAILABLE");

        pc3Remaining.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        pc3Remaining.setForeground(new java.awt.Color(43, 34, 39));
        pc3Remaining.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        pc3Remaining.setText("0:00:00");

        pc3Hours.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N

        pc3Start.setBackground(new java.awt.Color(186, 45, 90));
        pc3Start.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        pc3Start.setForeground(new java.awt.Color(255, 255, 255));
        pc3Start.setText("Start");
        pc3Start.addActionListener(this::pc3StartActionPerformed);

        pc3Extend.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        pc3Extend.setText("Extend");
        pc3Extend.addActionListener(this::pc3ExtendActionPerformed);

        pc3Stop.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        pc3Stop.setText("Stop");
        pc3Stop.addActionListener(this::pc3StopActionPerformed);

        javax.swing.GroupLayout pc3PanelLayout = new javax.swing.GroupLayout(pc3Panel);
        pc3Panel.setLayout(pc3PanelLayout);
        pc3PanelLayout.setHorizontalGroup(
            pc3PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pc3PanelLayout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(pc3PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pc3Name, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pc3Rate, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pc3Status, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pc3Remaining, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pc3Hours, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pc3Start, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pc3Extend, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pc3Stop, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(16, 16, 16))
        );
        pc3PanelLayout.setVerticalGroup(
            pc3PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pc3PanelLayout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(pc3Name)
                .addGap(4, 4, 4)
                .addComponent(pc3Rate)
                .addGap(8, 8, 8)
                .addComponent(pc3Status)
                .addGap(4, 4, 4)
                .addComponent(pc3Remaining)
                .addGap(12, 12, 12)
                .addComponent(pc3Hours, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(8, 8, 8)
                .addComponent(pc3Start, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(8, 8, 8)
                .addComponent(pc3Extend, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(8, 8, 8)
                .addComponent(pc3Stop, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(16, 16, 16))
        );

        javax.swing.GroupLayout mainCardLayout = new javax.swing.GroupLayout(mainCard);
        mainCard.setLayout(mainCardLayout);
        mainCardLayout.setHorizontalGroup(
            mainCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainCardLayout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addComponent(titleLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(salesLabel)
                .addGap(16, 16, 16)
                .addComponent(logoutButton, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(24, 24, 24))
            .addGroup(mainCardLayout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addComponent(pc1Panel, javax.swing.GroupLayout.PREFERRED_SIZE, 270, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(pc2Panel, javax.swing.GroupLayout.PREFERRED_SIZE, 270, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(pc3Panel, javax.swing.GroupLayout.PREFERRED_SIZE, 270, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(24, 24, 24))
        );
        mainCardLayout.setVerticalGroup(
            mainCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainCardLayout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(mainCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(titleLabel)
                    .addComponent(salesLabel)
                    .addComponent(logoutButton, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(16, 16, 16)
                .addGroup(mainCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pc1Panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pc2Panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pc3Panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(20, 20, 20))
        );

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(mainCard, javax.swing.GroupLayout.PREFERRED_SIZE, 920, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(mainCard, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
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

    private void logoutButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_logoutButtonActionPerformed
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        Login f = new Login();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_logoutButtonActionPerformed

    private void pc1StartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pc1StartActionPerformed
        doStart("PC1");
    }//GEN-LAST:event_pc1StartActionPerformed

    private void pc1ExtendActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pc1ExtendActionPerformed
        doExtend("PC1");
    }//GEN-LAST:event_pc1ExtendActionPerformed

    private void pc1StopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pc1StopActionPerformed
        doStop("PC1");
    }//GEN-LAST:event_pc1StopActionPerformed

    private void pc2StartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pc2StartActionPerformed
        doStart("PC2");
    }//GEN-LAST:event_pc2StartActionPerformed

    private void pc2ExtendActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pc2ExtendActionPerformed
        doExtend("PC2");
    }//GEN-LAST:event_pc2ExtendActionPerformed

    private void pc2StopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pc2StopActionPerformed
        doStop("PC2");
    }//GEN-LAST:event_pc2StopActionPerformed

    private void pc3StartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pc3StartActionPerformed
        doStart("PC3");
    }//GEN-LAST:event_pc3StartActionPerformed

    private void pc3ExtendActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pc3ExtendActionPerformed
        doExtend("PC3");
    }//GEN-LAST:event_pc3ExtendActionPerformed

    private void pc3StopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pc3StopActionPerformed
        doStop("PC3");
    }//GEN-LAST:event_pc3StopActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JButton logoutButton;
    private javax.swing.JPanel mainCard;
    private javax.swing.JButton pc1Extend;
    private javax.swing.JComboBox<String> pc1Hours;
    private javax.swing.JLabel pc1Name;
    private javax.swing.JPanel pc1Panel;
    private javax.swing.JLabel pc1Rate;
    private javax.swing.JLabel pc1Remaining;
    private javax.swing.JButton pc1Start;
    private javax.swing.JLabel pc1Status;
    private javax.swing.JButton pc1Stop;
    private javax.swing.JButton pc2Extend;
    private javax.swing.JComboBox<String> pc2Hours;
    private javax.swing.JLabel pc2Name;
    private javax.swing.JPanel pc2Panel;
    private javax.swing.JLabel pc2Rate;
    private javax.swing.JLabel pc2Remaining;
    private javax.swing.JButton pc2Start;
    private javax.swing.JLabel pc2Status;
    private javax.swing.JButton pc2Stop;
    private javax.swing.JButton pc3Extend;
    private javax.swing.JComboBox<String> pc3Hours;
    private javax.swing.JLabel pc3Name;
    private javax.swing.JPanel pc3Panel;
    private javax.swing.JLabel pc3Rate;
    private javax.swing.JLabel pc3Remaining;
    private javax.swing.JButton pc3Start;
    private javax.swing.JLabel pc3Status;
    private javax.swing.JButton pc3Stop;
    private javax.swing.JLabel salesLabel;
    private javax.swing.JLabel titleLabel;
    // End of variables declaration//GEN-END:variables
}
