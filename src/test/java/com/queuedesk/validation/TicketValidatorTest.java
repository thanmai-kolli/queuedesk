package com.queuedesk.validation;

import com.queuedesk.dto.CreateTicketRequest;
import com.queuedesk.dto.UpdateTicketRequest;
import com.queuedesk.exception.DuplicateTicketException;
import com.queuedesk.exception.StaleTicketException;
import com.queuedesk.exception.TicketValidationException;
import com.queuedesk.model.BugTicket;
import com.queuedesk.model.FeatureRequestTicket;
import com.queuedesk.model.Priority;
import com.queuedesk.model.Severity;
import com.queuedesk.model.Status;
import com.queuedesk.model.Ticket;
import com.queuedesk.model.TicketType;
import com.queuedesk.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * A unit test: no Spring, no database. Mockito gives the validator a fake TicketRepository
 * whose answers we choose, so each rule can be tested on its own, in milliseconds.
 */
@ExtendWith(MockitoExtension.class)
class TicketValidatorTest {

    @Mock
    private TicketRepository repository;

    @InjectMocks
    private TicketValidator validator;

    @Test
    void aCompleteBugPasses() {
        when(repository.countSameTicket(any(), anyString(), anyString())).thenReturn(0L);

        assertThatCode(() -> validator.validateNew(bug(Severity.MAJOR, "Click login", null)))
                .doesNotThrowAnyException();
    }

    @Test
    void aBugWithoutSeverityAndStepsReportsBothProblems() {
        TicketValidationException error = catchThrowableOfType(TicketValidationException.class,
                () -> validator.validateNew(bug(null, " ", null)));

        assertThat(error.getErrors())
                .anyMatch(message -> message.startsWith("severity"))
                .anyMatch(message -> message.startsWith("stepsToReproduce"));
        verifyNoInteractions(repository);   // invalid requests never reach the database
    }

    @Test
    void aFieldFromAnotherTicketTypeIsRejected() {
        TicketValidationException error = catchThrowableOfType(TicketValidationException.class,
                () -> validator.validateNew(bug(Severity.MINOR, "Click login", 5)));

        assertThat(error.getErrors()).contains("votes: only belongs to FEATURE tickets, not BUG");
    }

    @Test
    void aDuplicateTicketIsRejected() {
        when(repository.countSameTicket(any(), anyString(), anyString())).thenReturn(1L);

        assertThatThrownBy(() -> validator.validateNew(bug(Severity.MAJOR, "Click login", null)))
                .isInstanceOf(DuplicateTicketException.class);
    }

    @Test
    void anUpdateWithAnOldVersionIsRejected() {
        Ticket ticket = savedBug(1L, 3L);

        assertThatThrownBy(() -> validator.validateUpdate(ticket, update(2L, Status.IN_PROGRESS, null)))
                .isInstanceOf(StaleTicketException.class);
    }

    @Test
    void aClosedTicketCannotBeChanged() {
        Ticket ticket = savedBug(1L, 0L);
        ticket.setStatus(Status.CLOSED);

        TicketValidationException error = catchThrowableOfType(TicketValidationException.class,
                () -> validator.validateUpdate(ticket, update(0L, Status.OPEN, null)));

        assertThat(error.getErrors()).anyMatch(message -> message.contains("CLOSED"));
    }

    @Test
    void anEscalatedTicketsPriorityCannotBeLowered() {
        BugTicket ticket = savedBug(1L, 0L);
        ticket.escalate();   // HIGH -> CRITICAL

        TicketValidationException error = catchThrowableOfType(TicketValidationException.class,
                () -> validator.validateUpdate(ticket, update(0L, null, Priority.LOW)));

        assertThat(error.getErrors()).anyMatch(message -> message.startsWith("priority"));
    }

    @Test
    void aFeatureRequestCannotBeEscalated() {
        Ticket feature = new FeatureRequestTicket("Dark mode", "", "Mira", Priority.LOW, 3, "Eyes hurt");
        ReflectionTestUtils.setField(feature, "id", 2L);
        ReflectionTestUtils.setField(feature, "version", 0L);

        assertThatThrownBy(() -> validator.validateEscalation(feature, 0L))
                .isInstanceOf(TicketValidationException.class)
                .hasMessageContaining("cannot be escalated");
    }

    // ----- helpers -----

    private static CreateTicketRequest bug(Severity severity, String steps, Integer votes) {
        return new CreateTicketRequest(TicketType.BUG, "Login fails", "Cannot sign in", "Asha", Priority.HIGH,
                severity, steps, votes, null, null, null, null, null);
    }

    private static UpdateTicketRequest update(Long version, Status status, Priority priority) {
        return new UpdateTicketRequest(version, status, priority, null, null, null);
    }

    // A ticket as if it came from the database: id and version are set by Hibernate normally,
    // so the test sets the private fields directly.
    private static BugTicket savedBug(Long id, Long version) {
        BugTicket ticket = new BugTicket("Login fails", "", "Asha", Priority.HIGH, Severity.MAJOR, "Click login");
        ReflectionTestUtils.setField(ticket, "id", id);
        ReflectionTestUtils.setField(ticket, "version", version);
        return ticket;
    }
}
