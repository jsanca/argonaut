package dev.jsanca.argonaut.decision.jev.internal.error;

import dev.langchain4j.exception.NonRetriableException;

/**
 * Thrown when the Jev API returns HTTP 401 (authentication failure). Non-retriable.
 */
public class JevAuthException extends NonRetriableException {

    public JevAuthException(final String message) {
        super(message);
    }

    public int httpStatus() {
        return 401;
    }
}
