package com.queuedesk.dto;

import com.queuedesk.model.Priority;
import com.queuedesk.model.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * The JSON body of PATCH /api/tickets/{id}. Send only what you want to change;
 * a missing (null) field is left as it is. An empty assignee ("") removes the assignee.
 *
 * version is required: it is the version you got when you last read the ticket.
 * If someone changed the ticket since then, the update is refused with 409 Conflict,
 * so you never overwrite a change you haven't seen.
 */
public record UpdateTicketRequest(
        @NotNull(message = "is required: send the version you got when you last read the ticket")
        Long version,

        Status status,

        Priority priority,

        @Size(max = 2000, message = "must be at most 2000 characters")
        String description,

        @Size(max = 100, message = "must be at most 100 characters")
        String assignee,

        @Size(max = 10, message = "can have at most 10 tags")
        List<@NotBlank(message = "a tag cannot be empty")
             @Size(max = 50, message = "a tag must be at most 50 characters") String> addTags) {

    public boolean hasChanges() {
        return status != null || priority != null || description != null || assignee != null
                || (addTags != null && !addTags.isEmpty());
    }
}
