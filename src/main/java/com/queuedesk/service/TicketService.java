package com.queuedesk.service;

import com.queuedesk.dto.AnalyticsResponse;
import com.queuedesk.dto.CreateTicketRequest;
import com.queuedesk.dto.TicketResponse;
import com.queuedesk.dto.UpdateTicketRequest;
import com.queuedesk.exception.InvalidTicketDataException;
import com.queuedesk.exception.TicketNotFoundException;
import com.queuedesk.model.AccessRequestTicket;
import com.queuedesk.model.BugTicket;
import com.queuedesk.model.Escalatable;
import com.queuedesk.model.FeatureRequestTicket;
import com.queuedesk.model.Priority;
import com.queuedesk.model.Status;
import com.queuedesk.model.Ticket;
import com.queuedesk.repository.TicketRepository;
import com.queuedesk.validation.TicketValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * The business logic for tickets. Controllers call this class; this class calls the validator
 * and the repository.
 *
 * @Transactional: everything inside the method happens in ONE database transaction.
 * If the method throws, every change it made is rolled back, as if it never happened.
 * readOnly = true tells the database (and Hibernate) that nothing will be written.
 */
@Service
public class TicketService {

    private final TicketRepository repository;
    private final TicketValidator validator;
    private final TicketFileLoader fileLoader;

    public TicketService(TicketRepository repository, TicketValidator validator, TicketFileLoader fileLoader) {
        this.repository = repository;
        this.validator = validator;
        this.fileLoader = fileLoader;
    }

    // ---------- create ----------

    @Transactional
    public Ticket create(CreateTicketRequest request) {
        validator.validateNew(request);   // throws if anything is wrong, so nothing is saved

        Ticket ticket = switch (request.type()) {
            case BUG -> new BugTicket(request.title(), request.description(), request.requester(),
                    request.priority(), request.severity(), request.stepsToReproduce());
            case FEATURE -> new FeatureRequestTicket(request.title(), request.description(), request.requester(),
                    request.priority(), request.votes(), request.businessJustification());
            case ACCESS -> new AccessRequestTicket(request.title(), request.description(), request.requester(),
                    request.priority(), request.systemName(), request.accessLevel());
        };
        ticket.setAssignee(request.assignee());
        if (request.tags() != null) {
            request.tags().forEach(ticket::addTag);
        }
        // saveAndFlush runs the INSERT now, so a database error (e.g. the unique key) happens here.
        return repository.saveAndFlush(ticket);
    }

    // ---------- read ----------

    // Milestone 6: the lookup returns an Optional; "not found" is a normal answer.
    @Transactional(readOnly = true)
    public Optional<Ticket> findByIdSafe(long id) {
        return repository.findById(id);
    }

