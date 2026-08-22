package dev.jsanca.argonaut.core.evidence;

import java.util.Objects;

/**
 * A single piece of evidence retrieved or selected during an Argonaut experiment run.
 *
 * <p>Evidence items are displayable as UI cards in the Vue experiment console.</p>
 */
public record Evidence(
        String id,
        EvidenceKind kind,
        String sourceId,
        String title,
        String excerpt,
        double score,
        String usedFor
) {
    public Evidence {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(sourceId, "sourceId must not be null");
        if (!Double.isFinite(score)) {
            throw new IllegalArgumentException("score must be a finite value");
        }
    }
}
