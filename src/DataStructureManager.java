import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

public class DataStructureManager {

    public static class ExtendRecord {
        public final int sessionId;
        public final int extraHours;
        public final double extraAmount;
        public ExtendRecord(int sessionId, int extraHours, double extraAmount) {
            this.sessionId = sessionId;
            this.extraHours = extraHours;
            this.extraAmount = extraAmount;
        }
    }

    private Stack<ExtendRecord> extensionHistory = new Stack<>();

    public void pushExtension(int sessionId, int extraHours, double extraAmount) {
        extensionHistory.push(new ExtendRecord(sessionId, extraHours, extraAmount));
    }

    public ExtendRecord undoLastExtension() {
        if (extensionHistory.isEmpty()) {
            return null;
        }
        return extensionHistory.pop();
    }

    public List<Session> sortSessionsByAmount(List<Session> sessions) {
        // Copy the list so the original is not modified
        List<Session> sorted = new ArrayList<>(sessions);
        int n = sorted.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                if (sorted.get(j).getAmount() < sorted.get(j + 1).getAmount()) {
                    Session temp = sorted.get(j);
                    sorted.set(j, sorted.get(j + 1));
                    sorted.set(j + 1, temp);
                }
            }
        }
        return sorted;
    }

    public List<Session> sortSessionsById(List<Session> sessions) {
        List<Session> sorted = new ArrayList<>(sessions);
        int n = sorted.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                if (sorted.get(j).getId() > sorted.get(j + 1).getId()) {
                    Session temp = sorted.get(j);
                    sorted.set(j, sorted.get(j + 1));
                    sorted.set(j + 1, temp);
                }
            }
        }
        return sorted;
    }

    public Session binarySearchById(List<Session> sessions, int targetId) {
        // Assumes sessions are already sorted by ID ascending
        int left = 0;
        int right = sessions.size() - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            int midId = sessions.get(mid).getId();
            if (midId == targetId) {
                return sessions.get(mid);
            } else if (midId < targetId) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return null;
    }
}
