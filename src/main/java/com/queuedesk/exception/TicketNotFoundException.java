package com.queuedesk.exception;

/**
 * Milestone 5: an UNCHECKED exception (it extends RuntimeException).
 *
 * Thrown when someone asks for a ticket ID that does not exist.
 * The compiler does not force callers to catch it.
 */
public class TicketNotFoundException extends RuntimeException {

    private final int ticketId;

    public TicketNotFoundException(int ticketId) {
        super("No ticket found with ID " + ticketId);
        this.ticketId = ticketId;
    }

    public int getTicketId() {
        return ticketId;
    }
}
