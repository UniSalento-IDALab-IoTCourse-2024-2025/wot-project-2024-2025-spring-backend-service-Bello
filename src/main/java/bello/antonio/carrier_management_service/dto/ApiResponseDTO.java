package bello.antonio.carrier_management_service.dto;

public class ApiResponseDTO<T> {

    private String message;
    private int status;
    private long timestamp;
    private T body;

    public ApiResponseDTO(String message, int status, T body) {
        this.message = message;
        this.status = status;
        this.body = body;
        this.timestamp = System.currentTimeMillis();
    }

    public String getMessage() { return message; }
    public int getStatus() { return status; }
    public long getTimestamp() { return timestamp; }
    public T getBody() { return body; }
}


