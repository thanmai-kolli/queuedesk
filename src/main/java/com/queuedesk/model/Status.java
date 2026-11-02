package com.queuedesk.model;

/**
 * Where a ticket is in its life: OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED.
 */
public enum Status {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED
}
