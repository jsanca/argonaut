package dev.jsanca.argonaut.decision.systemone.internal.error;

import dev.langchain4j.exception.RetriableException;

/**
 * Thrown when a network-level error occurs communicating with the System One API:
 * connect/read timeout, {@link java.io.IOException}, or {@link InterruptedException}. Retriable.
 */
public class SystemOneTransportException extends RetriableException {

    public SystemOneTransportException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
