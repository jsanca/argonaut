package dev.jsanca.argonaut.decision.jev.internal.error;

/**
 * Thrown when the Jev API returns HTTP 503 (service overloaded / temporarily unavailable). Retriable.
 */
public class JevOverloadException extends JevException {

    public JevOverloadException(final String message) {
        super(message, 503);
    }
}
