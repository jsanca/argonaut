package dev.jsanca.argonaut.decision.jev;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import dev.jsanca.argonaut.decision.DecisionContext;
import dev.jsanca.argonaut.decision.DecisionRequest;
import dev.jsanca.argonaut.decision.DecisionResult;
import dev.jsanca.argonaut.decision.jev.internal.error.JevAuthException;
import dev.jsanca.argonaut.decision.jev.internal.error.JevException;
import dev.jsanca.argonaut.decision.jev.internal.error.JevRateLimitException;
import dev.jsanca.argonaut.decision.jev.internal.error.JevTransportException;
import dev.jsanca.argonaut.decision.jev.internal.error.JevValidationException;
import dev.jsanca.argonaut.decision.question.Choice;
import dev.jsanca.argonaut.decision.question.Noul;
import dev.jsanca.argonaut.decision.question.Score;
import dev.jsanca.argonaut.decision.result.ChoiceResult;
import dev.jsanca.argonaut.decision.result.NoulResult;
import dev.jsanca.argonaut.decision.result.ScoreResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JevDecisionModel} using WireMock to stub the Jev HTTP API.
 * No real network calls are made.
 */
@WireMockTest
class JevDecisionModelTest {

    private static final String FAKE_API_KEY = "test-key-123";
    private static final String HAPPY_CHOICE_RESPONSE = """
            {
              "answers": {
                "route": {
                  "type": "choice",
                  "choice": "FAST",
                  "confidence": 0.9,
                  "probabilities": {"FAST": 0.9, "SLOW": 0.1}
                }
              },
              "usage": {"input_tokens": 100, "output_tokens": 20}
            }
            """;

    private String wireMockBaseUrl;

    @BeforeEach
    void setUp(final WireMockRuntimeInfo wmInfo) {
        wireMockBaseUrl = wmInfo.getHttpBaseUrl();
    }

    /** Builds a model with no retries — keeps existing error tests fast. */
    private JevDecisionModel buildModel() {
        return JevDecisionModel.builder()
                .apiKey(FAKE_API_KEY)
                .baseUrl(wireMockBaseUrl)
                .maxRetries(0)
                .build();
    }

    /** Builds a model with a short read timeout and no retries. */
    private JevDecisionModel buildModelWithReadTimeout(final Duration timeout) {
        return JevDecisionModel.builder()
                .apiKey(FAKE_API_KEY)
                .baseUrl(wireMockBaseUrl)
                .readTimeout(timeout)
                .maxRetries(0)
                .build();
    }

    /** Builds a model for retry tests: minimal delay (1 ms) + specified maxRetries. */
    private JevDecisionModel buildModelForRetry(final int maxRetries) {
        return JevDecisionModel.builder()
                .apiKey(FAKE_API_KEY)
                .baseUrl(wireMockBaseUrl)
                .maxRetries(maxRetries)
                .build();
    }

    private static DecisionRequest routeRequest() {
        final Choice<String> question = Choice.ofStrings("route", "Select the execution route",
                List.of("FAST", "SLOW"));
        return DecisionRequest.builder()
                .context(DecisionContext.of("test context"))
                .question(question)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JV-001: Choice<String> — happy path
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void jv001_choiceString_happyPath() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(okJson("""
                        {
                          "answers": {
                            "route": {
                              "type": "choice",
                              "choice": "FAST",
                              "confidence": 0.9,
                              "probabilities": {"FAST": 0.9, "SLOW": 0.1}
                            }
                          },
                          "usage": {"input_tokens": 100, "output_tokens": 20}
                        }
                        """)));

        final Choice<String> question = Choice.ofStrings("route",
                "Select the execution route", List.of("FAST", "SLOW"));
        final DecisionRequest request = DecisionRequest.builder()
                .context(DecisionContext.of("test context"))
                .question(question)
                .build();

        final DecisionResult result = buildModel().decide(request);
        final ChoiceResult<String> answer = result.get(question);

        assertEquals("FAST", answer.selected());
        assertEquals(0.9, answer.confidence(), 1e-9);
        assertNotNull(answer.distribution());
        assertEquals(2, answer.distribution().entries().size());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JV-002: Choice<Enum> — happy path
    // ─────────────────────────────────────────────────────────────────────────

