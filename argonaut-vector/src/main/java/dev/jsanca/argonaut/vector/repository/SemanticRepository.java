package dev.jsanca.argonaut.vector.repository;

import dev.jsanca.argonaut.vector.domain.Document;
import dev.jsanca.argonaut.vector.domain.Embedding;
import dev.jsanca.argonaut.vector.domain.SearchOptions;
import dev.jsanca.argonaut.vector.domain.SearchResult;
import dev.jsanca.argonaut.vector.domain.VectorDocument;
import dev.jsanca.argonaut.vector.port.EmbeddingPort;
import dev.jsanca.argonaut.vector.port.SearchPort;
import dev.jsanca.argonaut.vector.port.StorePort;

import java.util.List;

/**
 * High-level repository that combines embedding, storage, and search into a single coherent API.
 *
 * <p>Callers interact with plain {@link Document} and query strings; embedding is handled
 * transparently by the injected {@link EmbeddingPort}.
 */
public class SemanticRepository {

    private final EmbeddingPort embeddingPort;
    private final StorePort storePort;
    private final SearchPort searchPort;

    public SemanticRepository(EmbeddingPort embeddingPort, StorePort storePort, SearchPort searchPort) {
        this.embeddingPort = embeddingPort;
        this.storePort = storePort;
        this.searchPort = searchPort;
    }

    /**
     * Embeds the document's content and stores it in the vector store.
     */
    public void store(Document document) {
        Embedding embedding = embeddingPort.embed(document.content());
        storePort.store(new VectorDocument(document, embedding));
    }

    /**
     * Embeds the query string and returns the most similar stored documents.
     */
    public List<SearchResult> search(String query, SearchOptions options) {
        Embedding embedding = embeddingPort.embed(query);
        return searchPort.search(embedding, options);
    }
}
