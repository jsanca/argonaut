package dev.jsanca.argonaut.core.experiment;

import java.util.List;
import java.util.Objects;

/**
 * Describes an Argonaut service implementation, returned by {@code GET /api/about}.
 *
 * <p>{@code modelProvider} and {@code modelName} may be {@code null} if the implementation
 * does not expose model identity at startup time.</p>
 */
public record ArgonautInfo(
        String frameworkId,
        String frameworkName,
        String implementationVersion,
        String modelProvider,
        String modelName,
        List<String> capabilities
) {
    public ArgonautInfo {
        Objects.requireNonNull(frameworkId, "frameworkId must not be null");
        Objects.requireNonNull(frameworkName, "frameworkName must not be null");
        capabilities = capabilities != null ? List.copyOf(capabilities) : List.of();
    }
}
