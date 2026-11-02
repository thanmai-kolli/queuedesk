package com.queuedesk.service;

import com.queuedesk.exception.InvalidTicketDataException;
import com.queuedesk.model.AccessRequestTicket;
import com.queuedesk.model.BugTicket;
import com.queuedesk.model.FeatureRequestTicket;
import com.queuedesk.model.Priority;
import com.queuedesk.model.Severity;
import com.queuedesk.model.Ticket;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Milestone 5: reads tickets from a text file, one ticket per line.
 *
 * Line format (fields separated by |):
 *   bug|title|description|requester|PRIORITY|SEVERITY|steps to reproduce
 *   feature|title|description|requester|PRIORITY|votes|business justification
 *   access|title|description|requester|PRIORITY|system name|access level
 * An optional 8th field is the creation time, for example 2026-10-30T09:00.
 * Empty lines and lines starting with # are skipped.
 *
 * If ANY line is wrong, nothing is returned: the caller gets an exception
 * that says which line is wrong.
 */
public class TicketFileLoader {

    public List<Ticket> load(Path file) throws IOException, InvalidTicketDataException {
        List<Ticket> tickets = new ArrayList<>();

        // try-with-resources: the reader is closed automatically, even if an exception happens.
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank() || line.trim().startsWith("#")) {
                    continue;
                }
                try {
                    tickets.add(parseLine(line));
                } catch (IllegalArgumentException e) {
                    // Turn a low-level problem into our own exception, adding the line number.
                    throw new InvalidTicketDataException(
                            "Line " + lineNumber + ": " + e.getMessage(), lineNumber, e);
                }
            }
        }
        return tickets;
    }

    private Ticket parseLine(String line) {
        // -1 keeps empty fields at the end, so "a|b|" gives 3 fields, not 2.
        String[] fields = line.split("\\|", -1);
        if (fields.length != 7 && fields.length != 8) {
            throw new IllegalArgumentException("expected 7 or 8 fields separated by |, but found " + fields.length);
        }
        for (int i = 0; i < fields.length; i++) {
            fields[i] = fields[i].trim();
        }
        for (int i = 0; i < 7; i++) {
            if (fields[i].isEmpty()) {
                throw new IllegalArgumentException("field " + (i + 1) + " is empty");
            }
        }

        String type = fields[0].toLowerCase();
        String title = fields[1];
        String description = fields[2];
        String requester = fields[3];
        Priority priority = parseEnum(Priority.class, fields[4], "priority");

        Ticket ticket = switch (type) {
            case "bug" -> new BugTicket(title, description, requester, priority,
                    parseEnum(Severity.class, fields[5], "severity"), fields[6]);
            case "feature" -> new FeatureRequestTicket(title, description, requester, priority,
                    parseVotes(fields[5]), fields[6]);
            case "access" -> new AccessRequestTicket(title, description, requester, priority,
                    fields[5], fields[6]);
            default -> throw new IllegalArgumentException(
                    "unknown ticket type '" + fields[0] + "' (use bug, feature or access)");
        };

        if (fields.length == 8 && !fields[7].isEmpty()) {
            ticket.setCreatedAt(parseDateTime(fields[7]));
        }
        return ticket;
    }

    // A generic method: works for any enum type (Priority, Severity, ...).
    private <E extends Enum<E>> E parseEnum(Class<E> enumType, String text, String fieldName) {
        try {
            return Enum.valueOf(enumType, text.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown " + fieldName + " '" + text + "' (use one of "
                    + Arrays.toString(enumType.getEnumConstants()) + ")");
        }
    }

    private int parseVotes(String text) {
        int votes;
        try {
            votes = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("votes must be a whole number, but found '" + text + "'");
        }
        if (votes < 0) {
            throw new IllegalArgumentException("votes cannot be negative, but found " + votes);
        }
        return votes;
    }

    private LocalDateTime parseDateTime(String text) {
        try {
            return LocalDateTime.parse(text);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("created time must look like 2026-10-30T09:00, but found '" + text + "'");
        }
    }
}
