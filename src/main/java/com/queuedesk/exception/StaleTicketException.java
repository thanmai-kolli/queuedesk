package com.queuedesk.exception;

/**
 * The client sent an old version of a ticket: someone else changed it after the client read it.
 * Becomes 409 Conflict. The client should GET the ticket again and retry with the new version.
 */
public class StaleTicketException extends RuntimeException {

    public StaleTicketException(long ticketId, long sentVersion, long currentVersion) {
        super("Ticket #" + ticketId + " was changed by someone else: you sent version " + sentVersion
                + " but the current version is " + currentVersion + ". Read the ticket again and retry.");
    }
}
