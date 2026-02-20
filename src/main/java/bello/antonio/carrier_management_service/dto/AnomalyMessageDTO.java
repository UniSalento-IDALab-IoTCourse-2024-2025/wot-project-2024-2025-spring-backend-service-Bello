package bello.antonio.carrier_management_service.dto;

public class AnomalyMessageDTO {
    private String vehicleName;
    private String timestamp;
    private int rowIndex;
    private double reconstructionError;
    private int isAnomaly;
    private int anomalyCounter;
    private int normalCounter;
    private boolean anomalyDetected;
    private String alertMessage;

    public String getAlertMessage() {
        return alertMessage;
    }

    public void setAlertMessage(String alertMessage) {
        this.alertMessage = alertMessage;
    }

    public boolean isAnomalyDetected() {
        return anomalyDetected;
    }

    public void setAnomalyDetected(boolean anomalyDetected) {
        this.anomalyDetected = anomalyDetected;
    }

    public int getNormalCounter() {
        return normalCounter;
    }

    public void setNormalCounter(int normalCounter) {
        this.normalCounter = normalCounter;
    }

    public int getAnomalyCounter() {
        return anomalyCounter;
    }

    public void setAnomalyCounter(int anomalyCounter) {
        this.anomalyCounter = anomalyCounter;
    }

    public int getIsAnomaly() {
        return isAnomaly;
    }

    public void setIsAnomaly(int isAnomaly) {
        this.isAnomaly = isAnomaly;
    }

    public double getReconstructionError() {
        return reconstructionError;
    }

    public void setReconstructionError(double reconstructionError) {
        this.reconstructionError = reconstructionError;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public void setRowIndex(int rowIndex) {
        this.rowIndex = rowIndex;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getVehicleName() {
        return vehicleName;
    }

    public void setVehicleName(String vehicleName) {
        this.vehicleName = vehicleName;
    }
}
