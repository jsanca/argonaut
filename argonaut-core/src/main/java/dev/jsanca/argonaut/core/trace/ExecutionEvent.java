package dev.jsanca.argonaut.core.trace;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * A single observable event in an Argonaut execution timeline.
 *
 * <p>{@code metadata} carries framework-specific detail without polluting the common contract.</p>
 * <p>{@code durationMs} is null for events that mark the start of an activity.</p>
 */
public record ExecutionEvent(
        String id,
        Instant timestamp,
        ExecutionEventType type,
        String actor,
        String name,
        String summary,
        Map<String, String> metadata,
        Long durationMs
) {
    public ExecutionEvent {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        Objects.requireNonNull(type, "type must not be null");
        metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }
}
