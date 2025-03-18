package com.mobicomm.exception;

public class PlanException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public PlanException(String message) {
        super(message);
    }

    public PlanException(String message, Throwable cause) {
        super(message, cause);
    }
}