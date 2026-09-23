package dev.jsanca.argonaut.decision;

import dev.jsanca.argonaut.decision.probability.OutcomeProbability;
import dev.jsanca.argonaut.decision.probability.Probability;
import dev.jsanca.argonaut.decision.probability.ProbabilityDistribution;
import dev.jsanca.argonaut.decision.question.Choice;
import dev.jsanca.argonaut.decision.question.Noul;
import dev.jsanca.argonaut.decision.question.Score;
import dev.jsanca.argonaut.decision.result.ChoiceResult;
import dev.jsanca.argonaut.decision.result.NoulResult;
import dev.jsanca.argonaut.decision.result.ScoreResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD contract for the argonaut-decision domain skeleton.
 *
 * <p>Each test maps to a scenario defined in ARG-DECISION-EP-001 §24.</p>
 */
class DecisionModelContractTest {

    enum Route { FAST, NORMAL, DEEP }
    enum Complexity { LOW, MEDIUM, HIGH }

    // ── TC-DM-001: Typed Choice ──────────────────────────────────────────────

    @Test
    void tc_dm_001_typedChoice_enumCandidates_returnsTypedResult() {
        var route = Choice.ofEnum("route", "Select execution route", Route.class);

        var model = FakeDecisionModel.builder()
                .preset(route, ChoiceResult.of(Route.FAST))
                .build();

        DecisionResult result = model.decide(
                DecisionRequest.builder()
                        .context(DecisionContext.of("context"))
                        .question(route)
                        .build());

        ChoiceResult<Route> routeResult = result.get(route);
        assertNotNull(routeResult);
        assertEquals(Route.FAST, routeResult.selected());
    }

    // ── TC-DM-002: String Choice ─────────────────────────────────────────────

    @Test
    void tc_dm_002_stringChoice_returnsSelectedString() {
        var category = Choice.ofStrings("category", "Classify the request",
                List.of("billing", "support", "sales"));

        var model = FakeDecisionModel.builder()
                .preset(category, ChoiceResult.of("billing"))
                .build();

        DecisionResult result = model.decide(
                DecisionRequest.builder()
                        .context(DecisionContext.of("help with my invoice"))
                        .question(category)
                        .build());

        ChoiceResult<String> categoryResult = result.get(category);
        assertEquals("billing", categoryResult.selected());
    }

    // ── TC-DM-003: Ordered Score ─────────────────────────────────────────────

    @Test
    void tc_dm_003_orderedScore_scalePreservedInResult() {
        var scale = List.of(Complexity.LOW, Complexity.MEDIUM, Complexity.HIGH);
        var complexity = Score.of("complexity", "Evaluate architectural complexity", scale);

        var model = FakeDecisionModel.builder()
                .preset(complexity, ScoreResult.of(Complexity.HIGH, scale))
                .build();

        DecisionResult result = model.decide(
                DecisionRequest.builder()
                        .context(DecisionContext.of("major API overhaul"))
                        .question(complexity)
                        .build());

        ScoreResult<Complexity> complexityResult = result.get(complexity);
        assertEquals(Complexity.HIGH, complexityResult.selected());
        assertEquals(List.of(Complexity.LOW, Complexity.MEDIUM, Complexity.HIGH),
                complexityResult.scale());
        // Order LOW < MEDIUM < HIGH is observable via index
        assertTrue(complexityResult.scale().indexOf(Complexity.LOW)
                < complexityResult.scale().indexOf(Complexity.MEDIUM));
        assertTrue(complexityResult.scale().indexOf(Complexity.MEDIUM)
                < complexityResult.scale().indexOf(Complexity.HIGH));
    }

    // ── TC-DM-004: Probabilistic Noul ────────────────────────────────────────

    @Test
    void tc_dm_004_noulResult_probabilityPreserved_notConvertedToBoolean() {
        var review = Noul.of("needs-review", "This change requires architectural review");

        var model = FakeDecisionModel.builder()
                .preset(review, NoulResult.of(0.73))
                .build();

        DecisionResult result = model.decide(
                DecisionRequest.builder()
                        .context(DecisionContext.of("significant API redesign"))
                        .question(review)
                        .build());

        NoulResult reviewResult = result.get(review);
        assertEquals(0.73, reviewResult.probabilityTrue().value(), 1e-10);
        // NoulResult exposes only probabilityTrue — no selected(), no boolean conversion
        // Compile-time verification: this class has no such method
    }

    // ── TC-DM-005: Heterogeneous Batch ───────────────────────────────────────

    @Test
    void tc_dm_005_heterogeneousBatch_allResultsRecoverable() {
        var scale = List.of(Complexity.LOW, Complexity.MEDIUM, Complexity.HIGH);
        var route = Choice.ofEnum("route", "Select route", Route.class);
        var complexity = Score.of("complexity", "Evaluate complexity", scale);
        var review = Noul.of("needs-review", "Requires review");

        var model = FakeDecisionModel.builder()
                .preset(route, ChoiceResult.of(Route.DEEP))
                .preset(complexity, ScoreResult.of(Complexity.MEDIUM, scale))
                .preset(review, NoulResult.of(0.45))
                .build();

        var request = DecisionRequest.builder()
                .context(DecisionContext.of("shared context for all questions"))
                .question(route)
                .question(complexity)
                .question(review)
                .build();

        assertEquals(3, request.questions().size());

        DecisionResult result = model.decide(request);

        ChoiceResult<Route> routeResult = result.get(route);
        ScoreResult<Complexity> complexityResult = result.get(complexity);
        NoulResult reviewResult = result.get(review);

        assertEquals(Route.DEEP, routeResult.selected());
        assertEquals(Complexity.MEDIUM, complexityResult.selected());
        assertEquals(0.45, reviewResult.probabilityTrue().value(), 1e-10);
    }

