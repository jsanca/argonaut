package dev.jsanca.argonaut.decision.jev.internal.error;

import dev.langchain4j.exception.RetriableException;

/**
 * Base exception for Jev HTTP errors that are retriable by default (5xx responses, unknown HTTP errors).
 *
 * <p>{@code httpStatus} carries the HTTP response status code. Non-HTTP transport failures
 * are represented by {@link JevTransportException}; domain validation failures by
 * {@link JevValidationException}.</p>
 */
public class JevException extends RetriableException {

    private final int httpStatus;

    public JevException(final String message, final int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
