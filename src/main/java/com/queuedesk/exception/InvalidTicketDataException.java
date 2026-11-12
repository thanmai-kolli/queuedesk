package com.queuedesk.exception;

/**
 * Milestone 5: a CHECKED exception (it extends Exception).
 *
 * Thrown when a line in a ticket text file is wrong. Because it is checked, the compiler
 * forces the caller to handle it with try/catch or declare it with "throws".
 * The REST API turns it into a 400 Bad Request that names the line.
 */
public class InvalidTicketDataException extends Exception {

    private final int lineNumber;

    public InvalidTicketDataException(String message, int lineNumber, Throwable cause) {
        super(message, cause);
        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
