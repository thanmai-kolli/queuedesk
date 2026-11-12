package com.queuedesk.service;

import com.queuedesk.dto.CreateTicketRequest;
import com.queuedesk.dto.UpdateTicketRequest;
import com.queuedesk.exception.StaleTicketException;
import com.queuedesk.model.Priority;
import com.queuedesk.model.Severity;
import com.queuedesk.model.Ticket;
import com.queuedesk.model.TicketType;
import com.queuedesk.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What happens when many requests hit the same ticket at the same moment?
 * Each test starts several threads, holds them at a "starting line" (CountDownLatch),
 * releases them together, and checks that the data is still correct afterwards.
 */
@SpringBootTest
@ActiveProfiles("test")
class ConcurrencyIntegrationTest {

    private static final int THREADS = 10;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TriageService triageService;

    @Autowired
    private TicketRepository repository;

    @BeforeEach
    void startWithNoTickets() {
        repository.deleteAll();
    }

    @Test
    void tenServeNextCallsAtOnceServeTheOnlyTicketExactlyOnce() throws Exception {
        createBug("Login fails");

        List<Optional<Ticket>> results = runAtTheSameTime(THREADS,
                () -> triageService.serveNext(QueueOrder.PRIORITY, null));

        long served = results.stream().filter(Optional::isPresent).count();
        assertThat(served).isEqualTo(1);
    }

    @Test
    void twoUpdatesOfTheSameVersionAtOnceOnlyOneWins() throws Exception {
        Ticket ticket = createBug("Search is slow");
        long id = ticket.getId();

        List<Future<Ticket>> futures = startAtTheSameTime(2,
                () -> ticketService.update(id, new UpdateTicketRequest(0L, null, Priority.CRITICAL, null, null, null)));

        int succeeded = 0;
        int refused = 0;
        for (Future<Ticket> future : futures) {
            try {
                future.get();
                succeeded++;
            } catch (ExecutionException e) {
                // Either our own version check or Hibernate's @Version check stops the second one.
                assertThat(e.getCause()).isInstanceOfAny(StaleTicketException.class,
                        ObjectOptimisticLockingFailureException.class);
                refused++;
            }
        }
        assertThat(succeeded).isEqualTo(1);
        assertThat(refused).isEqualTo(1);
        assertThat(repository.findById(id).orElseThrow().getVersion()).isEqualTo(1L);
    }

    // ----- helpers -----

    private Ticket createBug(String title) {
        return ticketService.create(new CreateTicketRequest(TicketType.BUG, title, "", "Asha", Priority.HIGH,
                Severity.MAJOR, "Try it", null, null, null, null, null, null));
    }

    private <T> List<T> runAtTheSameTime(int threads, Callable<T> task) throws Exception {
        List<T> results = new ArrayList<>();
        for (Future<T> future : startAtTheSameTime(threads, task)) {
            results.add(future.get());
        }
        return results;
    }

    private <T> List<Future<T>> startAtTheSameTime(int threads, Callable<T> task) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startingLine = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                startingLine.await();   // every thread waits here...
                return task.call();
            }));
        }
        startingLine.countDown();       // ...until all are released together
        pool.shutdown();
        pool.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS);
        return futures;
    }
}
