package com.queuedesk.controller;

import com.queuedesk.dto.TicketResponse;
import com.queuedesk.model.Ticket;
import com.queuedesk.service.QueueOrder;
import com.queuedesk.service.TicketService;
import com.queuedesk.service.TriageBoard;
import com.queuedesk.service.TriageService;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Triage endpoints: the HTTP version of console options 2 (board), 4 (queue) and 5 (serve next).
 */
@RestController
@RequestMapping("/api/triage")
public class TriageController {

    private final TriageService triageService;
    private final TicketService ticketService;
    private final Clock clock;

    public TriageController(TriageService triageService, TicketService ticketService, Clock clock) {
        this.triageService = triageService;
        this.ticketService = ticketService;
        this.clock = clock;
    }

    // GET /api/triage?order=PRIORITY  ->  the OPEN tickets in the order they would be served.
    @GetMapping
    public List<TicketResponse> queue(@RequestParam(defaultValue = "PRIORITY") QueueOrder order) {
        LocalDateTime now = LocalDateTime.now(clock);
        return triageService.waitingInOrder(order).stream()
                .map(ticket -> TicketResponse.from(ticket, now))
                .toList();
    }

    // POST /api/triage/serve-next?order=PRIORITY&assignee=Asha
    // 200 with the ticket that is now IN_PROGRESS, or 204 No Content when nothing is waiting.
    @PostMapping("/serve-next")
    public ResponseEntity<TicketResponse> serveNext(
            @RequestParam(defaultValue = "PRIORITY") QueueOrder order,
            @RequestParam(required = false) @Size(max = 100, message = "must be at most 100 characters") String assignee) {
        LocalDateTime now = LocalDateTime.now(clock);
        return triageService.serveNext(order, assignee)
                .map(ticket -> ResponseEntity.ok(TicketResponse.from(ticket, now)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    // GET /api/triage/board  ->  plain text, every ticket in natural order (Milestone 3's board).
    @GetMapping(value = "/board", produces = MediaType.TEXT_PLAIN_VALUE)
    public String board() {
        List<Ticket> tickets = new ArrayList<>(ticketService.findAll());
        Collections.sort(tickets);   // Ticket.compareTo: most urgent first, then oldest
        return TriageBoard.renderTriageBoard(tickets, LocalDateTime.now(clock));
    }
}
