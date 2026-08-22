package dev.jsanca.argonaut.core.knowledge;

/**
 * A request to search the controlled knowledge corpus.
 */
public record KnowledgeSearchRequest(String query, int topK) {

    public KnowledgeSearchRequest {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query must not be blank");
        }
        if (topK <= 0) {
            throw new IllegalArgumentException("topK must be a positive integer");
        }
    }
}
