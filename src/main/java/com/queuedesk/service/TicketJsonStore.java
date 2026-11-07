package com.queuedesk.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.queuedesk.exception.InvalidTicketDataException;
import com.queuedesk.model.Ticket;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Milestone 7: saves all tickets to tickets.json and loads them back.
 *
 * Jackson is only used inside this class. Its exceptions are turned into our own
 * InvalidTicketDataException, so the rest of the program never needs to know Jackson exists.
 */
public class TicketJsonStore {

    // "A list of Ticket". Java forgets generic types when it runs (type erasure),
    // so this object keeps the full type for Jackson.
    private static final TypeReference<List<Ticket>> TICKET_LIST = new TypeReference<>() {};

    // The mapper is built once, in one place.
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())                    // understands LocalDateTime
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // "2026-11-08T09:30:00", not numbers
            .enable(SerializationFeature.INDENT_OUTPUT);             // readable, indented JSON

    public void save(List<Ticket> tickets, Path file) throws InvalidTicketDataException {
        // Write to a temporary file first, then swap it in. If the program stops in the
        // middle of writing, the old tickets.json is still complete.
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            // writerFor(TICKET_LIST) makes Jackson write the "type" of every ticket in the list.
            mapper.writerFor(TICKET_LIST).writeValue(temp.toFile(), tickets);
            moveIntoPlace(temp, file);
        } catch (IOException e) {
            throw new InvalidTicketDataException("Could not save tickets to " + file + ": " + e.getMessage(), e);
        }
    }

    public List<Ticket> load(Path file) throws InvalidTicketDataException {
        List<Ticket> tickets;
        try {
            // The TypeReference makes Jackson create BugTicket, FeatureRequestTicket and
            // AccessRequestTicket objects (using "type"), not plain maps.
            tickets = mapper.readValue(file.toFile(), TICKET_LIST);
        } catch (IOException e) {
            throw new InvalidTicketDataException("Could not read " + file + ": " + e.getMessage(), e);
        }

        if (tickets == null) {
            throw new InvalidTicketDataException(file + " must contain a list of tickets, like []", null);
        }

        Set<Integer> seenIds = new HashSet<>();
        int highestId = 0;
        for (int i = 0; i < tickets.size(); i++) {
            Ticket ticket = tickets.get(i);
            if (ticket == null) {
                throw new InvalidTicketDataException("Ticket number " + (i + 1) + " in " + file + " is empty", null);
            }
            try {
                ticket.validate();
            } catch (IllegalArgumentException e) {
                throw new InvalidTicketDataException(
                        "Ticket number " + (i + 1) + " in " + file + ": " + e.getMessage(), e);
            }
            if (!seenIds.add(ticket.getId())) {
                throw new InvalidTicketDataException("Two tickets in " + file + " have id " + ticket.getId(), null);
            }
            highestId = Math.max(highestId, ticket.getId());
        }

        Ticket.continueIdsAfter(highestId);
        return tickets;
    }

    private void moveIntoPlace(Path temp, Path file) throws IOException {
        try {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
