package com.queuedesk.dto;

import java.util.List;

/**
 * The tickets of one assignee (GET /api/tickets/by-assignee).
 * oldestTicketId comes from the generic method TriageService.oldest (Milestone 4).
 */
public record AssigneeGroupResponse(
        String assignee,
        int count,
        Long oldestTicketId,
        List<TicketResponse> tickets) {
}
