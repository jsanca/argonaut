package dev.jsanca.argonaut.decision.systemone.internal.error;

/**
 * Thrown when the System One API returns HTTP 503 (service overloaded / temporarily unavailable). Retriable.
 */
public class SystemOneOverloadException extends SystemOneException {

    public SystemOneOverloadException(final String message) {
        super(message, 503);
    }
}
