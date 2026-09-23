package dev.jsanca.argonaut.decision.systemone.internal.error;

/**
 * Thrown when the System One API returns HTTP 429 (rate limit exceeded). Retriable.
 */
public class SystemOneRateLimitException extends SystemOneException {

    public SystemOneRateLimitException(final String message) {
        super(message, 429);
    }
}
