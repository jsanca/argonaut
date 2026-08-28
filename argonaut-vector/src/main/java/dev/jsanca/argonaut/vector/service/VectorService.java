package dev.jsanca.argonaut.vector.service;

import dev.jsanca.argonaut.vector.config.ActiveVectorProvider;
import dev.jsanca.argonaut.vector.config.VectorProvider;
import dev.jsanca.argonaut.vector.config.VectorProviderRegistry;
import dev.jsanca.argonaut.vector.domain.Document;
import dev.jsanca.argonaut.vector.domain.Embedding;
import dev.jsanca.argonaut.vector.domain.SearchOptions;
import dev.jsanca.argonaut.vector.domain.SearchResult;
import dev.jsanca.argonaut.vector.domain.VectorDocument;
import dev.jsanca.argonaut.vector.port.EmbeddingPort;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application-layer service for vector store operations.
 *
 * <p>Resolves the active provider on every call via {@link ActiveVectorProvider}, so provider
 * switches take effect immediately without restarting the service.
 */
@Service
public class VectorService {

    private final EmbeddingPort embeddingPort;
    private final VectorProviderRegistry registry;
    private final ActiveVectorProvider activeProvider;

    public VectorService(EmbeddingPort embeddingPort,
                         VectorProviderRegistry registry,
                         ActiveVectorProvider activeProvider) {
        this.embeddingPort = embeddingPort;
        this.registry = registry;
        this.activeProvider = activeProvider;
    }

    /**
     * Embeds {@code content} and stores the document in the currently active backend.
     */
    public void storeDocument(String id, String content) {
        VectorProvider provider = activeProvider.current();
        Embedding embedding = embeddingPort.embed(content);
        registry.storeFor(provider).store(new VectorDocument(Document.of(id, content), embedding));
    }

    /**
     * Embeds {@code query} and returns the top-{@code limit} most similar documents
     * from the currently active backend.
     */
    public List<SearchResult> search(String query, int limit) {
        VectorProvider provider = activeProvider.current();
        Embedding embedding = embeddingPort.embed(query);
        return registry.searchFor(provider).search(embedding, SearchOptions.top(limit));
    }
}
