package dev.jsanca.argonaut.decision.systemone.internal.error;

import dev.langchain4j.exception.NonRetriableException;

/**
 * Thrown when the System One API returns HTTP 401 (authentication failure). Non-retriable.
 */
public class SystemOneAuthException extends NonRetriableException {

    public SystemOneAuthException(final String message) {
        super(message);
    }

    public int httpStatus() {
        return 401;
    }
}
