package com.queuedesk.exception;

/**
 * Milestone 5: an UNCHECKED exception (it extends RuntimeException).
 *
 * Thrown when someone asks for a ticket ID that does not exist.
 * Callers are not forced to catch it; GlobalExceptionHandler turns it into 404 Not Found.
 */
public class TicketNotFoundException extends RuntimeException {

    private final long ticketId;

    public TicketNotFoundException(long ticketId) {
        super("No ticket found with ID " + ticketId);
        this.ticketId = ticketId;
    }

    public long getTicketId() {
        return ticketId;
    }
}
