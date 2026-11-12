package com.queuedesk.config;

import com.queuedesk.repository.TicketRepository;
import com.queuedesk.service.TicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Runs once when the app has started (ApplicationRunner).
 * If the tickets table is empty, it imports the sample tickets from data/tickets.txt,
 * like the one-time migration in Month 1. If there are tickets already, it does nothing.
 *
 * @Value reads a setting from application.yml (queuedesk.seed-file).
 */
@Component
public class SampleDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SampleDataLoader.class);

    private final TicketRepository repository;
    private final TicketService ticketService;
    private final String seedFile;

    public SampleDataLoader(TicketRepository repository, TicketService ticketService,
                            @Value("${queuedesk.seed-file:}") String seedFile) {
        this.repository = repository;
        this.ticketService = ticketService;
        this.seedFile = seedFile;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (seedFile == null || seedFile.isBlank()) {
            return;
        }
        Path file = Path.of(seedFile);
        if (repository.count() > 0) {
            log.info("Database already has tickets; not importing {}", file);
            return;
        }
        if (!Files.exists(file)) {
            log.warn("Seed file {} not found (looked in {}); starting with no tickets", file, file.toAbsolutePath());
            return;
        }
        TicketService.ImportOutcome outcome = ticketService.importFile(file);
        log.info("Imported {} sample ticket(s) from {}", outcome.imported().size(), file);
    }
}
