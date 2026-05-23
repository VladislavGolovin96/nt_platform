package com.loadtest.project.exception;

import com.loadtest.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(1)
public class ProjectExceptionHandler {

    @ExceptionHandler(ProjectBuildException.class)
    public ResponseEntity<ErrorResponse> handleBuildFailure(
            ProjectBuildException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(422, "Build Failed", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArg(
            IllegalArgumentException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(400, "Bad Request", ex.getMessage(), request.getRequestURI()));
    }
}
