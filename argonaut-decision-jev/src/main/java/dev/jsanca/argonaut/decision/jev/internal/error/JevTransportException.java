package dev.jsanca.argonaut.decision.jev.internal.error;

import dev.langchain4j.exception.RetriableException;

/**
 * Thrown when a network-level error occurs communicating with the Jev API:
 * connect/read timeout, {@link java.io.IOException}, or {@link InterruptedException}. Retriable.
 */
public class JevTransportException extends RetriableException {

    public JevTransportException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