    enum Route { FAST, SLOW }

    @Test
    void jv002_choiceEnum_happyPath() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(okJson("""
                        {
                          "answers": {
                            "route": {
                              "type": "choice",
                              "choice": "SLOW",
                              "confidence": 0.7
                            }
                          },
                          "usage": {"input_tokens": 80, "output_tokens": 15}
                        }
                        """)));

        final Choice<Route> question = Choice.ofEnum("route", "Pick a route", Route.class);
        final DecisionRequest request = DecisionRequest.builder()
                .context(DecisionContext.of("enum context"))
                .question(question)
                .build();

        final DecisionResult result = buildModel().decide(request);
        final ChoiceResult<Route> answer = result.get(question);

        assertEquals(Route.SLOW, answer.selected());
        assertEquals(0.7, answer.confidence(), 1e-9);
        assertNull(answer.distribution());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JV-003: Score — happy path with fractional rawScore
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void jv003_score_happyPath_fractionalRawScore() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(okJson("""
                        {
                          "answers": {
                            "severity": {
                              "type": "score",
                              "score": 1.04,
                              "confidence": 0.8,
                              "probabilities": {"0": 0.1, "1": 0.85, "2": 0.05},
                              "legend": ["LOW", "MEDIUM", "HIGH"]
                            }
                          },
                          "usage": {"input_tokens": 120, "output_tokens": 30}
                        }
                        """)));

        final Score<String> question = Score.of("severity",
                "Evaluate severity", List.of("LOW", "MEDIUM", "HIGH"));
        final DecisionRequest request = DecisionRequest.builder()
                .context(DecisionContext.of("incident context"))
                .question(question)
                .build();

        final DecisionResult result = buildModel().decide(request);
        final ScoreResult<String> answer = result.get(question);

        assertEquals("MEDIUM", answer.selected()); // argmax of probabilities: index 1
        assertEquals(1.04, answer.rawScore(), 1e-9);
        assertEquals(0.8, answer.confidence(), 1e-9);
        assertNotNull(answer.distribution());
        assertEquals(3, answer.distribution().entries().size());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JV-004: Noul — happy path
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void jv004_noul_happyPath() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(okJson("""
                        {
                          "answers": {
                            "needs_review": {
                              "type": "noul",
                              "noul": 0.73,
                              "confidence": 0.9
                            }
                          },
                          "usage": {"input_tokens": 90, "output_tokens": 10}
                        }
                        """)));

        final Noul question = Noul.of("needs_review", "This incident requires immediate attention");
        final DecisionRequest request = DecisionRequest.builder()
                .context(DecisionContext.of("incident"))
                .question(question)
                .build();

        final DecisionResult result = buildModel().decide(request);
        final NoulResult answer = result.get(question);

        assertEquals(0.73, answer.probabilityTrue().value(), 1e-9);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JV-005: Heterogeneous batch (Choice + Score + Noul) — single HTTP call
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void jv005_heterogeneousBatch_singleHttpCall() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(okJson("""
                        {
                          "answers": {
                            "route": {
                              "type": "choice",
                              "choice": "FAST",
                              "confidence": 0.85
                            },
                            "severity": {
                              "type": "score",
                              "score": 0.5,
                              "probabilities": {"0": 0.6, "1": 0.3, "2": 0.1}
                            },
                            "needs_review": {
                              "type": "noul",
                              "noul": 0.4
                            }
                          },
                          "usage": {"input_tokens": 200, "output_tokens": 50}
                        }
                        """)));

        final Choice<String> routeQ = Choice.ofStrings("route", "Select route", List.of("FAST", "SLOW"));
        final Score<String> severityQ = Score.of("severity", "Rate severity", List.of("LOW", "MEDIUM", "HIGH"));
        final Noul reviewQ = Noul.of("needs_review", "Needs review");

        final DecisionRequest request = DecisionRequest.builder()
                .context(DecisionContext.of("batch context"))
                .question(routeQ)
                .question(severityQ)
                .question(reviewQ)
                .build();

        final DecisionResult result = buildModel().decide(request);

        assertEquals("FAST", result.get(routeQ).selected());
        assertEquals("LOW", result.get(severityQ).selected()); // argmax index 0
        assertEquals(0.4, result.get(reviewQ).probabilityTrue().value(), 1e-9);

        // Verify exactly ONE HTTP call was made
        verify(1, postRequestedFor(urlEqualTo("/v1/systemone")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JV-006: Probability distribution — complete mapping
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void jv006_choiceWithFullProbabilityDistribution() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(okJson("""
                        {
                          "answers": {
                            "color": {
                              "type": "choice",
                              "choice": "RED",
                              "confidence": 0.6,
                              "probabilities": {"RED": 0.6, "GREEN": 0.25, "BLUE": 0.15}
                            }
                          },
                          "usage": {"input_tokens": 50, "output_tokens": 10}
                        }
                        """)));

        final Choice<String> question = Choice.ofStrings("color", "Pick a color",
                List.of("RED", "GREEN", "BLUE"));
        final DecisionRequest request = DecisionRequest.builder()
                .context(DecisionContext.of("color context"))
                .question(question)
                .build();

        final DecisionResult result = buildModel().decide(request);
        final ChoiceResult<String> answer = result.get(question);

        assertEquals("RED", answer.selected());
        assertNotNull(answer.distribution());
        assertEquals(3, answer.distribution().entries().size());

        // Find the RED entry and verify its probability
        final double redProb = answer.distribution().entries().stream()
                .filter(e -> "RED".equals(e.outcome()))
                .mapToDouble(e -> e.probability().value())
                .findFirst()
                .orElseThrow();
        assertEquals(0.6, redProb, 1e-9);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JV-007: Absent confidence and probabilities — null in domain types
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void jv007_absentConfidenceAndProbabilities_nullInDomainTypes() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(okJson("""
                        {
                          "answers": {
                            "route": {
                              "type": "choice",
                              "choice": "FAST"
                            }
                          },
                          "usage": {"input_tokens": 60, "output_tokens": 10}
                        }
                        """)));

        final Choice<String> question = Choice.ofStrings("route", "Select route",
                List.of("FAST", "SLOW"));
        final DecisionRequest request = DecisionRequest.builder()
                .context(DecisionContext.of("minimal context"))
                .question(question)
                .build();

        final DecisionResult result = buildModel().decide(request);
        final ChoiceResult<String> answer = result.get(question);

        assertEquals("FAST", answer.selected());
        assertNull(answer.confidence());
        assertNull(answer.distribution());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JV-008: Unknown choice candidate in response → JevValidationException
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void jv008_unknownChoiceCandidate_throwsJevValidationException() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(okJson("""
                        {
                          "answers": {
                            "route": {
                              "type": "choice",
                              "choice": "UNKNOWN_KEY"
                            }
                          },
                          "usage": {"input_tokens": 60, "output_tokens": 10}
                        }
                        """)));

        final Choice<String> question = Choice.ofStrings("route", "Select route",
                List.of("FAST", "SLOW"));
        final DecisionRequest request = DecisionRequest.builder()
                .context(DecisionContext.of("context"))
                .question(question)
                .build();

        assertThrows(JevValidationException.class, () -> buildModel().decide(request));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JV-009: Missing question in response → JevValidationException
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void jv009_missingQuestionInResponse_throwsJevValidationException() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(okJson("""
                        {
                          "answers": {
                            "route": {
                              "type": "choice",
                              "choice": "FAST"
                            }
                          },
                          "usage": {"input_tokens": 60, "output_tokens": 10}
                        }
                        """)));

        final Choice<String> routeQ = Choice.ofStrings("route", "Select route", List.of("FAST", "SLOW"));
        final Noul reviewQ = Noul.of("needs_review", "Needs review");
        final DecisionRequest request = DecisionRequest.builder()
                .context(DecisionContext.of("context"))
                .question(routeQ)
                .question(reviewQ)
                .build();

        assertThrows(JevValidationException.class, () -> buildModel().decide(request));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JV-010: Unexpected question in response → JevValidationException
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void jv010_unexpectedQuestionInResponse_throwsJevValidationException() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(okJson("""
                        {
                          "answers": {
                            "route": {
                              "type": "choice",
                              "choice": "FAST"
                            },
                            "extra_question": {
                              "type": "noul",
                              "noul": 0.5
                            }
                          },
                          "usage": {"input_tokens": 60, "output_tokens": 10}
                        }
                        """)));

        final Choice<String> question = Choice.ofStrings("route", "Select route", List.of("FAST", "SLOW"));
        final DecisionRequest request = DecisionRequest.builder()
                .context(DecisionContext.of("context"))
                .question(question)
                .build();

        assertThrows(JevValidationException.class, () -> buildModel().decide(request));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JV-011: Invalid probability in response → JevValidationException
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void jv011_invalidProbabilityInResponse_throwsJevValidationException() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(okJson("""
                        {
                          "answers": {
                            "route": {
                              "type": "choice",
                              "choice": "FAST",
                              "probabilities": {"FAST": 1.5, "SLOW": -0.5}
                            }
                          },
                          "usage": {"input_tokens": 60, "output_tokens": 10}
                        }
                        """)));

        final Choice<String> question = Choice.ofStrings("route", "Select route", List.of("FAST", "SLOW"));
        final DecisionRequest request = DecisionRequest.builder()
                .context(DecisionContext.of("context"))
                .question(question)
                .build();

        assertThrows(JevValidationException.class, () -> buildModel().decide(request));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JV-012: HTTP 401 → JevAuthException (non-retriable)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void jv012_http401_throwsJevAuthException() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(status(401).withBody("Unauthorized")));

        final Choice<String> question = Choice.ofStrings("route", "Select route", List.of("FAST", "SLOW"));
        final DecisionRequest request = DecisionRequest.builder()
                .context(DecisionContext.of("context"))
                .question(question)
                .build();

        final JevAuthException ex = assertThrows(JevAuthException.class,
                () -> buildModel().decide(request));
        assertEquals(401, ex.httpStatus());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JV-013: HTTP 429 → JevRateLimitException (retriable, no retries configured)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void jv013_http429_throwsJevRateLimitException() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(status(429).withBody("Rate limit exceeded")));

        final Choice<String> question = Choice.ofStrings("route", "Select route", List.of("FAST", "SLOW"));
        final DecisionRequest request = DecisionRequest.builder()
                .context(DecisionContext.of("context"))
                .question(question)
                .build();

        final JevRateLimitException ex = assertThrows(JevRateLimitException.class,
                () -> buildModel().decide(request));
        assertEquals(429, ex.httpStatus());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JV-014: HTTP 500 → JevException with correct status (retriable, no retries)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void jv014_http500_throwsJevExceptionWithStatus500() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(status(500).withBody("Internal Server Error")));

        final Choice<String> question = Choice.ofStrings("route", "Select route", List.of("FAST", "SLOW"));
        final DecisionRequest request = DecisionRequest.builder()
                .context(DecisionContext.of("context"))
                .question(question)
                .build();

        final JevException ex = assertThrows(JevException.class,
                () -> buildModel().decide(request));
        assertEquals(500, ex.httpStatus());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JV-015: Read timeout → JevTransportException (retriable, no retries)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void jv015_timeout_throwsJevTransportException() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(okJson("""
                        {
                          "answers": {
                            "route": {"type": "choice", "choice": "FAST"}
                          },
                          "usage": {"input_tokens": 10, "output_tokens": 5}
                        }
                        """)
                        .withFixedDelay(5000))); // 5s delay > 100ms timeout

        final Choice<String> question = Choice.ofStrings("route", "Select route", List.of("FAST", "SLOW"));
        final DecisionRequest request = DecisionRequest.builder()
                .context(DecisionContext.of("context"))
                .question(question)
                .build();

        assertThrows(JevTransportException.class,
                () -> buildModelWithReadTimeout(Duration.ofMillis(100)).decide(request));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JV-016: HTTP 429 → retry → success (verifies retry attempt count)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void jv016_http429_retrySucceeds() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .inScenario("rate-limit-then-ok")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(status(429).withBody("Rate limit exceeded"))
                .willSetStateTo("retry-ok"));

        stubFor(post(urlEqualTo("/v1/systemone"))
                .inScenario("rate-limit-then-ok")
                .whenScenarioStateIs("retry-ok")
                .willReturn(okJson(HAPPY_CHOICE_RESPONSE)));

        final DecisionResult result = buildModelForRetry(1).decide(routeRequest());
        assertNotNull(result);

        // First call failed (429), second call succeeded — 2 total requests
        verify(2, postRequestedFor(urlEqualTo("/v1/systemone")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JV-017: HTTP 503 → retry → success
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void jv017_http503_retrySucceeds() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .inScenario("overload-then-ok")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(status(503).withBody("Service Unavailable"))
                .willSetStateTo("retry-ok"));

        stubFor(post(urlEqualTo("/v1/systemone"))
                .inScenario("overload-then-ok")
                .whenScenarioStateIs("retry-ok")
                .willReturn(okJson(HAPPY_CHOICE_RESPONSE)));

        final DecisionResult result = buildModelForRetry(1).decide(routeRequest());
        assertNotNull(result);

        verify(2, postRequestedFor(urlEqualTo("/v1/systemone")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JV-018: HTTP 401 → no retry (non-retriable)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void jv018_http401_noRetry() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(status(401).withBody("Unauthorized")));

        assertThrows(JevAuthException.class, () -> buildModelForRetry(2).decide(routeRequest()));

        // Only one attempt — 401 is non-retriable
        verify(1, postRequestedFor(urlEqualTo("/v1/systemone")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JV-019: HTTP 422 → no retry (non-retriable)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void jv019_http422_noRetry() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(status(422).withBody("Unprocessable Entity")));

        assertThrows(JevValidationException.class, () -> buildModelForRetry(2).decide(routeRequest()));

        verify(1, postRequestedFor(urlEqualTo("/v1/systemone")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JV-020: Retry exhaustion — 429 persists → throw after maxRetries+1 attempts
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void jv020_http429_exhaustsRetries_throwsAfterAllAttempts() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(status(429).withBody("Rate limit exceeded")));

        assertThrows(JevRateLimitException.class,
                () -> buildModelForRetry(2).decide(routeRequest()));

        // 1 initial attempt + 2 retries = 3 total
        verify(3, postRequestedFor(urlEqualTo("/v1/systemone")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JV-021: Timeout → retry → success
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void jv021_timeout_retrySucceeds() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .inScenario("timeout-then-ok")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(okJson(HAPPY_CHOICE_RESPONSE).withFixedDelay(5000)) // exceeds read timeout
                .willSetStateTo("retry-ok"));

        stubFor(post(urlEqualTo("/v1/systemone"))
                .inScenario("timeout-then-ok")
                .whenScenarioStateIs("retry-ok")
                .willReturn(okJson(HAPPY_CHOICE_RESPONSE)));

        final JevDecisionModel model = JevDecisionModel.builder()
                .apiKey(FAKE_API_KEY)
                .baseUrl(wireMockBaseUrl)
                .readTimeout(Duration.ofMillis(200))
                .maxRetries(1)
                .build();

        final DecisionResult result = model.decide(routeRequest());
        assertNotNull(result);

        verify(2, postRequestedFor(urlEqualTo("/v1/systemone")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JV-022: Connect and read timeout can be configured independently
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void jv022_connectAndReadTimeoutConfiguredIndependently() {
        // Verifies that the builder accepts both independently (structural test).
        // A live network test for connect timeout is not deterministic in unit scope.
        final JevDecisionModel model = JevDecisionModel.builder()
                .apiKey(FAKE_API_KEY)
                .baseUrl(wireMockBaseUrl)
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(30))
                .maxRetries(0)
                .build();
        assertNotNull(model);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JV-023: Injectable HttpClientBuilder is accepted
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void jv023_injectableHttpClientBuilder_isAccepted() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(okJson(HAPPY_CHOICE_RESPONSE)));

        final dev.langchain4j.http.client.jdk.JdkHttpClientBuilder customBuilder =
                new dev.langchain4j.http.client.jdk.JdkHttpClientBuilder();

        final JevDecisionModel model = JevDecisionModel.builder()
                .apiKey(FAKE_API_KEY)
                .baseUrl(wireMockBaseUrl)
                .httpClientBuilder(customBuilder)
                .maxRetries(0)
                .build();

        final DecisionResult result = model.decide(routeRequest());
        assertNotNull(result);
    }
}
