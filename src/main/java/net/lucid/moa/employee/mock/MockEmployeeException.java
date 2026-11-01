package net.lucid.moa.employee.mock;

import org.springframework.http.HttpStatus;

/** Temporary mock errors, unrelated to the unavailable common ErrorCode contract. */
public class MockEmployeeException extends RuntimeException {
    private final HttpStatus status;

    public MockEmployeeException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
