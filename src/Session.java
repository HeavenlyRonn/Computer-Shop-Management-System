public class Session {
    private int id;
    private int pcId;
    private int hours;
    private double amount;
    private String startTime;
    private String endTime;
    private String status;
    private boolean isArchived;
    private int customerId;

    public Session() {
    }

    public Session(int id, int pcId, int hours, double amount, String startTime, String endTime, String status, boolean isArchived, int customerId) {
        this.id = id;
        this.pcId = pcId;
        this.hours = hours;
        this.amount = amount;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.isArchived = isArchived;
        this.customerId = customerId;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public int getPcId() {
        return pcId;
    }
    public void setPcId(int pcId) {
        this.pcId = pcId;
    }
    public int getHours() {
        return hours;
    }
    public void setHours(int hours) {
        this.hours = hours;
    }
    public double getAmount() {
        return amount;
    }
    public void setAmount(double amount) {
        this.amount = amount;
    }
    public String getStartTime() {
        return startTime;
    }
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }
    public String getEndTime() {
        return endTime;
    }
    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public boolean isArchived() {
        return isArchived;
    }
    public void setArchived(boolean isArchived) {
        this.isArchived = isArchived;
    }
    public int getCustomerId() {
        return customerId;
    }
    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }
}
