package com.queuedesk.controller;

import com.queuedesk.dto.AnalyticsResponse;
import com.queuedesk.service.ReportGenerator;
import com.queuedesk.service.TicketService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Reports and analytics: the HTTP version of console options 7 (EOD report) and 11 (analytics).
 */
@RestController
@RequestMapping("/api")
public class ReportController {

    private static final DateTimeFormatter FILE_NAME_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private final TicketService ticketService;
    private final ReportGenerator reportGenerator;
    private final Clock clock;

    public ReportController(TicketService ticketService, ReportGenerator reportGenerator, Clock clock) {
        this.ticketService = ticketService;
        this.reportGenerator = reportGenerator;
        this.clock = clock;
    }

    // GET /api/reports/eod  ->  the end-of-day report as plain text.
    @GetMapping(value = "/reports/eod", produces = MediaType.TEXT_PLAIN_VALUE)
    public String endOfDayReport() {
        return reportGenerator.generate(ticketService.findAll(), LocalDateTime.now(clock));
    }

    // POST /api/reports/eod/files  ->  201, and the report saved in a new file on the server.
    @PostMapping("/reports/eod/files")
    public ResponseEntity<Map<String, String>> saveEndOfDayReport() throws IOException {
        LocalDateTime now = LocalDateTime.now(clock);
        String report = reportGenerator.generate(ticketService.findAll(), now);
        Path file = Path.of("data", "reports", "eod-" + now.format(FILE_NAME_TIME) + ".txt");
        reportGenerator.writeToFile(report, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("file", file.toString()));
    }

    // GET /api/analytics  ->  Milestone 6's Streams results plus two database queries.
    @GetMapping("/analytics")
    public AnalyticsResponse analytics() {
        return ticketService.analytics(LocalDateTime.now(clock));
    }
}
