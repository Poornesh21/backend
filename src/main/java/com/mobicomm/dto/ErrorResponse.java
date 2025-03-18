package com.mobicomm.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Comprehensive Error Response DTO with user-friendly error messages
 */
public class ErrorResponse {
    private String message;      // Primary user-friendly error message
    private LocalDateTime timestamp;
    private int status;          // HTTP status code
    private String supportCode;  // Optional support reference code
    private List<String> details; // Additional error details

    // Default constructor
    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
        this.details = new ArrayList<>();
        generateSupportCode();
    }

    // Constructor with message
    public ErrorResponse(String message) {
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.details = new ArrayList<>();
        generateSupportCode();
    }

    // Constructor with message and status
    public ErrorResponse(String message, int status) {
        this.message = message;
        this.status = status;
        this.timestamp = LocalDateTime.now();
        this.details = new ArrayList<>();
        generateSupportCode();
    }

    // Constructor with message, status, and additional details
    public ErrorResponse(String message, int status, List<String> details) {
        this.message = message;
        this.status = status;
        this.timestamp = LocalDateTime.now();
        this.details = details != null ? details : new ArrayList<>();
        generateSupportCode();
    }

    // Generate a unique support code for tracking
    private void generateSupportCode() {
        // Create a simple support code based on timestamp
        this.supportCode = "ERR-" +
                String.format("%04d", Math.abs(timestamp.hashCode() % 10000));
    }

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getSupportCode() {
        return supportCode;
    }

    public void setSupportCode(String supportCode) {
        this.supportCode = supportCode;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }

    /**
     * Add an additional error detail
     */
    public void addDetail(String detail) {
        if (this.details == null) {
            this.details = new ArrayList<>();
        }
        this.details.add(detail);
    }

    /**
     * Provides a user-friendly error message with support code
     */
    @Override
    public String toString() {
        return String.format(
                "Error: %s (Support Code: %s)",
                message,
                supportCode
        );
    }
}