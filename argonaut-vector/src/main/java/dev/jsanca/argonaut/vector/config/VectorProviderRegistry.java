package dev.jsanca.argonaut.vector.config;

import dev.jsanca.argonaut.vector.port.SearchPort;
import dev.jsanca.argonaut.vector.port.StorePort;

import java.util.List;
import java.util.Map;

/**
 * Immutable registry of all wired vector-store adapters, keyed by {@link VectorProvider}.
 *
 * <p>Created as a {@code @Bean} in {@link VectorConfiguration} — not a {@code @Component}.
 * The registry is populated at startup and never mutated after construction.
 */
public class VectorProviderRegistry {

    private final Map<VectorProvider, StorePort> storeMap;
    private final Map<VectorProvider, SearchPort> searchMap;

    public VectorProviderRegistry(Map<VectorProvider, StorePort> storeMap,
                                   Map<VectorProvider, SearchPort> searchMap) {
        this.storeMap = Map.copyOf(storeMap);
        this.searchMap = Map.copyOf(searchMap);
    }

    public StorePort storeFor(VectorProvider provider) {
        StorePort port = storeMap.get(provider);
        if (port == null) throw new IllegalArgumentException(
                "Vector provider not available: " + provider + ". Available: " + storeMap.keySet());
        return port;
    }

    public SearchPort searchFor(VectorProvider provider) {
        SearchPort port = searchMap.get(provider);
        if (port == null) throw new IllegalArgumentException(
                "Vector provider not available: " + provider + ". Available: " + searchMap.keySet());
        return port;
    }

    public List<VectorProvider> available() {
        return List.copyOf(storeMap.keySet());
    }

    public boolean contains(VectorProvider provider) {
        return storeMap.containsKey(provider);
    }
}
