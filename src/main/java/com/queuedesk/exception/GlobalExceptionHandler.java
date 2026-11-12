package com.queuedesk.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.nio.file.FileAlreadyExistsException;
import java.util.List;

/**
 * One place that turns exceptions into HTTP error responses (@RestControllerAdvice).
 *
 * Every error has the same JSON shape, "Problem Details" (RFC 9457), built into Spring:
 *   { "status": 404, "title": "Ticket not found", "detail": "No ticket found with ID 99", ... }
 * Validation errors also have an "errors" list.
 *
 * Without this class, a TicketNotFoundException would reach the client as
 * 500 Internal Server Error.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ----- 404 Not Found -----

    @ExceptionHandler(TicketNotFoundException.class)
    public ProblemDetail handleNotFound(TicketNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Ticket not found", e.getMessage());
    }

    // ----- 400 Bad Request: the request itself is wrong -----

    // @Valid on a request body failed, e.g. @NotBlank title was empty.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleInvalidBody(MethodArgumentNotValidException e) {
        List<String> errors = e.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::describe)
                .toList();
        return withErrors(problem(HttpStatus.BAD_REQUEST, "Invalid request", "Some fields are not valid"), errors);
    }

    // A constraint on a request parameter failed, e.g. @Min(1) on ?start=0.
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleInvalidParameters(HandlerMethodValidationException e) {
        List<String> errors = e.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> result.getMethodParameter().getParameterName() + ": "
                                + error.getDefaultMessage()))
                .toList();
        return withErrors(problem(HttpStatus.BAD_REQUEST, "Invalid request", "Some parameters are not valid"), errors);
    }

    // TicketValidator found broken business rules.
    @ExceptionHandler(TicketValidationException.class)
    public ProblemDetail handleBusinessRules(TicketValidationException e) {
        return withErrors(problem(HttpStatus.BAD_REQUEST, "Invalid request", "The ticket breaks some rules"),
                e.getErrors());
    }

    // A line in an uploaded text file is wrong (Milestone 5's checked exception).
    @ExceptionHandler(InvalidTicketDataException.class)
    public ProblemDetail handleBadFile(InvalidTicketDataException e) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Invalid ticket file",
                e.getMessage() + ". Nothing was imported.");
        problem.setProperty("lineNumber", e.getLineNumber());
        return problem;
    }

    // Broken JSON, or a value that doesn't fit, e.g. "priority": "URGENT".
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException e) {
        return problem(HttpStatus.BAD_REQUEST, "Unreadable request body",
                "The JSON is not valid, or a value has the wrong type. Check enum values such as "
                        + "priority (LOW, MEDIUM, HIGH, CRITICAL).");
    }

    // A parameter has the wrong type, e.g. /api/tickets/abc or ?status=DONE.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request",
                "'" + e.getValue() + "' is not a valid value for '" + e.getName() + "'");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParameter(MissingServletRequestParameterException e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request",
                "The request parameter '" + e.getParameterName() + "' is required");
    }

    // ?sort=nonsense on a paged request.
    @ExceptionHandler(PropertyReferenceException.class)
    public ProblemDetail handleBadSort(PropertyReferenceException e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid sort", e.getMessage());
    }

    // A rule inside an entity constructor or setter (the last line of defence in Java code).
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", e.getMessage());
    }

    // ----- 409 Conflict: the request is fine, but it clashes with the current data -----

    // Two report saves in the same millisecond would pick the same file name.
    @ExceptionHandler(FileAlreadyExistsException.class)
    public ProblemDetail handleFileExists(FileAlreadyExistsException e) {
        return problem(HttpStatus.CONFLICT, "File already exists",
                "A report file with that name was just written. Try again.");
    }

    @ExceptionHandler(DuplicateTicketException.class)
    public ProblemDetail handleDuplicate(DuplicateTicketException e) {
        return problem(HttpStatus.CONFLICT, "Duplicate ticket", e.getMessage());
    }

    @ExceptionHandler(StaleTicketException.class)
    public ProblemDetail handleStale(StaleTicketException e) {
        return problem(HttpStatus.CONFLICT, "Ticket was changed by someone else", e.getMessage());
    }

    // @Version check failed: another request saved the same ticket a moment earlier.
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException e) {
        return problem(HttpStatus.CONFLICT, "Ticket was changed by someone else",
                "Another request changed this ticket at the same moment. Read it again and retry.");
    }

    // A database constraint refused the data, e.g. the unique (type, title, requester) key.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleConstraint(DataIntegrityViolationException e) {
        log.warn("Database constraint violated: {}", e.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "Conflicts with existing data",
                "The database refused the change, for example because the same ticket already exists.");
    }

    // ----- helpers -----

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }

    private static ProblemDetail withErrors(ProblemDetail problem, List<String> errors) {
        problem.setProperty("errors", errors);
        return problem;
    }

    private static String describe(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }
}
