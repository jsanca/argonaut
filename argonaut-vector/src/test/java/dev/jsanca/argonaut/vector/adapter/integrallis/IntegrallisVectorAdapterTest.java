package dev.jsanca.argonaut.vector.adapter.integrallis;

import dev.jsanca.argonaut.vector.config.VectorProperties;
import dev.jsanca.argonaut.vector.domain.Document;
import dev.jsanca.argonaut.vector.domain.Embedding;
import dev.jsanca.argonaut.vector.domain.SearchOptions;
import dev.jsanca.argonaut.vector.domain.SearchResult;
import dev.jsanca.argonaut.vector.domain.VectorDocument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrallisVectorAdapterTest {

    private static VectorProperties inMemoryProps() {
        return new VectorProperties(
                new VectorProperties.EmbeddingConfig("onnx"),
                new VectorProperties.VectorConfig(
                        dev.jsanca.argonaut.vector.config.VectorProvider.INTEGRALLIS,
                        new VectorProperties.QdrantConfig("localhost", 6334),
                        new VectorProperties.IntegrallisConfig("") // empty = in-memory mode
                )
        );
    }

    @Test
    void store_and_search_with_integrallis_returns_matching_document() {
        IntegrallisVectorAdapter adapter = new IntegrallisVectorAdapter(inMemoryProps());
        try {
            float[] vec = new float[384];
            vec[0] = 1.0f;

            adapter.store(new VectorDocument(Document.of("doc-1", "hello world"), new Embedding(vec)));

            List<SearchResult> results = adapter.search(new Embedding(vec), SearchOptions.top(5));
            assertThat(results).isNotEmpty();
            assertThat(results.get(0).document().id()).isEqualTo("doc-1");
        } finally {
            adapter.close();
        }
    }

    @Test
    void search_returns_content_text() {
        IntegrallisVectorAdapter adapter = new IntegrallisVectorAdapter(inMemoryProps());
        try {
            float[] vec = new float[384];
            vec[0] = 1.0f;

            adapter.store(new VectorDocument(Document.of("doc-2", "vector content here"), new Embedding(vec)));

            List<SearchResult> results = adapter.search(new Embedding(vec), SearchOptions.top(1));
            assertThat(results).hasSize(1);
            assertThat(results.get(0).document().content()).isEqualTo("vector content here");
        } finally {
            adapter.close();
        }
    }
}
