package dev.jsanca.argonaut.vector.service;

import dev.jsanca.argonaut.vector.adapter.memory.InMemoryVectorAdapter;
import dev.jsanca.argonaut.vector.config.ActiveVectorProvider;
import dev.jsanca.argonaut.vector.config.VectorProvider;
import dev.jsanca.argonaut.vector.config.VectorProviderRegistry;
import dev.jsanca.argonaut.vector.domain.Embedding;
import dev.jsanca.argonaut.vector.domain.SearchResult;
import dev.jsanca.argonaut.vector.port.EmbeddingPort;
import dev.jsanca.argonaut.vector.port.SearchPort;
import dev.jsanca.argonaut.vector.port.StorePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for provider-switching behaviour in {@link VectorService}.
 *
 * <p>Uses two separate {@link InMemoryVectorAdapter} instances — one for the IN_MEMORY slot and one
 * standing in for the INTEGRALLIS slot — to verify that store/search operations are routed to the
 * correct backend and that documents stored in one provider are not visible in the other.
 */
class VectorServiceProviderSwitchTest {

    /** Fixed 384-dimensional unit vector returned by the mock EmbeddingPort. */
    private static final float[] FIXED_VECTOR;
    static {
        FIXED_VECTOR = new float[384];
        FIXED_VECTOR[0] = 1.0f;
    }

    private final EmbeddingPort mockEmbedding = text -> new Embedding(FIXED_VECTOR);

    private InMemoryVectorAdapter memAdapter;
    private InMemoryVectorAdapter intAdapter;
    private VectorProviderRegistry registry;
    private AtomicReference<VectorProvider> currentProvider;

    @BeforeEach
    void setUp() {
        memAdapter = new InMemoryVectorAdapter();
        intAdapter = new InMemoryVectorAdapter();

        EnumMap<VectorProvider, StorePort> storeMap = new EnumMap<>(VectorProvider.class);
        EnumMap<VectorProvider, SearchPort> searchMap = new EnumMap<>(VectorProvider.class);

        storeMap.put(VectorProvider.IN_MEMORY, memAdapter);
        searchMap.put(VectorProvider.IN_MEMORY, memAdapter);
        storeMap.put(VectorProvider.INTEGRALLIS, intAdapter);
        searchMap.put(VectorProvider.INTEGRALLIS, intAdapter);

        registry = new VectorProviderRegistry(storeMap, searchMap);
        currentProvider = new AtomicReference<>(VectorProvider.IN_MEMORY);
    }

    /** Minimal {@link ActiveVectorProvider} backed by an AtomicReference. */
    private ActiveVectorProvider activeProviderFor(AtomicReference<VectorProvider> ref) {
        return new ActiveVectorProvider() {
            @Override public VectorProvider current() { return ref.get(); }
            @Override public void select(VectorProvider p) { ref.set(p); }
            @Override public List<VectorProvider> available() {
                return List.of(VectorProvider.IN_MEMORY, VectorProvider.INTEGRALLIS);
            }
        };
    }

    @Test
    void storeDocument_after_switch_goes_to_new_provider() {
        VectorService service = new VectorService(mockEmbedding, registry, activeProviderFor(currentProvider));

        // Store in IN_MEMORY
        service.storeDocument("doc-a", "content a");

        // Switch to INTEGRALLIS
        currentProvider.set(VectorProvider.INTEGRALLIS);

        // Store in INTEGRALLIS
        service.storeDocument("doc-b", "content b");

        // IN_MEMORY should have 1 document
        List<SearchResult> memResults = memAdapter.search(new Embedding(FIXED_VECTOR),
                dev.jsanca.argonaut.vector.domain.SearchOptions.top(10));
        assertThat(memResults).hasSize(1);
        assertThat(memResults.get(0).document().id()).isEqualTo("doc-a");

        // INTEGRALLIS slot should have 1 document
        List<SearchResult> intResults = intAdapter.search(new Embedding(FIXED_VECTOR),
                dev.jsanca.argonaut.vector.domain.SearchOptions.top(10));
        assertThat(intResults).hasSize(1);
        assertThat(intResults.get(0).document().id()).isEqualTo("doc-b");
    }

    @Test
    void search_after_switch_returns_results_from_new_provider() {
        VectorService service = new VectorService(mockEmbedding, registry, activeProviderFor(currentProvider));

        // Pre-populate each backend directly
        service.storeDocument("mem-doc", "memory content");
        currentProvider.set(VectorProvider.INTEGRALLIS);
        service.storeDocument("int-doc", "integrallis content");

        // Search while INTEGRALLIS is active
        List<SearchResult> results = service.search("query", 5);
        assertThat(results).extracting(r -> r.document().id()).containsExactly("int-doc");
    }

    @Test
    void provider_isolation_documents_in_a_not_visible_in_b() {
        VectorService service = new VectorService(mockEmbedding, registry, activeProviderFor(currentProvider));

        // Store two documents in IN_MEMORY
        service.storeDocument("doc-1", "first");
        service.storeDocument("doc-2", "second");

        // Switch to INTEGRALLIS — should see empty
        currentProvider.set(VectorProvider.INTEGRALLIS);
        List<SearchResult> results = service.search("query", 10);
        assertThat(results).isEmpty();
    }
}
