package dev.jsanca.argonaut.decision.systemone.internal.error;

import dev.langchain4j.exception.NonRetriableException;

/**
 * Thrown for HTTP 422 responses, or when local domain mapping of a provider response fails
 * (e.g. unknown candidate key, out-of-range probability). Non-retriable in both cases.
 *
 * <p>For domain mapping failures (not HTTP-originated), {@code httpStatus()} is 0.</p>
 */
public class SystemOneValidationException extends NonRetriableException {

    private final int httpStatus;

    public SystemOneValidationException(final String message, final int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
