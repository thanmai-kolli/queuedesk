package com.queuedesk.service;

/**
 * Which of the two triage queues to use.
 * PRIORITY: Ticket's natural order (Comparable): most urgent first, then oldest.
 * ASSIGNEE: TicketComparators.BY_ASSIGNEE_THEN_PRIORITY (a Comparator).
 */
public enum QueueOrder {
    PRIORITY,
    ASSIGNEE
}
