package dev.jsanca.argonaut.decision.jev;

import dev.jsanca.argonaut.decision.DecisionContext;
import dev.jsanca.argonaut.decision.DecisionRequest;
import dev.jsanca.argonaut.decision.DecisionResult;
import dev.jsanca.argonaut.decision.question.Choice;
import dev.jsanca.argonaut.decision.question.Noul;
import dev.jsanca.argonaut.decision.question.Score;
import dev.jsanca.argonaut.decision.result.ChoiceResult;
import dev.jsanca.argonaut.decision.result.NoulResult;
import dev.jsanca.argonaut.decision.result.ScoreResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Live integration test against the real Jev API.
 *
 * <p>Skipped unless {@code JEV_API_KEY} environment variable is set.</p>
 */
class JevDecisionModelLiveIT {

    @Test
    void liveIntegration_structuralProperties() {
        final String apiKey = System.getenv("JEV_API_KEY");
        assumeTrue(apiKey != null && !apiKey.isBlank(), "JEV_API_KEY not set, skipping live test");

        final JevDecisionModel model = JevDecisionModel.builder()
                .apiKey(apiKey)
                .build();

        final Choice<String> routeQ = Choice.ofStrings("route",
                "Select the execution route based on the context",
                List.of("FAST", "SLOW"));
        final Score<String> severityQ = Score.of("severity",
                "Evaluate the severity of the situation",
                List.of("LOW", "MEDIUM", "HIGH"));
        final Noul reviewQ = Noul.of("needs_review",
                "This situation requires immediate human review");

        final DecisionRequest request = DecisionRequest.builder()
                .context(DecisionContext.of(
                        "A critical production incident has been detected with cascading failures "
                        + "affecting 30% of users. Response time has increased 10x."))
                .question(routeQ)
                .question(severityQ)
                .question(reviewQ)
                .build();

        final DecisionResult result = model.decide(request);

        // Structural assertions — we don't assert specific values since those depend on the model
        final ChoiceResult<String> routeResult = result.get(routeQ);
        assertNotNull(routeResult, "route result must not be null");
        assertNotNull(routeResult.selected(), "selected route must not be null");
        assertTrue(List.of("FAST", "SLOW").contains(routeResult.selected()),
                "selected route must be one of the candidates");

        final ScoreResult<String> severityResult = result.get(severityQ);
        assertNotNull(severityResult, "severity result must not be null");
        assertNotNull(severityResult.selected(), "selected severity must not be null");
        assertTrue(List.of("LOW", "MEDIUM", "HIGH").contains(severityResult.selected()),
                "selected severity must be one of the scale entries");

        final NoulResult reviewResult = result.get(reviewQ);
        assertNotNull(reviewResult, "needs_review result must not be null");
        assertNotNull(reviewResult.probabilityTrue(), "probabilityTrue must not be null");
        final double pTrue = reviewResult.probabilityTrue().value();
        assertTrue(pTrue >= 0.0 && pTrue <= 1.0,
                "probabilityTrue must be in [0.0, 1.0], got: " + pTrue);
    }
}
