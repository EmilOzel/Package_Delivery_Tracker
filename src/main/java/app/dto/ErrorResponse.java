package app.dto;

import lombok.Getter;

import java.time.Instant;

@Getter
public class ErrorResponse {

    private final int status;
    private final String message;
    private final String path;
    private final Instant timestamp;

    public ErrorResponse(int status, String message, String path) {
        this.status = status;
        this.message = message;
        this.path = path;
        this.timestamp = Instant.now();
    }
}
