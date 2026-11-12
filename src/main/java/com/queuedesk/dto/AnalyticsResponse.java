package com.queuedesk.dto;

import com.queuedesk.model.Priority;
import com.queuedesk.model.Status;

import java.util.Map;

/**
 * GET /api/analytics. Most numbers come from Milestone 6's TicketAnalytics (Java Streams).
 * The two "...FromDatabase" numbers come from SQL queries in TicketRepository instead,
 * so you can compare counting in Java with counting in the database.
 */
public record AnalyticsResponse(
        long totalTickets,
        Map<Priority, Long> ticketsPerPriority,
        Map<Status, Long> ticketsPerStatusFromDatabase,
        Map<String, Integer> ticketsPerAssignee,
        double averageEffortHours,
        double totalEffortHoursLoop,
        double totalEffortHoursStream,
        TicketResponse oldestOpenTicket,
        String allTags,
        long overdueCriticalFromDatabase) {
}
