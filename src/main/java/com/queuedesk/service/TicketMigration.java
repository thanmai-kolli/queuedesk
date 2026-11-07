package com.queuedesk.service;

import com.queuedesk.exception.InvalidTicketDataException;
import com.queuedesk.model.Ticket;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Milestone 7: the one-time move from the old text format to JSON.
 *
 * Reads tickets.txt with the Milestone 5 loader and writes them out once as tickets.json.
 * From then on QueueDesk only reads and writes tickets.json.
 */
public class TicketMigration {

    private final TicketFileLoader textLoader;
    private final TicketJsonStore jsonStore;

    public TicketMigration(TicketFileLoader textLoader, TicketJsonStore jsonStore) {
        this.textLoader = textLoader;
        this.jsonStore = jsonStore;
    }

    public List<Ticket> migrate(Path textFile, Path jsonFile) throws IOException, InvalidTicketDataException {
        if (Files.exists(jsonFile)) {
            throw new IOException(jsonFile + " already exists. Migration only runs once and never overwrites it.");
        }
        List<Ticket> tickets = textLoader.load(textFile);
        jsonStore.save(tickets, jsonFile);
        return tickets;
    }
}
