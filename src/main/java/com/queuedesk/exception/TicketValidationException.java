package com.queuedesk.exception;

import java.util.List;

/**
 * Thrown by TicketValidator when a request breaks a business rule
 * (for example "severity is required for a BUG ticket"). Becomes 400 Bad Request.
 * It carries every problem found, so the caller can fix them all at once.
 */
public class TicketValidationException extends RuntimeException {

    private final List<String> errors;

    public TicketValidationException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
