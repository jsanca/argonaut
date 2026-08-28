package dev.jsanca.argonaut.vector.adapter.memory;

import dev.jsanca.argonaut.vector.domain.Embedding;
import dev.jsanca.argonaut.vector.domain.SearchOptions;
import dev.jsanca.argonaut.vector.domain.SearchResult;
import dev.jsanca.argonaut.vector.domain.VectorDocument;
import dev.jsanca.argonaut.vector.port.SearchPort;
import dev.jsanca.argonaut.vector.port.StorePort;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * In-process, in-memory vector store. Thread-safe via {@link CopyOnWriteArrayList}.
 *
 * <p>Since embeddings are L2-normalized, cosine similarity reduces to a dot product.
 * This adapter is suitable for small corpora and testing. No persistence.
 */
@Component
public class InMemoryVectorAdapter implements StorePort, SearchPort {

    private final List<VectorDocument> store = new CopyOnWriteArrayList<>();

    @Override
    public void store(VectorDocument document) {
        store.add(document);
    }

    @Override
    public List<SearchResult> search(Embedding query, SearchOptions options) {
        return store.stream()
                .map(vd -> new SearchResult(vd.document(), cosineSimilarity(query.values(), vd.embedding().values())))
                .sorted(Comparator.comparingDouble(SearchResult::score).reversed())
                .limit(options.limit())
                .collect(Collectors.toList());
    }

    /**
     * For L2-normalized embeddings the dot product equals cosine similarity.
     */
    private float cosineSimilarity(float[] a, float[] b) {
        float dot = 0f;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
        }
        return dot;
    }
}
