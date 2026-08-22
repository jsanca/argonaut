package dev.jsanca.argonaut.core.error;

import java.util.Objects;

/**
 * Structured error produced during an Argonaut experiment run.
 *
 * <p>Error messages must never contain secrets or credentials.</p>
 */
public record ArgonautError(
        ArgonautErrorCode code,
        String message,
        String component,
        boolean retryable
) {
    public ArgonautError {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }
}
