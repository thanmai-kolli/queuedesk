package com.queuedesk.exception;

/**
 * A ticket with the same type, title and requester already exists. Becomes 409 Conflict.
 */
public class DuplicateTicketException extends RuntimeException {

    public DuplicateTicketException(String message) {
        super(message);
    }
}
