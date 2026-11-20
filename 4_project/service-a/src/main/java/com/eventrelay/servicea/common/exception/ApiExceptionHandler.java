package com.eventrelay.servicea.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static com.eventrelay.servicea.ingress.IncomingRequestContext.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> status(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(ProblemDetail.forStatusAndDetail(ex.getStatusCode(),
                ex.getReason() == null ? "Request failed" : ex.getReason()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException ex) {
        reason(Reason.INVALID_FIELDS);
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> conflict(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                        "Registration conflicts with an existing record"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> malformed(HttpMessageNotReadableException ex) {
        reason(Reason.MALFORMED_REQUEST);
        return ResponseEntity.badRequest()
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed or unreadable request body"));
    }
}
