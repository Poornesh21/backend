package com.mobicomm.exception;

import com.mobicomm.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Comprehensive Global Exception Handler for MobiComm Application
 * Provides detailed, user-friendly error responses
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle specific Resource Not Found Exceptions
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            WebRequest request
    ) {
        // Log the error for internal tracking
        logger.error("Resource Not Found: {}", ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
                "Sorry, we couldn't find the information you're looking for.",
                HttpStatus.NOT_FOUND.value()
        );
        error.addDetail(ex.getMessage());

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle validation errors for request body
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex
    ) {
        // Log the validation errors
        logger.error("Validation Error: {}", ex.getMessage(), ex);

        // Collect all validation errors
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatValidationError)
                .collect(Collectors.toList());

        ErrorResponse error = new ErrorResponse(
                "Please check your input and try again.",
                HttpStatus.BAD_REQUEST.value(),
                errors
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle custom business logic exceptions
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex,
            WebRequest request
    ) {
        // Log the business logic error
        logger.error("Business Logic Error: {}", ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
                "Operation could not be completed due to a business rule violation.",
                HttpStatus.UNPROCESSABLE_ENTITY.value()
        );
        error.addDetail(ex.getMessage());

        return new ResponseEntity<>(error, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     * Handle authentication-related exceptions
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex,
            WebRequest request
    ) {
        // Log the authentication error
        logger.error("Authentication Error: {}", ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
                "We couldn't verify your identity. Please check your credentials.",
                HttpStatus.UNAUTHORIZED.value()
        );
        error.addDetail(ex.getMessage());

        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handle payment-related exceptions
     */
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorResponse> handlePaymentException(
            PaymentException ex,
            WebRequest request
    ) {
        // Log the payment error
        logger.error("Payment Error: {}", ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
                "We encountered an issue processing your payment. Please try again.",
                HttpStatus.PAYMENT_REQUIRED.value()
        );
        error.addDetail(ex.getMessage());

        return new ResponseEntity<>(error, HttpStatus.PAYMENT_REQUIRED);
    }

    /**
     * Handle plan-related exceptions
     */
    @ExceptionHandler(PlanException.class)
    public ResponseEntity<ErrorResponse> handlePlanException(
            PlanException ex,
            WebRequest request
    ) {
        // Log the plan-related error
        logger.error("Plan Error: {}", ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
                "We couldn't process your plan request. Please contact support.",
                HttpStatus.BAD_REQUEST.value()
        );
        error.addDetail(ex.getMessage());

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Catch-all handler for unexpected exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            WebRequest request
    ) {
        // Log the full stack trace for internal tracking
        logger.error("Unexpected Error: {}", ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
                "We're experiencing a temporary issue. Our team has been notified.",
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        error.addDetail("An unexpected error occurred. Please try again later.");

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Format validation errors in a user-friendly manner
     */
    private String formatValidationError(FieldError error) {
        return error.getField() + " " + error.getDefaultMessage();
    }
}