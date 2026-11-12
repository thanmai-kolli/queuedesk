package com.queuedesk.validation;

import com.queuedesk.dto.CreateTicketRequest;
import com.queuedesk.dto.UpdateTicketRequest;
import com.queuedesk.exception.DuplicateTicketException;
import com.queuedesk.exception.StaleTicketException;
import com.queuedesk.exception.TicketValidationException;
import com.queuedesk.model.Escalatable;
import com.queuedesk.model.Status;
import com.queuedesk.model.Ticket;
import com.queuedesk.model.TicketType;
import com.queuedesk.repository.TicketRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks every create, update and escalate BEFORE anything is written to the database.
 *
 * Validation happens in three layers:
 *  1. @Valid + annotations on the request classes: required fields, lengths  -> 400
 *  2. this class: rules that depend on the ticket type or on the stored data  -> 400 / 409
 *  3. MySQL constraints: NOT NULL columns and the unique key                  -> 409
 * Layer 3 still matters: two requests can pass layer 2 at the same moment.
 */
@Component
public class TicketValidator {

    private final TicketRepository repository;

    // Constructor injection: Spring passes the TicketRepository bean in when it creates this class.
    public TicketValidator(TicketRepository repository) {
        this.repository = repository;
    }

    public void validateNew(CreateTicketRequest request) {
        List<String> errors = new ArrayList<>();
        TicketType type = request.type();

        switch (type) {
            case BUG -> {
                if (request.severity() == null) {
                    errors.add("severity: is required for a BUG ticket (MINOR, MAJOR or BLOCKER)");
                }
                if (isBlank(request.stepsToReproduce())) {
                    errors.add("stepsToReproduce: is required for a BUG ticket");
                }
            }
            case FEATURE -> {
                if (request.votes() == null) {
                    errors.add("votes: is required for a FEATURE ticket (0 or more)");
                }
                if (isBlank(request.businessJustification())) {
                    errors.add("businessJustification: is required for a FEATURE ticket");
                }
            }
            case ACCESS -> {
                if (isBlank(request.systemName())) {
                    errors.add("systemName: is required for an ACCESS ticket");
                }
                if (isBlank(request.accessLevel())) {
                    errors.add("accessLevel: is required for an ACCESS ticket (for example READ, WRITE, ADMIN)");
                }
            }
        }

        // Fields that belong to a different ticket type are a mistake, not something to ignore.
        rejectIfSent(errors, type, TicketType.BUG, "severity", request.severity());
        rejectIfSent(errors, type, TicketType.BUG, "stepsToReproduce", request.stepsToReproduce());
        rejectIfSent(errors, type, TicketType.FEATURE, "votes", request.votes());
        rejectIfSent(errors, type, TicketType.FEATURE, "businessJustification", request.businessJustification());
        rejectIfSent(errors, type, TicketType.ACCESS, "systemName", request.systemName());
        rejectIfSent(errors, type, TicketType.ACCESS, "accessLevel", request.accessLevel());

        if (!errors.isEmpty()) {
            throw new TicketValidationException(errors);
        }

        long existing = repository.countSameTicket(type.entityClass(), request.title().trim(), request.requester().trim());
        if (existing > 0) {
            throw new DuplicateTicketException("A " + type + " ticket titled '" + request.title().trim()
                    + "' from " + request.requester().trim() + " already exists");
        }
    }

    public void validateUpdate(Ticket ticket, UpdateTicketRequest request) {
        checkVersion(ticket, request.version());

        List<String> errors = new ArrayList<>();
        if (!request.hasChanges()) {
            errors.add("nothing to update: send at least one of status, priority, description, assignee, addTags");
        }
        if (ticket.getStatus() == Status.CLOSED) {
            errors.add("status: ticket #" + ticket.getId() + " is CLOSED and can no longer be changed");
        }
        if (request.priority() != null
                && ticket instanceof Escalatable escalatable && escalatable.isEscalated()
                && request.priority().compareTo(ticket.getPriority()) < 0) {
            errors.add("priority: an escalated ticket's priority cannot be lowered (it is "
                    + ticket.getPriority() + ")");
        }
        if (!errors.isEmpty()) {
            throw new TicketValidationException(errors);
        }
    }

    public void validateEscalation(Ticket ticket, Long version) {
        checkVersion(ticket, version);

        List<String> errors = new ArrayList<>();
        if (!(ticket instanceof Escalatable)) {
            errors.add(ticket.getType() + " tickets cannot be escalated (only BUG and ACCESS tickets can)");
        }
        if (ticket.getStatus() == Status.RESOLVED || ticket.getStatus() == Status.CLOSED) {
            errors.add("only OPEN or IN_PROGRESS tickets can be escalated; ticket #" + ticket.getId()
                    + " is " + ticket.getStatus());
        }
        if (!errors.isEmpty()) {
            throw new TicketValidationException(errors);
        }
    }

    // The client must have the latest version, otherwise it would overwrite a change it never saw.
    private void checkVersion(Ticket ticket, Long sentVersion) {
        if (sentVersion == null) {
            throw new TicketValidationException(List.of(
                    "version: is required: send the version you got when you last read the ticket"));
        }
        if (!sentVersion.equals(ticket.getVersion())) {
            throw new StaleTicketException(ticket.getId(), sentVersion, ticket.getVersion());
        }
    }

    private static void rejectIfSent(List<String> errors, TicketType actual, TicketType owner,
                                     String field, Object value) {
        boolean sent = value instanceof String text ? !text.isBlank() : value != null;
        if (actual != owner && sent) {
            errors.add(field + ": only belongs to " + owner + " tickets, not " + actual);
        }
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
