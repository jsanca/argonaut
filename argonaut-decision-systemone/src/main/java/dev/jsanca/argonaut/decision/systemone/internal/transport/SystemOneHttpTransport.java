package dev.jsanca.argonaut.decision.systemone.internal.transport;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jsanca.argonaut.decision.systemone.internal.dto.SystemOneRequestDto;
import dev.jsanca.argonaut.decision.systemone.internal.dto.SystemOneResponseDto;
import dev.jsanca.argonaut.decision.systemone.internal.error.SystemOneAuthException;
import dev.jsanca.argonaut.decision.systemone.internal.error.SystemOneException;
import dev.jsanca.argonaut.decision.systemone.internal.error.SystemOneOverloadException;
import dev.jsanca.argonaut.decision.systemone.internal.error.SystemOneRateLimitException;
import dev.jsanca.argonaut.decision.systemone.internal.error.SystemOneTransportException;
import dev.jsanca.argonaut.decision.systemone.internal.error.SystemOneValidationException;
import dev.langchain4j.exception.HttpException;
import dev.langchain4j.exception.TimeoutException;
import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpMethod;
import dev.langchain4j.http.client.HttpRequest;
import dev.langchain4j.http.client.SuccessfulHttpResponse;
import dev.langchain4j.internal.RetryUtils;

import java.io.IOException;

/**
 * Low-level HTTP transport for the System One API backed by LangChain4j infrastructure.
 *
 * <p>Posts to {@code {baseUrl}/v1/systemone} with a Bearer token and returns the
 * deserialized {@link SystemOneResponseDto}. HTTP and network errors are translated into
 * the appropriate {@link SystemOneException} subclass.
 * Transient errors (429, 503, timeouts) are retried according to the configured policy.</p>
 *
 * <p>Tolerant deserialization ({@code FAIL_ON_UNKNOWN_PROPERTIES = false}) ensures
 * provider-specific extension fields (e.g. Kev's {@code latency_ms}, OpenRouter's
 * {@code id} and {@code usage.cost}) are silently ignored.</p>
 */
public final class SystemOneHttpTransport {

    private final String baseUrl;
    private final String apiKey;
    private final HttpClient httpClient;
    private final RetryUtils.RetryPolicy retryPolicy;
    private final ObjectMapper mapper;

    public SystemOneHttpTransport(
            final String baseUrl,
            final String apiKey,
            final HttpClient httpClient,
            final RetryUtils.RetryPolicy retryPolicy) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.httpClient = httpClient;
        this.retryPolicy = retryPolicy;
        this.mapper = new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /**
     * Sends the request to the System One API and returns the parsed response.
     *
     * <p>Transient errors (429, 503, transport) are retried per the configured policy.
     * Non-retriable errors (401, 422) propagate immediately.</p>
     *
     * @throws SystemOneAuthException        on HTTP 401 (non-retriable)
     * @throws SystemOneValidationException  on HTTP 422 or domain mapping failure (non-retriable)
     * @throws SystemOneRateLimitException   on HTTP 429 after retries exhausted (retriable)
     * @throws SystemOneOverloadException    on HTTP 503 after retries exhausted (retriable)
     * @throws SystemOneException            on other HTTP 4xx/5xx after retries exhausted (retriable for 5xx)
     * @throws SystemOneTransportException   on connect/read timeout or IO error after retries exhausted (retriable)
     */
    public SystemOneResponseDto send(final SystemOneRequestDto request) {
        return send(request, "v1/systemone");
    }

    /**
     * Sends the request to the specified System One endpoint and returns the parsed response.
     *
     * @throws SystemOneAuthException        on HTTP 401 (non-retriable)
     * @throws SystemOneValidationException  on HTTP 422 or domain mapping failure (non-retriable)
     * @throws SystemOneRateLimitException   on HTTP 429 after retries exhausted (retriable)
     * @throws SystemOneOverloadException    on HTTP 503 after retries exhausted (retriable)
     * @throws SystemOneException            on other HTTP 4xx/5xx after retries exhausted (retriable for 5xx)
     * @throws SystemOneTransportException   on connect/read timeout or IO error after retries exhausted (retriable)
     */
    public SystemOneResponseDto send(final SystemOneRequestDto request, final String endpoint) {
        final String requestBody;
        try {
            requestBody = mapper.writeValueAsString(request);
        } catch (final IOException e) {
            throw new SystemOneValidationException(
                    "Failed to serialize System One request: " + e.getMessage(), 0);
        }

        final HttpRequest httpRequest = HttpRequest.builder()
                .method(HttpMethod.POST)
                .url(baseUrl, endpoint)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + apiKey)
                .body(requestBody)
                .build();

        return retryPolicy.withRetry(() -> executeOnce(httpRequest));
    }

    private SystemOneResponseDto executeOnce(final HttpRequest httpRequest) {
        final SuccessfulHttpResponse response;
        try {
            response = httpClient.execute(httpRequest);
        } catch (final TimeoutException e) {
            throw new SystemOneTransportException("System One API request timed out", e);
        } catch (final HttpException e) {
            throw mapHttpException(e);
        } catch (final RuntimeException e) {
            throw new SystemOneTransportException(
                    "Transport error communicating with System One API", e);
        }

        try {
            return mapper.readValue(response.body(), SystemOneResponseDto.class);
        } catch (final IOException e) {
            throw new SystemOneValidationException(
                    "Failed to deserialize System One response: " + e.getMessage(), 0);
        }
    }

    private static RuntimeException mapHttpException(final HttpException e) {
        final int status = e.statusCode();
        final String msg = "System One API error HTTP " + status + ": " + e.getMessage();
        if (status == 401 || status == 403) {
            return new SystemOneAuthException(msg);
        } else if (status == 422) {
            return new SystemOneValidationException(msg, status);
        } else if (status == 429) {
            return new SystemOneRateLimitException(msg);
        } else if (status == 503) {
            return new SystemOneOverloadException(msg);
        } else {
            return new SystemOneException(msg, status);
        }
    }
}
