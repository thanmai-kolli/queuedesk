package com.queuedesk.dto;

import com.queuedesk.model.Priority;
import com.queuedesk.model.Status;
import com.queuedesk.model.Ticket;
import com.queuedesk.model.TicketType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * What the API sends back for one ticket.
 *
 * Why not return the Ticket entity itself? The entity is shaped for the database; this class
 * is shaped for the client. Keeping them apart means a database change doesn't break the API,
 * and only chosen fields are ever exposed.
 */
public record TicketResponse(
        Long id,
        TicketType type,
        String typeName,
        String title,
        String description,
        String requester,
        Priority priority,
        Status status,
        String assignee,
        Set<String> tags,
        LocalDateTime createdAt,
        long ageInHours,
        double estimatedEffortHours,
        Long version,
        Map<String, Object> details) {

    // typeDetails() and estimateEffortHours() are polymorphic: each subclass fills in its own.
    public static TicketResponse from(Ticket ticket, LocalDateTime now) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTicketType(),
                ticket.getType(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getRequester(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getAssignee(),
                ticket.getTags(),
                ticket.getCreatedAt(),
                ticket.ageInHours(now),
                ticket.estimateEffortHours(),
                ticket.getVersion(),
                ticket.typeDetails());
    }
}
