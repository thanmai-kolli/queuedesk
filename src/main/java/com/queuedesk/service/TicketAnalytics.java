package com.queuedesk.service;

import com.queuedesk.model.Priority;
import com.queuedesk.model.Status;
import com.queuedesk.model.Ticket;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Milestone 6: questions about many tickets, answered with Streams.
 *
 * A stream reads the list step by step (filter, map, ...) and builds a new result.
 * It never changes the list it came from.
 */
public final class TicketAnalytics {

    private TicketAnalytics() {
        // only static methods here, so nobody needs to create an object
    }

    // How many tickets each priority has, e.g. {HIGH=1, CRITICAL=2}.
    // A priority with no tickets does not appear in the map at all.
    public static Map<Priority, Long> countByPriority(List<Ticket> tickets) {
        return tickets.stream()
                .collect(Collectors.groupingBy(Ticket::getPriority, Collectors.counting()));
    }

    // The one-line stream version of Repository.groupByAssignee() (which uses a loop).
    public static Map<String, List<Ticket>> groupByAssignee(List<Ticket> tickets) {
        return tickets.stream()
                .collect(Collectors.groupingBy(t -> t.getAssignee() == null ? "Unassigned" : t.getAssignee()));
    }

    // average() returns an OptionalDouble, because an empty list has no average.
    // The empty case is decided here on purpose: no tickets means 0.0 hours.
    public static double averageEffortHours(List<Ticket> tickets) {
        if (tickets.isEmpty()) {
            return 0.0;
        }
        return tickets.stream()
                .mapToDouble(Ticket::estimateEffortHours)
                .average()
                .getAsDouble();
    }

    // The total effort, written as a normal loop ...
    public static double totalEffortHoursLoop(List<Ticket> tickets) {
        double total = 0.0;
        for (Ticket ticket : tickets) {
            total += ticket.estimateEffortHours();
        }
        return total;
    }

    // ... and the same total with a stream, so the two can be compared side by side.
    // reduce starts at 0.0 and adds each value. The third argument (the "combiner") is only
    // used by parallel streams to join partial sums.
    public static double totalEffortHours(List<Ticket> tickets) {
        return tickets.stream()
                .map(Ticket::estimateEffortHours)
                .reduce(0.0, Double::sum, Double::sum);
    }

    // min(...) already returns an Optional: it is empty when there are no OPEN tickets.
    public static Optional<Ticket> oldestOpenTicket(List<Ticket> tickets) {
        return tickets.stream()
                .filter(t -> t.getStatus() == Status.OPEN)
                .min(Comparator.comparing(Ticket::getCreatedAt));
    }

    // Every tag used by any ticket, once each, A-Z, in one line: "billing, login, urgent".
    // flatMap turns "a stream of tag sets" into "one stream of tags".
    public static String tagLine(List<Ticket> tickets) {
        return tickets.stream()
                .flatMap(t -> t.getTags().stream())
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));
    }
}
