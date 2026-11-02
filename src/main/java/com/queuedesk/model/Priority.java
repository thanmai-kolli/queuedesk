package com.queuedesk.model;

/**
 * How urgent a ticket is.
 * The order of the values matters: enums compare in the order they are written,
 * so LOW is the smallest and CRITICAL is the biggest.
 */
public enum Priority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
