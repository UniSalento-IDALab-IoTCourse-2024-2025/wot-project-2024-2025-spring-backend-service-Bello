package bello.antonio.carrier_management_service.dto;

public class ApiResponseDTO {
    private String message;
    private int status;
    private long timestamp;

    public ApiResponseDTO(String message, int status) {
        this.message = message;
        this.status = status;
        this.timestamp = System.currentTimeMillis();
    }

    public String getMessage() { return message; }
    public int getStatus() { return status; }
    public long getTimestamp() { return timestamp; }
}

