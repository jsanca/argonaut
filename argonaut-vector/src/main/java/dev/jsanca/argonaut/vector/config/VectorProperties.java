package dev.jsanca.argonaut.vector.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the argonaut-vector module.
 *
 * <p>Bound from {@code argonaut.*} in {@code application.yml}.
 */
@ConfigurationProperties(prefix = "argonaut")
public record VectorProperties(
        EmbeddingConfig embedding,
        VectorConfig vector) {

    public record EmbeddingConfig(String provider) {}

    public record VectorConfig(
            VectorProvider provider,
            QdrantConfig qdrant,
            IntegrallisConfig integrallis) {}

    public record QdrantConfig(String host, int grpcPort) {
        public QdrantConfig {
            if (host == null) host = "localhost";
            if (grpcPort == 0) grpcPort = 6334;
        }
    }

    public record IntegrallisConfig(String storagePath) {}
}
