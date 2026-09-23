package dev.jsanca.argonaut.decision.jev.internal.transport;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jsanca.argonaut.decision.jev.internal.dto.JevRequestDto;
import dev.jsanca.argonaut.decision.jev.internal.dto.JevResponseDto;
import dev.jsanca.argonaut.decision.jev.internal.error.JevAuthException;
import dev.jsanca.argonaut.decision.jev.internal.error.JevException;
import dev.jsanca.argonaut.decision.jev.internal.error.JevOverloadException;
import dev.jsanca.argonaut.decision.jev.internal.error.JevRateLimitException;
import dev.jsanca.argonaut.decision.jev.internal.error.JevTransportException;
import dev.jsanca.argonaut.decision.jev.internal.error.JevValidationException;
import dev.langchain4j.exception.HttpException;
import dev.langchain4j.exception.TimeoutException;
import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpMethod;
import dev.langchain4j.http.client.HttpRequest;
import dev.langchain4j.http.client.SuccessfulHttpResponse;
import dev.langchain4j.internal.RetryUtils;

import java.io.IOException;

/**
 * Low-level HTTP transport for the Jev System One API backed by LangChain4j infrastructure.
 *
 * <p>Posts to {@code {baseUrl}/v1/systemone} with a Bearer token and returns the
 * deserialized {@link JevResponseDto}. HTTP and network errors are translated into
 * the appropriate {@link dev.jsanca.argonaut.decision.jev.internal.error.JevException} subclass.
 * Transient errors (429, 503, timeouts) are retried according to the configured policy.</p>
 */
public final class JevHttpTransport {

    private final String baseUrl;
    private final String apiKey;
    private final HttpClient httpClient;
    private final RetryUtils.RetryPolicy retryPolicy;
    private final ObjectMapper mapper;

    public JevHttpTransport(
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
     * Sends the request to the Jev API and returns the parsed response.
     *
     * <p>Transient errors (429, 503, transport) are retried per the configured policy.
     * Non-retriable errors (401, 422) propagate immediately.</p>
     *
     * @throws JevAuthException        on HTTP 401 (non-retriable)
     * @throws JevValidationException  on HTTP 422 or domain mapping failure (non-retriable)
     * @throws JevRateLimitException   on HTTP 429 after retries exhausted (retriable)
     * @throws JevOverloadException    on HTTP 503 after retries exhausted (retriable)
     * @throws JevException            on other HTTP 4xx/5xx after retries exhausted (retriable for 5xx)
     * @throws JevTransportException   on connect/read timeout or IO error after retries exhausted (retriable)
     */
    public JevResponseDto send(final JevRequestDto request) {
        final String requestBody;
        try {
            requestBody = mapper.writeValueAsString(request);
        } catch (final IOException e) {
            throw new JevValidationException("Failed to serialize Jev request: " + e.getMessage(), 0);
        }

        final HttpRequest httpRequest = HttpRequest.builder()
                .method(HttpMethod.POST)
                .url(baseUrl, "v1/systemone")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + apiKey)
                .body(requestBody)
                .build();

        return retryPolicy.withRetry(() -> executeOnce(httpRequest));
    }

    private JevResponseDto executeOnce(final HttpRequest httpRequest) {
        final SuccessfulHttpResponse response;
        try {
            response = httpClient.execute(httpRequest);
        } catch (final TimeoutException e) {
            throw new JevTransportException("Jev API request timed out", e);
        } catch (final HttpException e) {
            throw mapHttpException(e);
        } catch (final RuntimeException e) {
            throw new JevTransportException("Transport error communicating with Jev API", e);
        }

        try {
            return mapper.readValue(response.body(), JevResponseDto.class);
        } catch (final IOException e) {
            throw new JevValidationException("Failed to deserialize Jev response: " + e.getMessage(), 0);
        }
    }

    private static RuntimeException mapHttpException(final HttpException e) {
        final int status = e.statusCode();
        final String msg = "Jev API error HTTP " + status + ": " + e.getMessage();
        if (status == 401 || status == 403) {
            return new JevAuthException(msg);
        } else if (status == 422) {
            return new JevValidationException(msg, status);
        } else if (status == 429) {
            return new JevRateLimitException(msg);
        } else if (status == 503) {
            return new JevOverloadException(msg);
        } else {
            return new JevException(msg, status);
        }
    }
}
