package com.queuedesk.dto;

import java.util.List;

/**
 * The result of uploading a text file of tickets (POST /api/tickets/import).
 * Tickets that already exist (same type, title and requester) are skipped, not duplicated.
 */
public record ImportResult(
        int imported,
        int skipped,
        List<TicketResponse> importedTickets) {
}
