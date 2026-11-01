package net.lucid.moa.employee.mock;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import net.lucid.moa.employee.controller.EmployeeController;
import net.lucid.moa.employee.mock.contract.MockApiResponse;

/** Mock-only error handling; TODO replace with common advice and ErrorCode. */
@RestControllerAdvice(assignableTypes = EmployeeController.class)
@Profile("mock & !prod")
public class MockEmployeeExceptionHandler {
    @ExceptionHandler(MockEmployeeException.class)
    public ResponseEntity<MockApiResponse<Void>> handleMockError(MockEmployeeException exception) {
        return ResponseEntity.status(exception.status()).body(MockApiResponse.failure(exception.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HandlerMethodValidationException.class,
            HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<MockApiResponse<Void>> handleBadRequest(Exception exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(MockApiResponse.failure("Invalid employee request"));
    }
}
