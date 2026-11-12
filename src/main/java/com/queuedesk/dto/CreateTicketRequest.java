package com.queuedesk.dto;

import com.queuedesk.model.Priority;
import com.queuedesk.model.Severity;
import com.queuedesk.model.TicketType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * The JSON body of POST /api/tickets. A record: a short immutable class with
 * a constructor, getters (type(), title(), ...), equals, hashCode and toString.
 *
 * The annotations are the FIRST validation layer (checked by @Valid in the controller):
 * required fields and lengths. Rules that depend on the ticket type or on the database
 * are checked afterwards by TicketValidator.
 *
 * Fields for the other ticket types are left out (null). For example a BUG needs
 * severity and stepsToReproduce, and must not send votes.
 */
public record CreateTicketRequest(
        @NotNull(message = "is required: BUG, FEATURE or ACCESS")
        TicketType type,

        @NotBlank(message = "is required")
        @Size(max = 200, message = "must be at most 200 characters")
        String title,

        @Size(max = 2000, message = "must be at most 2000 characters")
        String description,

        @NotBlank(message = "is required")
        @Size(max = 100, message = "must be at most 100 characters")
        String requester,

        @NotNull(message = "is required: LOW, MEDIUM, HIGH or CRITICAL")
        Priority priority,

        // BUG only
        Severity severity,
        @Size(max = 2000, message = "must be at most 2000 characters")
        String stepsToReproduce,

        // FEATURE only
        @PositiveOrZero(message = "cannot be negative")
        Integer votes,
        @Size(max = 2000, message = "must be at most 2000 characters")
        String businessJustification,

        // ACCESS only
        @Size(max = 100, message = "must be at most 100 characters")
        String systemName,
        @Size(max = 50, message = "must be at most 50 characters")
        String accessLevel,

        // Any type, optional
        @Size(max = 100, message = "must be at most 100 characters")
        String assignee,

        @Size(max = 10, message = "can have at most 10 tags")
        List<@NotBlank(message = "a tag cannot be empty")
             @Size(max = 50, message = "a tag must be at most 50 characters") String> tags) {
}
