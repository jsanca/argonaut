package dev.jsanca.argonaut.vector.domain;

/**
 * A single ranked result from a vector similarity search.
 */
public record SearchResult(Document document, float score) {
}
