package dev.jsanca.argonaut.vector.config;

import dev.jsanca.argonaut.vector.adapter.memory.InMemoryVectorAdapter;
import dev.jsanca.argonaut.vector.port.SearchPort;
import dev.jsanca.argonaut.vector.port.StorePort;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link RuntimeVectorProvider}.
 */
class RuntimeVectorProviderTest {

    /** Constructs {@link VectorProperties} with the given initial provider. */
    private static VectorProperties propsFor(VectorProvider initial) {
        return new VectorProperties(
                new VectorProperties.EmbeddingConfig("onnx"),
                new VectorProperties.VectorConfig(
                        initial,
                        new VectorProperties.QdrantConfig("localhost", 6334),
                        new VectorProperties.IntegrallisConfig("")));
    }

    /** Builds a registry containing only IN_MEMORY and INTEGRALLIS (Qdrant absent). */
    private VectorProviderRegistry twoProviderRegistry() {
        InMemoryVectorAdapter memAdapter = new InMemoryVectorAdapter();
        InMemoryVectorAdapter intAdapter = new InMemoryVectorAdapter();

        EnumMap<VectorProvider, StorePort> storeMap = new EnumMap<>(VectorProvider.class);
        EnumMap<VectorProvider, SearchPort> searchMap = new EnumMap<>(VectorProvider.class);

        storeMap.put(VectorProvider.IN_MEMORY, memAdapter);
        searchMap.put(VectorProvider.IN_MEMORY, memAdapter);
        storeMap.put(VectorProvider.INTEGRALLIS, intAdapter);
        searchMap.put(VectorProvider.INTEGRALLIS, intAdapter);

        return new VectorProviderRegistry(storeMap, searchMap);
    }

    @Test
    void default_provider_from_config_becomes_active() {
        VectorProviderRegistry registry = twoProviderRegistry();
        RuntimeVectorProvider rvp = new RuntimeVectorProvider(propsFor(VectorProvider.IN_MEMORY), registry);
        assertThat(rvp.current()).isEqualTo(VectorProvider.IN_MEMORY);
    }

    @Test
    void available_returns_registry_available_list() {
        VectorProviderRegistry registry = twoProviderRegistry();
        RuntimeVectorProvider rvp = new RuntimeVectorProvider(propsFor(VectorProvider.IN_MEMORY), registry);
        List<VectorProvider> available = rvp.available();
        assertThat(available).containsExactlyInAnyOrder(VectorProvider.IN_MEMORY, VectorProvider.INTEGRALLIS);
    }

    @Test
    void select_valid_provider_changes_current() {
        VectorProviderRegistry registry = twoProviderRegistry();
        RuntimeVectorProvider rvp = new RuntimeVectorProvider(propsFor(VectorProvider.IN_MEMORY), registry);

        rvp.select(VectorProvider.INTEGRALLIS);

        assertThat(rvp.current()).isEqualTo(VectorProvider.INTEGRALLIS);
    }

    @Test
    void select_unknown_provider_throws_and_current_unchanged() {
        VectorProviderRegistry registry = twoProviderRegistry();
        RuntimeVectorProvider rvp = new RuntimeVectorProvider(propsFor(VectorProvider.IN_MEMORY), registry);

        assertThatThrownBy(() -> rvp.select(VectorProvider.QDRANT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("QDRANT");

        // Current must remain unchanged
        assertThat(rvp.current()).isEqualTo(VectorProvider.IN_MEMORY);
    }

    @Test
    void constructor_throws_when_initial_provider_not_in_registry() {
        // Registry has only IN_MEMORY; config requests INTEGRALLIS — but let's remove INTEGRALLIS
        InMemoryVectorAdapter memAdapter = new InMemoryVectorAdapter();
        EnumMap<VectorProvider, StorePort> storeMap = new EnumMap<>(VectorProvider.class);
        EnumMap<VectorProvider, SearchPort> searchMap = new EnumMap<>(VectorProvider.class);
        storeMap.put(VectorProvider.IN_MEMORY, memAdapter);
        searchMap.put(VectorProvider.IN_MEMORY, memAdapter);
        VectorProviderRegistry registry = new VectorProviderRegistry(storeMap, searchMap);

        assertThatThrownBy(() -> new RuntimeVectorProvider(propsFor(VectorProvider.QDRANT), registry))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("QDRANT");
    }

    @Test
    void concurrency_switch_between_two_providers_no_exception() throws InterruptedException {
        VectorProviderRegistry registry = twoProviderRegistry();
        RuntimeVectorProvider rvp = new RuntimeVectorProvider(propsFor(VectorProvider.IN_MEMORY), registry);

        int threads = 10;
        int switchesPerThread = 100;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicReference<Throwable> firstError = new AtomicReference<>();

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int t = 0; t < threads; t++) {
            final int threadIndex = t;
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    for (int i = 0; i < switchesPerThread; i++) {
                        VectorProvider target = (threadIndex % 2 == 0)
                                ? VectorProvider.IN_MEMORY
                                : VectorProvider.INTEGRALLIS;
                        rvp.select(target);
                        // just read — must not throw
                        rvp.current();
                    }
                } catch (Throwable e) {
                    firstError.compareAndSet(null, e);
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        done.await();
        pool.shutdown();

        assertThat(firstError.get()).isNull();
        // current must be one of the two valid providers
        assertThat(rvp.current()).isIn(VectorProvider.IN_MEMORY, VectorProvider.INTEGRALLIS);
    }
}
