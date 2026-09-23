package dev.jsanca.argonaut.decision.jev;

import dev.jsanca.argonaut.decision.DecisionCapability;
import dev.jsanca.argonaut.decision.DecisionModel;
import dev.jsanca.argonaut.decision.DecisionRequest;
import dev.jsanca.argonaut.decision.DecisionResult;
import dev.jsanca.argonaut.decision.jev.internal.dto.JevResponseDto;
import dev.jsanca.argonaut.decision.jev.internal.mapping.JevRequestMapper;
import dev.jsanca.argonaut.decision.jev.internal.mapping.JevResponseMapper;
import dev.jsanca.argonaut.decision.jev.internal.transport.JevHttpTransport;
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.internal.RetryUtils;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;

import static dev.jsanca.argonaut.decision.DecisionCapability.*;

/**
 * {@link DecisionModel} implementation backed by the TypeSafe AI Jev System One API.
 *
 * <p>Create via the {@link Builder}:
 * <pre>{@code
 * JevDecisionModel model = JevDecisionModel.builder()
 *     .apiKey(System.getenv("JEV_API_KEY"))
 *     .build();
 * DecisionResult result = model.decide(request);
 * }</pre>
 * </p>
 *
 * <p>The Jev API is a native multi-question batch endpoint — all questions in a
 * {@link DecisionRequest} are submitted in a single HTTP call.</p>
 *
 * <p>Connect and read timeouts are configured independently. Defaults: 10 s connect,
 * 60 s read. Transient failures (429, 503, timeouts) are retried up to {@code maxRetries}
 * times (default 2) with exponential backoff and jitter.</p>
 */
public final class JevDecisionModel implements DecisionModel {

    private static final String DEFAULT_MODEL = "jev-latest";
    private static final String DEFAULT_BASE_URL = "https://api.typesafe.ai";
    /** Default connect timeout: fail fast on unresponsive hosts. */
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    /** Default read timeout: allow time for inference. */
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(60);
    private static final int DEFAULT_MAX_RETRIES = 2;

    private final JevHttpTransport transport;
    private final String modelName;

    private JevDecisionModel(final Builder builder) {
        this.modelName = builder.modelName;

        final HttpClientBuilder httpClientBuilder = (builder.httpClientBuilder != null
                ? builder.httpClientBuilder
                : new JdkHttpClientBuilder())
                .connectTimeout(builder.connectTimeout)
                .readTimeout(builder.readTimeout);

        final RetryUtils.RetryPolicy retryPolicy = RetryUtils.retryPolicyBuilder()
                .maxRetries(builder.maxRetries)
                .delayMillis(500)
                .jitterScale(0.2)
                .backoffExp(1.5)
                .build();

        this.transport = new JevHttpTransport(
                builder.baseUrl,
                builder.apiKey,
                httpClientBuilder.build(),
                retryPolicy);
    }

    @Override
    public DecisionResult decide(final DecisionRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        final JevRequestMapper.MappingResult mapped = JevRequestMapper.map(request, modelName);
        final JevResponseDto response = transport.send(mapped.request());
        return JevResponseMapper.map(response, request, mapped.candidateKeys());
    }

    @Override
    public Set<DecisionCapability> capabilities() {
        return Set.of(CHOICE, SCORE, NOUL, MULTI_QUESTION, NATIVE_BATCH, PROBABILITY,
                PROBABILITY_DISTRIBUTION);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String apiKey;
        private String baseUrl = DEFAULT_BASE_URL;
        private String modelName = DEFAULT_MODEL;
        private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        private Duration readTimeout = DEFAULT_READ_TIMEOUT;
        private int maxRetries = DEFAULT_MAX_RETRIES;
        private HttpClientBuilder httpClientBuilder;

        private Builder() {}

        public Builder apiKey(final String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /** Override the base URL — useful for tests and self-hosted deployments. */
        public Builder baseUrl(final String baseUrl) {
            this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl must not be null");
            return this;
        }

        public Builder modelName(final String modelName) {
            this.modelName = Objects.requireNonNull(modelName, "modelName must not be null");
            return this;
        }

        /** Maximum time to establish a TCP connection. Default: 10 s. */
        public Builder connectTimeout(final Duration connectTimeout) {
            this.connectTimeout = Objects.requireNonNull(connectTimeout, "connectTimeout must not be null");
            return this;
        }

        /** Maximum time to wait for a response after the connection is established. Default: 60 s. */
        public Builder readTimeout(final Duration readTimeout) {
            this.readTimeout = Objects.requireNonNull(readTimeout, "readTimeout must not be null");
            return this;
        }

        /**
         * Maximum number of retry attempts after a transient failure (429, 503, timeout).
         * 0 = no retries. Default: 2.
         */
        public Builder maxRetries(final int maxRetries) {
            if (maxRetries < 0) throw new IllegalArgumentException("maxRetries must be >= 0");
            this.maxRetries = maxRetries;
            return this;
        }

        /**
         * Supply a custom {@link HttpClientBuilder} — useful for testing or advanced SSL configuration.
         * The builder's {@code connectTimeout} and {@code readTimeout} are overwritten by the
         * values from this builder.
         */
        public Builder httpClientBuilder(final HttpClientBuilder httpClientBuilder) {
            this.httpClientBuilder = Objects.requireNonNull(httpClientBuilder, "httpClientBuilder must not be null");
            return this;
        }

        public JevDecisionModel build() {
            Objects.requireNonNull(apiKey, "apiKey must not be null");
            return new JevDecisionModel(this);
        }
    }
}
