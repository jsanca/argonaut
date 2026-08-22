package dev.jsanca.argonaut.core.knowledge;

/**
 * A single result returned from a knowledge search.
 */
public record KnowledgeSearchResult(
        String sourceId,
        String title,
        String excerpt,
        double score
) {}