    // The same lookup, but "not found" is an error (TicketNotFoundException -> 404).
    @Transactional(readOnly = true)
    public Ticket findById(long id) {
        return findByIdSafe(id).orElseThrow(() -> new TicketNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Ticket> findAll() {
        return repository.findAll();
    }

    // Paging and optional filters, using the derived query methods of TicketRepository.
    @Transactional(readOnly = true)
    public Page<Ticket> findPage(Status status, String assignee, Pageable pageable) {
        boolean hasAssignee = assignee != null && !assignee.isBlank();
        if (status != null && hasAssignee) {
            return repository.findByStatusAndAssigneeIgnoreCase(status, assignee.trim(), pageable);
        }
        if (status != null) {
            return repository.findByStatus(status, pageable);
        }
        if (hasAssignee) {
            return repository.findByAssigneeIgnoreCase(assignee.trim(), pageable);
        }
        return repository.findAll(pageable);
    }

    // Milestone 4's loop version of "group by assignee". TicketAnalytics has the stream version.
    @Transactional(readOnly = true)
    public Map<String, List<Ticket>> groupByAssignee() {
        Map<String, List<Ticket>> groups = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Ticket ticket : repository.findAll()) {
            String name = ticket.getAssignee() == null ? "Unassigned" : ticket.getAssignee();
            groups.computeIfAbsent(name, key -> new ArrayList<>()).add(ticket);
        }
        return groups;
    }

    // ---------- update ----------

    /**
     * Inside this transaction the ticket is "managed": Hibernate notices every change made with
     * the setters and writes them at the end. saveAndFlush makes that happen right here, and the
     * @Version check runs with it: if another request saved the ticket in the meantime,
     * Hibernate throws ObjectOptimisticLockingFailureException (-> 409 Conflict).
     */
    @Transactional
    public Ticket update(long id, UpdateTicketRequest request) {
        Ticket ticket = findById(id);
        validator.validateUpdate(ticket, request);

        if (request.status() != null) {
            ticket.setStatus(request.status());
        }
        if (request.priority() != null) {
            ticket.setPriority(request.priority());
        }
        if (request.description() != null) {
            ticket.setDescription(request.description());
        }
        if (request.assignee() != null) {
            ticket.setAssignee(request.assignee());   // "" removes the assignee
        }
        if (request.addTags() != null) {
            request.addTags().forEach(ticket::addTag);
        }
        return repository.saveAndFlush(ticket);
    }

    @Transactional
    public Ticket escalate(long id, Long version) {
        Ticket ticket = findById(id);
        validator.validateEscalation(ticket, version);
        ((Escalatable) ticket).escalate();   // safe: the validator checked it is Escalatable
        return repository.saveAndFlush(ticket);
    }

    // ---------- import a text file (Milestone 5's loader) ----------

    /**
     * Reads every line first; if any line is wrong, InvalidTicketDataException is thrown before
     * anything is saved. Tickets that already exist are skipped. The whole import is one
     * transaction: if saving fails half-way, all of it is rolled back.
     *
     * rollbackFor = Exception.class: by default Spring only rolls back for unchecked exceptions;
     * this makes the checked ones (IOException, InvalidTicketDataException) roll back too.
     */
    @Transactional(rollbackFor = Exception.class)
    public ImportOutcome importTickets(BufferedReader reader) throws IOException, InvalidTicketDataException {
        List<Ticket> loaded = fileLoader.load(reader);
        List<Ticket> saved = new ArrayList<>();
        int skipped = 0;
        for (Ticket ticket : loaded) {
            long existing = repository.countSameTicket(ticket.getClass(), ticket.getTitle(), ticket.getRequester());
            if (existing > 0) {
                skipped++;
            } else {
                saved.add(repository.save(ticket));
            }
        }
        repository.flush();
        return new ImportOutcome(saved, skipped);
    }

    @Transactional(rollbackFor = Exception.class)
    public ImportOutcome importFile(Path file) throws IOException, InvalidTicketDataException {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            return importTickets(reader);
        }
    }

    public record ImportOutcome(List<Ticket> imported, int skipped) {
    }

    // ---------- analytics (Milestone 6 streams + Month 2 queries) ----------

    // One read-only transaction, so every number comes from the same moment in the database.
    @Transactional(readOnly = true)
    public AnalyticsResponse analytics(LocalDateTime now) {
        List<Ticket> tickets = repository.findAll();

        // The map from groupingBy only has priorities that appear; fill in the zeros.
        Map<Priority, Long> perPriority = new EnumMap<>(Priority.class);
        Map<Priority, Long> counted = TicketAnalytics.countByPriority(tickets);
        for (Priority priority : Priority.values()) {
            perPriority.put(priority, counted.getOrDefault(priority, 0L));
        }

        Map<Status, Long> perStatus = new EnumMap<>(Status.class);
        for (Status status : Status.values()) {
            perStatus.put(status, 0L);
        }
        for (TicketRepository.StatusCount row : repository.countByStatus()) {
            perStatus.put(row.getStatus(), row.getTotal());
        }

        Map<String, Integer> perAssignee = new TreeMap<>();
        TicketAnalytics.groupByAssignee(tickets)
                .forEach((assignee, list) -> perAssignee.put(assignee, list.size()));

        return new AnalyticsResponse(
                tickets.size(),
                perPriority,
                perStatus,
                perAssignee,
                TicketAnalytics.averageEffortHours(tickets),
                TicketAnalytics.totalEffortHoursLoop(tickets),
                TicketAnalytics.totalEffortHours(tickets),
                TicketAnalytics.oldestOpenTicket(tickets).map(t -> TicketResponse.from(t, now)).orElse(null),
                TicketAnalytics.tagLine(tickets),
                repository.countOverdueCritical(now.minusHours(24)));
    }
}
