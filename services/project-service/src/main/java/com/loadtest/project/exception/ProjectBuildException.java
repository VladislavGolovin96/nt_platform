package com.loadtest.project.exception;

public class ProjectBuildException extends RuntimeException {

    public ProjectBuildException(String message) {
        super(message);
    }

    public ProjectBuildException(String message, Throwable cause) {
        super(message, cause);
    }
}
