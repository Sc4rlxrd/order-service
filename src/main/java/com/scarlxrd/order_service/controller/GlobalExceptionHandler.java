package com.scarlxrd.order_service.controller;

import com.scarlxrd.order_service.exception.RateLimitException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

    // 400 - VALIDATION ERROR
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation failed");
        problem.setDetail("Invalid request data");

        ex.getBindingResult().getFieldErrors().forEach(err ->
                problem.setProperty(err.getField(), err.getDefaultMessage())
        );

        return problem;
    }

    // 500 - GENERIC ERROR
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Internal Server Error");
        problem.setDetail("Unexpected error occurred");
        problem.setProperty("timestamp", ZonedDateTime.now(ZoneId.systemDefault()));
        return problem;
    }


    // 429 - RATE LIMIT
    @ExceptionHandler(RateLimitException.class)
    public ProblemDetail handleTooManyRequests(RateLimitException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.TOO_MANY_REQUESTS);
        problem.setTitle("Too Many Requests");
        problem.setDetail(ex.getMessage());
        problem.setProperty("timestamp", ZonedDateTime.now(ZoneId.systemDefault()));
        return problem;
    }

}
