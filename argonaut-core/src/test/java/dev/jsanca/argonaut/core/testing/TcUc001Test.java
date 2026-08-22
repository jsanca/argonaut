package dev.jsanca.argonaut.core.testing;

import dev.jsanca.argonaut.core.evidence.Evidence;
import dev.jsanca.argonaut.core.evidence.EvidenceKind;
import dev.jsanca.argonaut.core.experiment.ExperimentRequest;
import dev.jsanca.argonaut.core.experiment.ExperimentResult;
import dev.jsanca.argonaut.core.experiment.RunStatus;
import dev.jsanca.argonaut.core.metrics.ExecutionMetrics;
import dev.jsanca.argonaut.core.trace.ExecutionEvent;
import dev.jsanca.argonaut.core.trace.ExecutionEventType;
import dev.jsanca.argonaut.core.trace.ExecutionTrace;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TC-UC-001 test suite.
 *
 * <p>Positive: {@link ReferenceExecutor} produces a result that satisfies all contract assertions.
 * Negative: each assertion in {@link ControlledLocalEvidenceContract} fires correctly for a bad result.
 */
class TcUc001Test {

    // -------------------------------------------------------------------------
    // Positive — reference executor satisfies TC-UC-001
    // -------------------------------------------------------------------------

    @Test
    void referenceExecutor_satisfies_tc_uc_001() {
        assertDoesNotThrow(() ->
                ControlledLocalEvidenceContract.verify(new ReferenceExecutor()),
                "ReferenceExecutor must satisfy all TC-UC-001 assertions");
    }

    @Test
    void referenceExecutor_request_matches_contract_question() {
        ExperimentRequest request = ControlledLocalEvidenceContract.request();
        assertEquals(ControlledLocalEvidenceContract.RUN_ID, request.runId());
        assertEquals(ControlledLocalEvidenceContract.EXPERIMENT_QUESTION, request.question());
    }

    @Test
    void referenceExecutor_result_contains_exp001_evidence() {
        ExperimentResult result = new ReferenceExecutor().execute(ControlledLocalEvidenceContract.request());
        assertTrue(result.evidence().stream()
                .anyMatch(e -> "exp-001".equals(e.sourceId())),
                "reference result must contain exp-001 evidence");
    }

    @Test
    void referenceExecutor_result_has_non_blank_final_answer() {
        ExperimentResult result = new ReferenceExecutor().execute(ControlledLocalEvidenceContract.request());
        assertFalse(result.finalAnswer().isBlank(), "finalAnswer must not be blank");
    }

    @Test
    void referenceExecutor_result_has_consistent_metrics() {
        ExperimentResult result = new ReferenceExecutor().execute(ControlledLocalEvidenceContract.request());
        assertEquals(result.evidence().size(), result.metrics().evidenceCount(),
                "metrics.evidenceCount must match evidence list size");
        assertTrue(result.metrics().knowledgeSearches() >= 1, "must have at least one search");
        assertTrue(result.metrics().documentReads() >= 1, "must have at least one read");
        assertTrue(result.metrics().durationMs() >= 0);
    }

    // -------------------------------------------------------------------------
    // SC1 — status not COMPLETED
    // -------------------------------------------------------------------------

