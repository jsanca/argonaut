package dev.jsanca.argonaut.vector.config;

import dev.jsanca.argonaut.vector.adapter.integrallis.IntegrallisVectorAdapter;
import dev.jsanca.argonaut.vector.adapter.memory.InMemoryVectorAdapter;
import dev.jsanca.argonaut.vector.adapter.onnx.OnnxEmbeddingAdapter;
import dev.jsanca.argonaut.vector.adapter.qdrant.QdrantVectorAdapter;
import dev.jsanca.argonaut.vector.port.EmbeddingPort;
import dev.jsanca.argonaut.vector.port.SearchPort;
import dev.jsanca.argonaut.vector.port.StorePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;

/**
 * Wires vector-store adapters into a {@link VectorProviderRegistry} and exposes the
 * ONNX embedding adapter as a {@link EmbeddingPort} bean.
 *
 * <p>All three adapters (InMemory, Integrallis, Qdrant) are always instantiated. Qdrant is
 * only added to the registry when {@link QdrantVectorAdapter#isAvailable()} returns {@code true},
 * allowing the application to start even when no Qdrant server is reachable.
 */
@Configuration
@EnableConfigurationProperties(VectorProperties.class)
public class VectorConfiguration {

    private static final Logger log = LoggerFactory.getLogger(VectorConfiguration.class);

    @Bean
    EmbeddingPort embeddingPort() {
        try {
            return new OnnxEmbeddingAdapter();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize ONNX embedding adapter", e);
        }
    }

    @Bean
    VectorProviderRegistry vectorProviderRegistry(
            InMemoryVectorAdapter memory,
            IntegrallisVectorAdapter integrallis,
            QdrantVectorAdapter qdrant) {

        EnumMap<VectorProvider, StorePort> storeMap = new EnumMap<>(VectorProvider.class);
        EnumMap<VectorProvider, SearchPort> searchMap = new EnumMap<>(VectorProvider.class);

        storeMap.put(VectorProvider.IN_MEMORY, memory);
        searchMap.put(VectorProvider.IN_MEMORY, memory);

        storeMap.put(VectorProvider.INTEGRALLIS, integrallis);
        searchMap.put(VectorProvider.INTEGRALLIS, integrallis);

        if (qdrant.isAvailable()) {
            storeMap.put(VectorProvider.QDRANT, qdrant);
            searchMap.put(VectorProvider.QDRANT, qdrant);
            log.info("Qdrant adapter registered in VectorProviderRegistry");
        } else {
            log.info("Qdrant adapter skipped — not reachable at startup");
        }

        log.info("VectorProviderRegistry contains: {}", storeMap.keySet());
        return new VectorProviderRegistry(storeMap, searchMap);
    }
}
