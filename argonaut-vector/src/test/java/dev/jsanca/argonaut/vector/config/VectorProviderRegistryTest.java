package dev.jsanca.argonaut.vector.config;

import dev.jsanca.argonaut.vector.adapter.memory.InMemoryVectorAdapter;
import dev.jsanca.argonaut.vector.port.SearchPort;
import dev.jsanca.argonaut.vector.port.StorePort;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link VectorProviderRegistry}.
 */
class VectorProviderRegistryTest {

    /** Build a registry with IN_MEMORY and INTEGRALLIS, each backed by a fresh InMemoryVectorAdapter. */
    private VectorProviderRegistry registryWithTwoProviders() {
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
    void storeFor_returns_correct_adapter_for_in_memory() {
        VectorProviderRegistry registry = registryWithTwoProviders();
        StorePort port = registry.storeFor(VectorProvider.IN_MEMORY);
        assertThat(port).isNotNull();
    }

    @Test
    void storeFor_returns_correct_adapter_for_integrallis() {
        VectorProviderRegistry registry = registryWithTwoProviders();
        StorePort port = registry.storeFor(VectorProvider.INTEGRALLIS);
        assertThat(port).isNotNull();
    }

    @Test
    void searchFor_returns_correct_adapter_for_in_memory() {
        VectorProviderRegistry registry = registryWithTwoProviders();
        SearchPort port = registry.searchFor(VectorProvider.IN_MEMORY);
        assertThat(port).isNotNull();
    }

    @Test
    void searchFor_returns_correct_adapter_for_integrallis() {
        VectorProviderRegistry registry = registryWithTwoProviders();
        SearchPort port = registry.searchFor(VectorProvider.INTEGRALLIS);
        assertThat(port).isNotNull();
    }

    @Test
    void storeFor_with_unregistered_provider_throws_IllegalArgumentException() {
        VectorProviderRegistry registry = registryWithTwoProviders();
        assertThatThrownBy(() -> registry.storeFor(VectorProvider.QDRANT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("QDRANT");
    }

    @Test
    void searchFor_with_unregistered_provider_throws_IllegalArgumentException() {
        VectorProviderRegistry registry = registryWithTwoProviders();
        assertThatThrownBy(() -> registry.searchFor(VectorProvider.QDRANT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("QDRANT");
    }

    @Test
    void available_returns_all_registered_providers() {
        VectorProviderRegistry registry = registryWithTwoProviders();
        List<VectorProvider> available = registry.available();
        assertThat(available).containsExactlyInAnyOrder(VectorProvider.IN_MEMORY, VectorProvider.INTEGRALLIS);
    }

    @Test
    void contains_returns_true_for_registered_provider() {
        VectorProviderRegistry registry = registryWithTwoProviders();
        assertThat(registry.contains(VectorProvider.IN_MEMORY)).isTrue();
        assertThat(registry.contains(VectorProvider.INTEGRALLIS)).isTrue();
    }

    @Test
    void contains_returns_false_for_unregistered_provider() {
        VectorProviderRegistry registry = registryWithTwoProviders();
        assertThat(registry.contains(VectorProvider.QDRANT)).isFalse();
    }
}