    @Test
    void validator_rejects_failed_status() {
        ExperimentResult bad = hollowCompleted();
        ExperimentResult failed = new ExperimentResult(
                bad.runId(), bad.frameworkId(), RunStatus.FAILED,
                null, List.of(), bad.trace(), bad.metrics(), List.of());
        AssertionError ex = assertThrows(AssertionError.class,
                () -> ControlledLocalEvidenceContract.assertSatisfied(failed));
        assertTrue(ex.getMessage().contains("SC1"), ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // SC2 — blank finalAnswer
    // -------------------------------------------------------------------------

    @Test
    void completed_factory_rejects_blank_final_answer() {
        assertThrows(IllegalArgumentException.class, () ->
                ExperimentResult.completed("r", "f", "   ",
                        List.of(), ExecutionTrace.empty(), ExecutionMetrics.empty()));
    }

    @Test
    void completed_factory_rejects_null_final_answer() {
        assertThrows(IllegalArgumentException.class, () ->
                ExperimentResult.completed("r", "f", null,
                        List.of(), ExecutionTrace.empty(), ExecutionMetrics.empty()));
    }

    @Test
    void validator_rejects_blank_final_answer_via_record_constructor() {
        // The record constructor does not validate finalAnswer; the validator does.
        ExperimentResult bad = new ExperimentResult(
                "r", "f", RunStatus.COMPLETED, "  ",
                List.of(), minimalTrace(), metricsFor(List.of()), List.of());
        AssertionError ex = assertThrows(AssertionError.class,
                () -> ControlledLocalEvidenceContract.assertSatisfied(bad));
        assertTrue(ex.getMessage().contains("SC2"), ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // SC3 — exp-001 not in evidence
    // -------------------------------------------------------------------------

    @Test
    void validator_rejects_missing_exp001_evidence() {
        Evidence wrongEvidence = makeEvidence("ev1", "obs-001", "obs-001");
        ExperimentResult bad = completedWith(List.of(wrongEvidence), minimalTrace(), 1);
        AssertionError ex = assertThrows(AssertionError.class,
                () -> ControlledLocalEvidenceContract.assertSatisfied(bad));
        assertTrue(ex.getMessage().contains("SC3"), ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // SC4 — evidence sourceId not traceable in trace metadata (anti-gaming)
    // -------------------------------------------------------------------------

    @Test
    void validator_rejects_hollow_hard_coded_result() {
        // All required event types are present, but no DOCUMENT_READ_COMPLETED or
        // EVIDENCE_RETRIEVED event carries sourceId metadata. Evidence is hard-coded.
        Evidence evidence = makeEvidence("ev1", "exp-001", "exp-001");
        ExecutionTrace hollowTrace = buildTrace(
                ev(ExecutionEventType.RUN_STARTED),
                ev(ExecutionEventType.KNOWLEDGE_SEARCH_STARTED),
                ev(ExecutionEventType.KNOWLEDGE_SEARCH_COMPLETED),
                ev(ExecutionEventType.DOCUMENT_READ_STARTED),
                evWithMeta(ExecutionEventType.DOCUMENT_READ_COMPLETED, Map.of()), // no sourceId!
                evWithMeta(ExecutionEventType.EVIDENCE_RETRIEVED, Map.of()),       // no sourceId!
                ev(ExecutionEventType.EVIDENCE_SELECTED),
                ev(ExecutionEventType.ANSWER_SYNTHESIZED),
                ev(ExecutionEventType.RUN_COMPLETED)
        );
        ExperimentResult hollow = completedWith(List.of(evidence), hollowTrace, 1);
        AssertionError ex = assertThrows(AssertionError.class,
                () -> ControlledLocalEvidenceContract.assertSatisfied(hollow));
        assertTrue(ex.getMessage().contains("SC4"), ex.getMessage());
    }

    @Test
    void validator_rejects_evidence_sourceId_not_in_any_trace_event() {
        // DOCUMENT_READ_COMPLETED carries "obs-001" but evidence claims "exp-001".
        Evidence evidence = makeEvidence("ev1", "exp-001", "exp-001");
        ExecutionTrace trace = buildTrace(
                ev(ExecutionEventType.RUN_STARTED),
                ev(ExecutionEventType.KNOWLEDGE_SEARCH_STARTED),
                ev(ExecutionEventType.KNOWLEDGE_SEARCH_COMPLETED),
                ev(ExecutionEventType.DOCUMENT_READ_STARTED),
                evWithMeta(ExecutionEventType.DOCUMENT_READ_COMPLETED,
                        Map.of(TraceEventMetadata.SOURCE_ID, "obs-001")), // wrong sourceId
                evWithMeta(ExecutionEventType.EVIDENCE_RETRIEVED,
                        Map.of(TraceEventMetadata.SOURCE_ID, "obs-001")), // wrong sourceId
                ev(ExecutionEventType.EVIDENCE_SELECTED),
                ev(ExecutionEventType.ANSWER_SYNTHESIZED),
                ev(ExecutionEventType.RUN_COMPLETED)
        );
        ExperimentResult bad = completedWith(List.of(evidence), trace, 1);
        AssertionError ex = assertThrows(AssertionError.class,
                () -> ControlledLocalEvidenceContract.assertSatisfied(bad));
        assertTrue(ex.getMessage().contains("SC4"), ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // SC5 — missing required event type
    // -------------------------------------------------------------------------

    @Test
    void validator_rejects_missing_run_started() {
        ExecutionTrace trace = buildTrace(
                ev(ExecutionEventType.KNOWLEDGE_SEARCH_STARTED),
                ev(ExecutionEventType.KNOWLEDGE_SEARCH_COMPLETED),
                ev(ExecutionEventType.DOCUMENT_READ_STARTED),
                evWithMeta(ExecutionEventType.DOCUMENT_READ_COMPLETED, expSourceIdMeta()),
                evWithMeta(ExecutionEventType.EVIDENCE_RETRIEVED, expSourceIdMeta()),
                ev(ExecutionEventType.EVIDENCE_SELECTED),
                ev(ExecutionEventType.ANSWER_SYNTHESIZED),
                ev(ExecutionEventType.RUN_COMPLETED)
        );
        ExperimentResult bad = completedWith(expEvidence(), trace, 1);
        assertThrowsWithMessage(bad, "SC5");
    }

    @Test
    void validator_rejects_missing_knowledge_search_started() {
        assertThrowsWithMessage(completedWithoutEvent(ExecutionEventType.KNOWLEDGE_SEARCH_STARTED), "SC5");
    }

    @Test
    void validator_rejects_missing_document_read_completed() {
        assertThrowsWithMessage(completedWithoutEvent(ExecutionEventType.DOCUMENT_READ_COMPLETED), "SC5");
    }

    @Test
    void validator_rejects_missing_evidence_selected() {
        assertThrowsWithMessage(completedWithoutEvent(ExecutionEventType.EVIDENCE_SELECTED), "SC5");
    }

    @Test
    void validator_rejects_missing_answer_synthesized() {
        assertThrowsWithMessage(completedWithoutEvent(ExecutionEventType.ANSWER_SYNTHESIZED), "SC5");
    }

    // -------------------------------------------------------------------------
    // SC6 — trace ordering violations
    // -------------------------------------------------------------------------

    @Test
    void validator_rejects_run_completed_not_last() {
        ExecutionTrace trace = buildTrace(
                ev(ExecutionEventType.RUN_STARTED),
                ev(ExecutionEventType.KNOWLEDGE_SEARCH_STARTED),
                ev(ExecutionEventType.KNOWLEDGE_SEARCH_COMPLETED),
                ev(ExecutionEventType.DOCUMENT_READ_STARTED),
                evWithMeta(ExecutionEventType.DOCUMENT_READ_COMPLETED, expSourceIdMeta()),
                evWithMeta(ExecutionEventType.EVIDENCE_RETRIEVED, expSourceIdMeta()),
                ev(ExecutionEventType.EVIDENCE_SELECTED),
                ev(ExecutionEventType.ANSWER_SYNTHESIZED),
                ev(ExecutionEventType.RUN_COMPLETED),
                ev(ExecutionEventType.STEP_STARTED)  // extra event after RUN_COMPLETED
        );
        ExperimentResult bad = completedWith(expEvidence(), trace, 1);
        AssertionError ex = assertThrows(AssertionError.class,
                () -> ControlledLocalEvidenceContract.assertSatisfied(bad));
        assertTrue(ex.getMessage().contains("SC6"), ex.getMessage());
    }

    @Test
    void validator_rejects_answer_synthesized_before_evidence_selected() {
        ExecutionTrace trace = buildTrace(
                ev(ExecutionEventType.RUN_STARTED),
                ev(ExecutionEventType.KNOWLEDGE_SEARCH_STARTED),
                ev(ExecutionEventType.KNOWLEDGE_SEARCH_COMPLETED),
                ev(ExecutionEventType.DOCUMENT_READ_STARTED),
                evWithMeta(ExecutionEventType.DOCUMENT_READ_COMPLETED, expSourceIdMeta()),
                evWithMeta(ExecutionEventType.EVIDENCE_RETRIEVED, expSourceIdMeta()),
                ev(ExecutionEventType.ANSWER_SYNTHESIZED),  // too early!
                ev(ExecutionEventType.EVIDENCE_SELECTED),
                ev(ExecutionEventType.RUN_COMPLETED)
        );
        ExperimentResult bad = completedWith(expEvidence(), trace, 1);
        AssertionError ex = assertThrows(AssertionError.class,
                () -> ControlledLocalEvidenceContract.assertSatisfied(bad));
        assertTrue(ex.getMessage().contains("SC6"), ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // SC7 — STARTED/COMPLETED pairing
    // (No assertion currently fails with >= check; more failures come from missing event types above.
    //  This test verifies the count check fires if searches exceed completions.)
    // Already covered by SC5 tests since missing COMPLETED implicitly fails SC5.

    // -------------------------------------------------------------------------
    // SC8 — metrics consistency
    // -------------------------------------------------------------------------

    @Test
    void validator_rejects_metrics_evidence_count_mismatch() {
        // evidence list has 1 item but metrics say 2
        ExecutionMetrics badMetrics = new ExecutionMetrics(10, 0, 0, 1, 1, 2, 0); // evidenceCount=2 but list has 1
        ExperimentResult bad = new ExperimentResult(
                "r", "f", RunStatus.COMPLETED, "answer",
                expEvidence(), minimalTrace(), badMetrics, List.of());
        AssertionError ex = assertThrows(AssertionError.class,
                () -> ControlledLocalEvidenceContract.assertSatisfied(bad));
        assertTrue(ex.getMessage().contains("SC8"), ex.getMessage());
    }

    @Test
    void validator_rejects_zero_knowledge_searches() {
        ExecutionMetrics badMetrics = new ExecutionMetrics(10, 0, 0, 0, 1, 1, 0); // knowledgeSearches=0
        ExperimentResult bad = new ExperimentResult(
                "r", "f", RunStatus.COMPLETED, "answer",
                expEvidence(), minimalTrace(), badMetrics, List.of());
        AssertionError ex = assertThrows(AssertionError.class,
                () -> ControlledLocalEvidenceContract.assertSatisfied(bad));
        assertTrue(ex.getMessage().contains("SC8"), ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // Helpers — trace and result builders
    // -------------------------------------------------------------------------

    private static ExecutionTrace minimalTrace() {
        return buildTrace(
                ev(ExecutionEventType.RUN_STARTED),
                ev(ExecutionEventType.KNOWLEDGE_SEARCH_STARTED),
                ev(ExecutionEventType.KNOWLEDGE_SEARCH_COMPLETED),
                ev(ExecutionEventType.DOCUMENT_READ_STARTED),
                evWithMeta(ExecutionEventType.DOCUMENT_READ_COMPLETED, expSourceIdMeta()),
                evWithMeta(ExecutionEventType.EVIDENCE_RETRIEVED, expSourceIdMeta()),
                ev(ExecutionEventType.EVIDENCE_SELECTED),
                ev(ExecutionEventType.ANSWER_SYNTHESIZED),
                ev(ExecutionEventType.RUN_COMPLETED)
        );
    }

    private static List<Evidence> expEvidence() {
        return List.of(makeEvidence("ev-exp", "exp-001", "exp-001"));
    }

    private static ExperimentResult hollowCompleted() {
        return completedWith(expEvidence(), minimalTrace(), 1);
    }

    private static ExperimentResult completedWith(List<Evidence> evidence, ExecutionTrace trace, int searches) {
        ExecutionMetrics metrics = metricsFor(evidence, searches);
        return new ExperimentResult(
                "r", "f", RunStatus.COMPLETED, "Controlled evidence reduces noise.",
                evidence, trace, metrics, List.of());
    }

    private static ExecutionMetrics metricsFor(List<Evidence> evidence) {
        return metricsFor(evidence, 1);
    }

    private static ExecutionMetrics metricsFor(List<Evidence> evidence, int searches) {
        return new ExecutionMetrics(10, 0, 0, searches, 1, evidence.size(), 0);
    }

    private static ExperimentResult completedWithoutEvent(ExecutionEventType missing) {
        List<ExecutionEvent> events = buildEventList(
                ev(ExecutionEventType.RUN_STARTED),
                ev(ExecutionEventType.KNOWLEDGE_SEARCH_STARTED),
                ev(ExecutionEventType.KNOWLEDGE_SEARCH_COMPLETED),
                ev(ExecutionEventType.DOCUMENT_READ_STARTED),
                evWithMeta(ExecutionEventType.DOCUMENT_READ_COMPLETED, expSourceIdMeta()),
                evWithMeta(ExecutionEventType.EVIDENCE_RETRIEVED, expSourceIdMeta()),
                ev(ExecutionEventType.EVIDENCE_SELECTED),
                ev(ExecutionEventType.ANSWER_SYNTHESIZED),
                ev(ExecutionEventType.RUN_COMPLETED)
        );
        events.removeIf(e -> e.type() == missing);
        return completedWith(expEvidence(), new ExecutionTrace(events), 1);
    }

    private static void assertThrowsWithMessage(ExperimentResult bad, String msgFragment) {
        AssertionError ex = assertThrows(AssertionError.class,
                () -> ControlledLocalEvidenceContract.assertSatisfied(bad));
        assertTrue(ex.getMessage().contains(msgFragment),
                "Expected message to contain '" + msgFragment + "' but got: " + ex.getMessage());
    }

    private static Map<String, String> expSourceIdMeta() {
        return Map.of(TraceEventMetadata.SOURCE_ID, "exp-001");
    }

    private static Evidence makeEvidence(String evidenceId, String sourceId, String title) {
        return new Evidence(evidenceId, EvidenceKind.SUPPORTING, sourceId, title, "excerpt", 0.8, "for answer");
    }

    private static ExecutionEvent ev(ExecutionEventType type) {
        return new ExecutionEvent(UUID.randomUUID().toString(), Instant.now(), type,
                "test", type.name().toLowerCase(), type.name(), Map.of(), null);
    }

    private static ExecutionEvent evWithMeta(ExecutionEventType type, Map<String, String> metadata) {
        return new ExecutionEvent(UUID.randomUUID().toString(), Instant.now(), type,
                "test", type.name().toLowerCase(), type.name(), metadata, null);
    }

    private static ExecutionTrace buildTrace(ExecutionEvent... events) {
        return new ExecutionTrace(List.of(events));
    }

    private static java.util.ArrayList<ExecutionEvent> buildEventList(ExecutionEvent... events) {
        return new java.util.ArrayList<>(List.of(events));
    }

}
