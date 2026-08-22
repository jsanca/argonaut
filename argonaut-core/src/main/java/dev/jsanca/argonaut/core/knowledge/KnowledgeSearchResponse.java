package dev.jsanca.argonaut.core.knowledge;

import java.util.List;
import java.util.Objects;

/**
 * The response returned by a knowledge search, containing the original query and ranked results.
 */
public record KnowledgeSearchResponse(String query, List<KnowledgeSearchResult> results) {

    public KnowledgeSearchResponse {
        Objects.requireNonNull(query, "query must not be null");
        results = results != null ? List.copyOf(results) : List.of();
    }
}
