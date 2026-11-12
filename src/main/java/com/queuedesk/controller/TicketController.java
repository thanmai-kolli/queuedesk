package com.queuedesk.controller;

import com.queuedesk.dto.AssigneeGroupResponse;
import com.queuedesk.dto.CreateTicketRequest;
import com.queuedesk.dto.ImportResult;
import com.queuedesk.dto.PageResponse;
import com.queuedesk.dto.TicketResponse;
import com.queuedesk.dto.UpdateTicketRequest;
import com.queuedesk.exception.InvalidTicketDataException;
import com.queuedesk.exception.TicketNotFoundException;
import com.queuedesk.model.Status;
import com.queuedesk.model.Ticket;
import com.queuedesk.service.TicketService;
import com.queuedesk.service.TriageService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The ticket endpoints: the HTTP version of the old console menu options 1, 2, 3, 6, 9 and 10.
 *
 * A controller only deals with HTTP: it reads the request, calls a service, and turns the
 * result into a response. It contains no business rules and no database code.
 *
 * @RestController = @Controller + @ResponseBody: return values are written as JSON.
 * @RequestMapping("/api/tickets"): every path below starts with /api/tickets.
 */
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final Clock clock;

    public TicketController(TicketService ticketService, Clock clock) {
        this.ticketService = ticketService;
        this.clock = clock;
    }

    // POST /api/tickets  ->  201 Created, with a Location header pointing to the new ticket.
    // @Valid runs the annotations on CreateTicketRequest before this method starts.
    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicketRequest request) {
        Ticket ticket = ticketService.create(request);
        URI location = URI.create("/api/tickets/" + ticket.getId());
        return ResponseEntity.created(location).body(toResponse(ticket));
    }

    // GET /api/tickets?status=OPEN&assignee=asha&page=0&size=20&sort=createdAt,desc
    @GetMapping
    public PageResponse<TicketResponse> list(@RequestParam(required = false) Status status,
                                             @RequestParam(required = false) String assignee,
                                             @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        LocalDateTime now = now();
        return PageResponse.from(ticketService.findPage(status, assignee, pageable),
                ticket -> TicketResponse.from(ticket, now));
    }

    // GET /api/tickets/5  ->  200 with the ticket, or 404 if it doesn't exist.
    // Uses the Optional version of the lookup: map it to a response, or throw if empty.
    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable long id) {
        return ticketService.findByIdSafe(id)
                .map(this::toResponse)
                .orElseThrow(() -> new TicketNotFoundException(id));
    }

    // PATCH /api/tickets/5  with  {"version": 0, "status": "IN_PROGRESS"}
    @PatchMapping("/{id}")
    public TicketResponse update(@PathVariable long id, @Valid @RequestBody UpdateTicketRequest request) {
        return toResponse(ticketService.update(id, request));
    }

    // POST /api/tickets/5/escalate?version=0
    @PostMapping("/{id}/escalate")
    public TicketResponse escalate(@PathVariable long id, @RequestParam Long version) {
        return toResponse(ticketService.escalate(id, version));
    }

    // GET /api/tickets/by-assignee  ->  one group per assignee, A-Z, "Unassigned" included.
    @GetMapping("/by-assignee")
    public List<AssigneeGroupResponse> byAssignee() {
        LocalDateTime now = now();
        List<AssigneeGroupResponse> groups = new ArrayList<>();
        for (Map.Entry<String, List<Ticket>> entry : ticketService.groupByAssignee().entrySet()) {
            List<Ticket> tickets = entry.getValue();
            Ticket oldest = TriageService.oldest(tickets);   // the generic method from Milestone 4
            groups.add(new AssigneeGroupResponse(entry.getKey(), tickets.size(), oldest.getId(),
                    tickets.stream().map(t -> TicketResponse.from(t, now)).toList()));
        }
        return groups;
    }

    // POST /api/tickets/import  (multipart form, field "file")  ->  201 with what was imported.
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResult> importFile(@RequestParam("file") MultipartFile file)
            throws IOException, InvalidTicketDataException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("The uploaded file is empty");
        }
        TicketService.ImportOutcome outcome;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            outcome = ticketService.importTickets(reader);
        }
        LocalDateTime now = now();
        ImportResult result = new ImportResult(outcome.imported().size(), outcome.skipped(),
                outcome.imported().stream().map(t -> TicketResponse.from(t, now)).toList());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    private TicketResponse toResponse(Ticket ticket) {
        return TicketResponse.from(ticket, now());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
