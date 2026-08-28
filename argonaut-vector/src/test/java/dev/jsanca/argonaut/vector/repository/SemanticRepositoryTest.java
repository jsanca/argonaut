package dev.jsanca.argonaut.vector.repository;

import dev.jsanca.argonaut.vector.adapter.memory.InMemoryVectorAdapter;
import dev.jsanca.argonaut.vector.domain.Document;
import dev.jsanca.argonaut.vector.domain.Embedding;
import dev.jsanca.argonaut.vector.domain.SearchOptions;
import dev.jsanca.argonaut.vector.domain.SearchResult;
import dev.jsanca.argonaut.vector.port.EmbeddingPort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class SemanticRepositoryTest {

    private static final Random RNG = new Random(42);

    private static float[] randomUnitVector() {
        float[] v = new float[384];
        float norm = 0f;
        for (int i = 0; i < v.length; i++) {
            v[i] = (float) RNG.nextGaussian();
            norm += v[i] * v[i];
        }
        norm = (float) Math.sqrt(norm);
        for (int i = 0; i < v.length; i++) v[i] /= norm;
        return v;
    }

    @Test
    void store_then_search_finds_stored_document() {
        // Fixed embedding so stored doc matches query perfectly
        float[] fixedVec = new float[384];
        fixedVec[0] = 1.0f;

        EmbeddingPort mockEmbedding = text -> new Embedding(fixedVec);
        InMemoryVectorAdapter backend = new InMemoryVectorAdapter();
        SemanticRepository repo = new SemanticRepository(mockEmbedding, backend, backend);

        repo.store(Document.of("test-1", "some content"));
        List<SearchResult> results = repo.search("some query", SearchOptions.top(5));

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).document().id()).isEqualTo("test-1");
    }

    @Test
    void multiple_stored_documents_are_all_retrievable() {
        // Use deterministic mock that returns different embeddings for each distinct text
        InMemoryVectorAdapter backend = new InMemoryVectorAdapter();
        float[] q = randomUnitVector();
        EmbeddingPort mockEmbedding = text -> new Embedding(q);
        SemanticRepository repo = new SemanticRepository(mockEmbedding, backend, backend);

        repo.store(Document.of("doc-a", "content a"));
        repo.store(Document.of("doc-b", "content b"));
        repo.store(Document.of("doc-c", "content c"));

        List<SearchResult> results = repo.search("query", SearchOptions.top(10));
        assertThat(results).hasSize(3);
    }

    @Test
    void search_limit_is_respected() {
        float[] vec = new float[384];
        vec[0] = 1.0f;
        EmbeddingPort mockEmbedding = text -> new Embedding(vec);
        InMemoryVectorAdapter backend = new InMemoryVectorAdapter();
        SemanticRepository repo = new SemanticRepository(mockEmbedding, backend, backend);

        for (int i = 0; i < 20; i++) {
            repo.store(Document.of("doc-" + i, "content " + i));
        }

        List<SearchResult> results = repo.search("query", SearchOptions.top(5));
        assertThat(results).hasSize(5);
    }
}
