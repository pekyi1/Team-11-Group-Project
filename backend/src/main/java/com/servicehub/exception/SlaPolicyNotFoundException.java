package com.servicehub.exception;

/**
 * Exception thrown when an SLA policy is not found.
 * Extends ResourceNotFoundException for consistent error handling.
 */
public class SlaPolicyNotFoundException extends ResourceNotFoundException {
    
    public SlaPolicyNotFoundException(String message) {
        super(message);
    }
    
    public SlaPolicyNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(resourceName, fieldName, fieldValue);
    }
}

