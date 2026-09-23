package dev.jsanca.argonaut.decision.jev.internal.error;

/**
 * Thrown when the Jev API returns HTTP 429 (rate limit exceeded). Retriable.
 */
public class JevRateLimitException extends JevException {

    public JevRateLimitException(final String message) {
        super(message, 429);
    }
}
