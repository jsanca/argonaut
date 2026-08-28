package dev.jsanca.argonaut.vector.port;

import dev.jsanca.argonaut.vector.domain.Embedding;
import dev.jsanca.argonaut.vector.domain.SearchOptions;
import dev.jsanca.argonaut.vector.domain.SearchResult;

import java.util.List;

/**
 * Port for querying a vector store by embedding similarity.
 */
public interface SearchPort {

    List<SearchResult> search(Embedding query, SearchOptions options);
}
