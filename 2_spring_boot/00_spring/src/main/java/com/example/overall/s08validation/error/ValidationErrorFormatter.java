package com.example.overall.s8validation.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

// Console equivalent of the clean error response you would normally build in @ControllerAdvice.
@Component
public class ValidationErrorFormatter {

    public String format(ConstraintViolationException exception) {
        return exception.getConstraintViolations().stream()
                .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
                .map(this::formatOne)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private String formatOne(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        String fieldName = path.substring(path.lastIndexOf('.') + 1);
        return "E1. " + fieldName + " -> " + violation.getMessage();
    }
}
