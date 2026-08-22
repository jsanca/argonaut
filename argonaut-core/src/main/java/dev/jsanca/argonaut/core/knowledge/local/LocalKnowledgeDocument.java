package dev.jsanca.argonaut.core.knowledge.local;

import java.util.Map;
import java.util.Objects;

/**
 * A single document in the controlled local knowledge corpus.
 *
 * <p>Documents are immutable. {@code metadata} carries optional labels (e.g. topic, version)
 * without expanding the core contract.</p>
 */
public record LocalKnowledgeDocument(
        String id,
        String title,
        String content,
        Map<String, String> metadata
) {
    public LocalKnowledgeDocument {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id must not be blank");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(content, "content must not be null");
        metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    public static LocalKnowledgeDocument of(String id, String title, String content) {
        return new LocalKnowledgeDocument(id, title, content, Map.of());
    }
}
