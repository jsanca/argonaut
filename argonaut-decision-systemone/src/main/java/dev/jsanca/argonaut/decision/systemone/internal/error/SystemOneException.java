package dev.jsanca.argonaut.decision.systemone.internal.error;

import dev.langchain4j.exception.RetriableException;

/**
 * Base exception for System One HTTP errors that are retriable by default (5xx responses, unknown HTTP errors).
 *
 * <p>{@code httpStatus} carries the HTTP response status code. Non-HTTP transport failures
 * are represented by {@link SystemOneTransportException}; domain validation failures by
 * {@link SystemOneValidationException}.</p>
 */
public class SystemOneException extends RetriableException {

    private final int httpStatus;

    public SystemOneException(final String message, final int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
