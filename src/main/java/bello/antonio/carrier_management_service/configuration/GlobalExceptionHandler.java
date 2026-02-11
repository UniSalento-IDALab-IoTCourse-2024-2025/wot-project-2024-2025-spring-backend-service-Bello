package bello.antonio.carrier_management_service.configuration;

import bello.antonio.carrier_management_service.dto.ApiResponseDTO;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 400: richiesta non valida
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponseDTO<Object>> handleIllegalArgument(IllegalArgumentException ex) {
        ApiResponseDTO<Object> response = new ApiResponseDTO<>(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 409: conflitti (es. dati duplicati)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponseDTO<Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        ApiResponseDTO<Object> response = new ApiResponseDTO<>(
                "Data conflict: " + ex.getRootCause().getMessage(),
                HttpStatus.CONFLICT.value(),
                null
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // 500: errore generico
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDTO<Object>> handleAllExceptions(Exception ex) {
        ex.printStackTrace(); // log per debug
        ApiResponseDTO<Object> response = new ApiResponseDTO<>(
                "Internal server error: " + ex.getClass().getSimpleName() + " - " + ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

}
