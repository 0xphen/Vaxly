package com.vaxly.conversionservice.exceptions;

import com.vaxly.conversionservice.ConversionController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.UUID;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles downstream service failures.
     * Returns 503 with correlation ID and optional Retry-After.
     */
    @ExceptionHandler(DownStreamException.class)
    public ResponseEntity<ErrorResponse> handleDownstreamException(DownStreamException ex,
                                                                   HttpServletRequest request) {
        String correlationId = UUID.randomUUID().toString();

        ErrorResponse body = new ErrorResponse(
                Instant.now().toString(),
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Failed to retrieve historical rate for currency pair",
                correlationId
        );

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.RETRY_AFTER, "30");
        headers.add("X-Correlation-ID", correlationId);

        return new ResponseEntity<>(body, headers, HttpStatus.SERVICE_UNAVAILABLE);
    }

    // DTO for error response
    public static class ErrorResponse {
        private String timestamp;
        private int status;
        private String message;
        private String correlationId;

        public ErrorResponse(String timestamp, int status, String message, String correlationId) {
            this.timestamp = timestamp;
            this.status = status;
            this.message = message;
            this.correlationId = correlationId;
        }

        public String getTimestamp() { return timestamp; }
        public int getStatus() { return status; }
        public String getMessage() { return message; }
        public String getCorrelationId() { return correlationId; }
    }
}