    // ── TC-DM-006: Typed Retrieval — no consumer cast ─────────────────────────

    @Test
    void tc_dm_006_typedRetrieval_requiresNoConsumerCast() {
        var route = Choice.ofEnum("route", "Select route", Route.class);

        var model = FakeDecisionModel.builder()
                .preset(route, ChoiceResult.of(Route.NORMAL))
                .build();

        DecisionResult result = model.decide(
                DecisionRequest.builder()
                        .context(DecisionContext.of("ctx"))
                        .question(route)
                        .build());

        // Compile-time proof: this assignment requires no cast.
        // If this file compiles without @SuppressWarnings, TC-DM-006 is satisfied.
        ChoiceResult<Route> routeResult = result.get(route);
        assertEquals(Route.NORMAL, routeResult.selected());
    }

    // ── TC-DM-007: Probability Absence ───────────────────────────────────────

    @Test
    void tc_dm_007_probabilityAbsence_distinctFromZero() {
        var route = Choice.ofEnum("route", "Select route", Route.class);

        // null confidence = no confidence information available
        ChoiceResult<Route> withAbsentConfidence = ChoiceResult.of(Route.FAST);
        assertNull(withAbsentConfidence.confidence());

        // 0.0 confidence = present but zero
        ChoiceResult<Route> withZeroConfidence = ChoiceResult.of(Route.FAST, 0.0);
        assertNotNull(withZeroConfidence.confidence());
        assertEquals(0.0, withZeroConfidence.confidence(), 1e-10);

        // They are provably distinct
        assertNotEquals(withAbsentConfidence.confidence(), withZeroConfidence.confidence());

        // Probability(0.0) is a valid, constructable value — not a sentinel
        assertDoesNotThrow(() -> Probability.of(0.0));
        assertEquals(0.0, Probability.of(0.0).value(), 1e-10);

        // NoulResult(0.0) means P(true) = 0.0, NOT "probability unavailable"
        assertDoesNotThrow(() -> NoulResult.of(0.0));
        assertEquals(0.0, NoulResult.of(0.0).probabilityTrue().value(), 1e-10);
    }

    // ── TC-DM-008: Duplicate Question Identity ────────────────────────────────

    @Test
    void tc_dm_008_duplicateQuestionId_rejectedAtBuildTime() {
        var route1 = Choice.ofEnum("route", "First route question", Route.class);
        var route2 = Choice.ofEnum("route", "Second route question with same id", Route.class);

        DecisionRequest.Builder builder = DecisionRequest.builder()
                .context(DecisionContext.of("ctx"))
                .question(route1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> builder.question(route2));
        assertTrue(ex.getMessage().contains("route"));
    }

    // ── TC-DM-009: Invalid Probability ───────────────────────────────────────

    @Test
    void tc_dm_009_invalidProbability_rejected() {
        assertThrows(IllegalArgumentException.class, () -> Probability.of(-0.001));
        assertThrows(IllegalArgumentException.class, () -> Probability.of(1.001));
        assertThrows(IllegalArgumentException.class, () -> Probability.of(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> Probability.of(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> Probability.of(Double.NEGATIVE_INFINITY));

        // Valid boundary values must not throw
        assertDoesNotThrow(() -> Probability.of(0.0));
        assertDoesNotThrow(() -> Probability.of(1.0));
        assertDoesNotThrow(() -> Probability.of(0.5));
    }

    // ── TC-DM-010: Provider Independence ─────────────────────────────────────

    @Test
    void tc_dm_010_providerIndependence_fakeModelRequiresNoExternalDependency() {
        // FakeDecisionModel implements DecisionModel with no Jev, ChatModel, or
        // network dependency. This test compiling and passing proves the point.
        DecisionModel model = FakeDecisionModel.builder()
                .capability(DecisionCapability.CHOICE)
                .capability(DecisionCapability.MULTI_QUESTION)
                .preset(Choice.ofEnum("route", "Select route", Route.class),
                        ChoiceResult.of(Route.FAST))
                .build();

        assertTrue(model.capabilities().contains(DecisionCapability.CHOICE));
        assertTrue(model.capabilities().contains(DecisionCapability.MULTI_QUESTION));

        var route = Choice.ofEnum("route", "Select route", Route.class);
        DecisionResult result = model.decide(
                DecisionRequest.builder()
                        .context(DecisionContext.of("ctx"))
                        .question(route)
                        .build());

        assertNotNull(result);
        assertEquals(Route.FAST, result.get(route).selected());
    }

    // ── Supplemental: distribution carried in ChoiceResult ───────────────────

    @Test
    void choiceResult_withDistribution_distributionPreserved() {
        var distribution = ProbabilityDistribution.of(List.of(
                OutcomeProbability.of(Route.FAST, 0.7),
                OutcomeProbability.of(Route.NORMAL, 0.2),
                OutcomeProbability.of(Route.DEEP, 0.1)));

        ChoiceResult<Route> result = ChoiceResult.withDistribution(Route.FAST, distribution);

        assertEquals(Route.FAST, result.selected());
        assertNotNull(result.distribution());
        assertEquals(3, result.distribution().entries().size());
        assertEquals(0.7, result.distribution().entries().get(0).probability().value(), 1e-10);
    }
}
