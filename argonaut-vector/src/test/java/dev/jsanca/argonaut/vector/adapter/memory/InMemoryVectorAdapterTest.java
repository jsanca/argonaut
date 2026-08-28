package dev.jsanca.argonaut.vector.adapter.memory;

import dev.jsanca.argonaut.vector.domain.Document;
import dev.jsanca.argonaut.vector.domain.Embedding;
import dev.jsanca.argonaut.vector.domain.SearchOptions;
import dev.jsanca.argonaut.vector.domain.SearchResult;
import dev.jsanca.argonaut.vector.domain.VectorDocument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class InMemoryVectorAdapterTest {

    @Test
    void store_and_search_returns_matching_document() {
        InMemoryVectorAdapter adapter = new InMemoryVectorAdapter();
        float[] vec = new float[384];
        vec[0] = 1.0f; // simple unit vector

        adapter.store(new VectorDocument(Document.of("doc-1", "hello"), new Embedding(vec)));

        List<SearchResult> results = adapter.search(new Embedding(vec), SearchOptions.top(5));
        assertThat(results).hasSize(1);
        assertThat(results.get(0).document().id()).isEqualTo("doc-1");
        assertThat((double) results.get(0).score()).isCloseTo(1.0, within(0.001));
    }

    @Test
    void search_returns_results_ordered_by_score_descending() {
        InMemoryVectorAdapter adapter = new InMemoryVectorAdapter();

        float[] queryVec = new float[384];
        queryVec[0] = 1.0f;

        float[] highVec = new float[384];
        highVec[0] = 0.9f;
        highVec[1] = (float) Math.sqrt(1.0 - 0.9 * 0.9); // normalize

        float[] lowVec = new float[384];
        lowVec[1] = 1.0f; // orthogonal to query

        adapter.store(new VectorDocument(Document.of("low", "low match"), new Embedding(lowVec)));
        adapter.store(new VectorDocument(Document.of("high", "high match"), new Embedding(highVec)));

        List<SearchResult> results = adapter.search(new Embedding(queryVec), SearchOptions.top(2));
        assertThat(results).hasSize(2);
        assertThat(results.get(0).document().id()).isEqualTo("high");
        assertThat(results.get(0).score()).isGreaterThan(results.get(1).score());
    }

    @Test
    void search_respects_limit() {
        InMemoryVectorAdapter adapter = new InMemoryVectorAdapter();
        float[] vec = new float[384];
        vec[0] = 1.0f;

        for (int i = 0; i < 10; i++) {
            adapter.store(new VectorDocument(Document.of("doc-" + i, "content " + i), new Embedding(vec)));
        }

        List<SearchResult> results = adapter.search(new Embedding(vec), SearchOptions.top(3));
        assertThat(results).hasSize(3);
    }

    @Test
    void empty_store_returns_no_results() {
        InMemoryVectorAdapter adapter = new InMemoryVectorAdapter();
        float[] vec = new float[384];
        vec[0] = 1.0f;

        List<SearchResult> results = adapter.search(new Embedding(vec), SearchOptions.top(5));
        assertThat(results).isEmpty();
    }
}
