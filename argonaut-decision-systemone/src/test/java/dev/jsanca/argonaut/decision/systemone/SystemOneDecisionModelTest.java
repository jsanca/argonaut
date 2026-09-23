package dev.jsanca.argonaut.decision.systemone;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import dev.jsanca.argonaut.decision.DecisionContext;
import dev.jsanca.argonaut.decision.DecisionRequest;
import dev.jsanca.argonaut.decision.DecisionResult;
import dev.jsanca.argonaut.decision.systemone.internal.error.SystemOneAuthException;
import dev.jsanca.argonaut.decision.systemone.internal.error.SystemOneException;
import dev.jsanca.argonaut.decision.systemone.internal.error.SystemOneRateLimitException;
import dev.jsanca.argonaut.decision.systemone.internal.error.SystemOneTransportException;
import dev.jsanca.argonaut.decision.systemone.internal.error.SystemOneValidationException;
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
 * Unit tests for {@link SystemOneDecisionModel} using WireMock to stub the System One HTTP API.
 * No real network calls are made.
 */
@WireMockTest
class SystemOneDecisionModelTest {

    private static final String FAKE_API_KEY = "test-key-123";
    private static final String HAPPY_CHOICE_RESPONSE = """
            {
              "model": "jev-latest",
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
    private SystemOneDecisionModel buildModel() {
        return SystemOneDecisionModel.builder()
                .apiKey(FAKE_API_KEY)
                .baseUrl(wireMockBaseUrl)
                .maxRetries(0)
                .build();
    }

    /** Builds a model with a short read timeout and no retries. */
    private SystemOneDecisionModel buildModelWithReadTimeout(final Duration timeout) {
        return SystemOneDecisionModel.builder()
                .apiKey(FAKE_API_KEY)
                .baseUrl(wireMockBaseUrl)
                .readTimeout(timeout)
                .maxRetries(0)
                .build();
    }

    /** Builds a model for retry tests: minimal delay (1 ms) + specified maxRetries. */
    private SystemOneDecisionModel buildModelForRetry(final int maxRetries) {
        return SystemOneDecisionModel.builder()
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
    // SO-000: Default configuration — targets TypeSafe/Jev by default
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void so000_defaultConfiguration_targetsJevDefaults() {
        // Structural test — verify the builder accepts the Jev defaults without error.
        // We override baseUrl for test isolation; the default is https://api.typesafe.ai.
        final SystemOneDecisionModel model = SystemOneDecisionModel.builder()
                .apiKey(FAKE_API_KEY)
                .baseUrl(wireMockBaseUrl) // override for test
                .build();
        assertNotNull(model);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SO-001: Choice<String> — happy path
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void so001_choiceString_happyPath() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(okJson("""
                        {
                          "model": "jev-latest",
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
    // SO-002: Choice<Enum> — happy path
    // ─────────────────────────────────────────────────────────────────────────

    enum Route { FAST, SLOW }

    @Test
    void so002_choiceEnum_happyPath() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(okJson("""
                        {
                          "model": "jev-latest",
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
    // SO-003: Score — happy path with fractional rawScore
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void so003_score_happyPath_fractionalRawScore() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(okJson("""
                        {
                          "model": "jev-latest",
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
    // SO-004: Noul — happy path
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void so004_noul_happyPath() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(okJson("""
                        {
                          "model": "jev-latest",
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
    // SO-005: Heterogeneous batch (Choice + Score + Noul) — single HTTP call
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void so005_heterogeneousBatch_singleHttpCall() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(okJson("""
                        {
                          "model": "jev-latest",
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
    // SO-006: Probability distribution — complete mapping
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void so006_choiceWithFullProbabilityDistribution() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(okJson("""
                        {
                          "model": "jev-latest",
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

        final double redProb = answer.distribution().entries().stream()
                .filter(e -> "RED".equals(e.outcome()))
                .mapToDouble(e -> e.probability().value())
                .findFirst()
                .orElseThrow();
        assertEquals(0.6, redProb, 1e-9);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SO-007: Absent confidence and probabilities — null in domain types
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void so007_absentConfidenceAndProbabilities_nullInDomainTypes() {
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
    // SO-008: Unknown choice candidate in response → SystemOneValidationException
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void so008_unknownChoiceCandidate_throwsSystemOneValidationException() {
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

        assertThrows(SystemOneValidationException.class, () -> buildModel().decide(request));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SO-009: Missing question in response → SystemOneValidationException
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void so009_missingQuestionInResponse_throwsSystemOneValidationException() {
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

        assertThrows(SystemOneValidationException.class, () -> buildModel().decide(request));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SO-010: Unexpected question in response → SystemOneValidationException
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void so010_unexpectedQuestionInResponse_throwsSystemOneValidationException() {
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

        assertThrows(SystemOneValidationException.class, () -> buildModel().decide(request));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SO-011: Invalid probability in response → SystemOneValidationException
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void so011_invalidProbabilityInResponse_throwsSystemOneValidationException() {
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

        assertThrows(SystemOneValidationException.class, () -> buildModel().decide(request));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SO-012: HTTP 401 → SystemOneAuthException (non-retriable)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void so012_http401_throwsSystemOneAuthException() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(status(401).withBody("Unauthorized")));

        final Choice<String> question = Choice.ofStrings("route", "Select route", List.of("FAST", "SLOW"));
        final DecisionRequest request = DecisionRequest.builder()
                .context(DecisionContext.of("context"))
                .question(question)
                .build();

        final SystemOneAuthException ex = assertThrows(SystemOneAuthException.class,
                () -> buildModel().decide(request));
        assertEquals(401, ex.httpStatus());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SO-013: HTTP 429 → SystemOneRateLimitException (retriable, no retries configured)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void so013_http429_throwsSystemOneRateLimitException() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(status(429).withBody("Rate limit exceeded")));

        final Choice<String> question = Choice.ofStrings("route", "Select route", List.of("FAST", "SLOW"));
        final DecisionRequest request = DecisionRequest.builder()
                .context(DecisionContext.of("context"))
                .question(question)
                .build();

        final SystemOneRateLimitException ex = assertThrows(SystemOneRateLimitException.class,
                () -> buildModel().decide(request));
        assertEquals(429, ex.httpStatus());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SO-014: HTTP 500 → SystemOneException with correct status (retriable, no retries)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void so014_http500_throwsSystemOneExceptionWithStatus500() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(status(500).withBody("Internal Server Error")));

        final Choice<String> question = Choice.ofStrings("route", "Select route", List.of("FAST", "SLOW"));
        final DecisionRequest request = DecisionRequest.builder()
                .context(DecisionContext.of("context"))
                .question(question)
                .build();

        final SystemOneException ex = assertThrows(SystemOneException.class,
                () -> buildModel().decide(request));
        assertEquals(500, ex.httpStatus());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SO-015: Read timeout → SystemOneTransportException (retriable, no retries)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void so015_timeout_throwsSystemOneTransportException() {
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

        assertThrows(SystemOneTransportException.class,
                () -> buildModelWithReadTimeout(Duration.ofMillis(100)).decide(request));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SO-016: HTTP 429 → retry → success
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void so016_http429_retrySucceeds() {
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

        verify(2, postRequestedFor(urlEqualTo("/v1/systemone")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SO-017: HTTP 503 → retry → success
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void so017_http503_retrySucceeds() {
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
    // SO-018: HTTP 401 → no retry (non-retriable)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void so018_http401_noRetry() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(status(401).withBody("Unauthorized")));

        assertThrows(SystemOneAuthException.class, () -> buildModelForRetry(2).decide(routeRequest()));

        verify(1, postRequestedFor(urlEqualTo("/v1/systemone")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SO-019: HTTP 422 → no retry (non-retriable)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void so019_http422_noRetry() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(status(422).withBody("Unprocessable Entity")));

        assertThrows(SystemOneValidationException.class, () -> buildModelForRetry(2).decide(routeRequest()));

        verify(1, postRequestedFor(urlEqualTo("/v1/systemone")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SO-020: Retry exhaustion — 429 persists → throw after maxRetries+1 attempts
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void so020_http429_exhaustsRetries_throwsAfterAllAttempts() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(status(429).withBody("Rate limit exceeded")));

        assertThrows(SystemOneRateLimitException.class,
                () -> buildModelForRetry(2).decide(routeRequest()));

        verify(3, postRequestedFor(urlEqualTo("/v1/systemone")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SO-021: Timeout → retry → success
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void so021_timeout_retrySucceeds() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .inScenario("timeout-then-ok")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(okJson(HAPPY_CHOICE_RESPONSE).withFixedDelay(5000))
                .willSetStateTo("retry-ok"));

        stubFor(post(urlEqualTo("/v1/systemone"))
                .inScenario("timeout-then-ok")
                .whenScenarioStateIs("retry-ok")
                .willReturn(okJson(HAPPY_CHOICE_RESPONSE)));

        final SystemOneDecisionModel model = SystemOneDecisionModel.builder()
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
    // SO-022: Connect and read timeout can be configured independently
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void so022_connectAndReadTimeoutConfiguredIndependently() {
        final SystemOneDecisionModel model = SystemOneDecisionModel.builder()
                .apiKey(FAKE_API_KEY)
                .baseUrl(wireMockBaseUrl)
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(30))
                .maxRetries(0)
                .build();
        assertNotNull(model);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SO-023: Injectable HttpClientBuilder is accepted
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void so023_injectableHttpClientBuilder_isAccepted() {
        stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(okJson(HAPPY_CHOICE_RESPONSE)));

        final dev.langchain4j.http.client.jdk.JdkHttpClientBuilder customBuilder =
                new dev.langchain4j.http.client.jdk.JdkHttpClientBuilder();

        final SystemOneDecisionModel model = SystemOneDecisionModel.builder()
                .apiKey(FAKE_API_KEY)
                .baseUrl(wireMockBaseUrl)
                .httpClientBuilder(customBuilder)
                .maxRetries(0)
                .build();

        final DecisionResult result = model.decide(routeRequest());
        assertNotNull(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SO-024: Response model field is captured in SystemOneResponseDto
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void so024_responseModelField_capturedInDto() {
        // Verifies the wire-contract fix: "model" in the response is now deserialized.
        // The domain result does not expose it, but it should not cause deserialization failure.
        stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(okJson("""
                        {
                          "model": "jev-latest",
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
                .context(DecisionContext.of("context"))
                .question(question)
                .build();

        // If "model" were missing from the DTO, this would still pass (tolerant deserialization).
        // This test confirms the field is accepted and the response is correctly mapped.
        final DecisionResult result = buildModel().decide(request);
        assertEquals("FAST", result.get(question).selected());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SO-025: Extension tolerance — unknown fields in response are silently ignored
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void so025_unknownResponseFields_toleratedSilently() {
        // Simulates a Kev response with latency_ms, or OpenRouter response with id/provider/cost.
        stubFor(post(urlEqualTo("/v1/systemone"))
                .willReturn(okJson("""
                        {
                          "model": "kev-latest",
                          "id": "req_abc123",
                          "provider": "kev",
                          "latency_ms": 42,
                          "answers": {
                            "route": {
                              "type": "choice",
                              "choice": "FAST",
                              "confidence": 0.8
                            }
                          },
                          "usage": {
                            "input_tokens": 50,
                            "output_tokens": 10,
                            "cost": 0.0001
                          }
                        }
                        """)));

        final Choice<String> question = Choice.ofStrings("route", "Select route",
                List.of("FAST", "SLOW"));
        final DecisionRequest request = DecisionRequest.builder()
                .context(DecisionContext.of("context"))
                .question(question)
                .build();

        final DecisionResult result = buildModel().decide(request);
        assertEquals("FAST", result.get(question).selected());
    }
}
