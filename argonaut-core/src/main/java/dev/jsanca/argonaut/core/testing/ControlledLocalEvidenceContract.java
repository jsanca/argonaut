package dev.jsanca.argonaut.core.testing;

import dev.jsanca.argonaut.core.evidence.Evidence;
import dev.jsanca.argonaut.core.experiment.ExperimentRequest;
import dev.jsanca.argonaut.core.experiment.ExperimentResult;
import dev.jsanca.argonaut.core.experiment.RunStatus;
import dev.jsanca.argonaut.core.trace.ExecutionEvent;
import dev.jsanca.argonaut.core.trace.ExecutionEventType;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TC-UC-001 — Controlled Local Evidence RAG use-case contract.
 *
 * <p>This class provides the canonical test request and all required assertions for the first
 * Argonaut use case. Every framework implementation must produce an {@link ExperimentResult}
 * that satisfies all nine assertions below.
 *
 * <h3>Usage by framework test classes</h3>
 * <pre>
 * // Option 1 — verify via executor lambda
 * ControlledLocalEvidenceContract.verify(request -> myFramework.run(request));
 *
 * // Option 2 — assert a previously obtained result
 * ExperimentResult result = myFramework.run(ControlledLocalEvidenceContract.request());
 * ControlledLocalEvidenceContract.assertSatisfied(result);
 * </pre>
 *
 * <h3>Anti-gaming</h3>
 * <p>Assertion 4 (evidence traceability) requires that every {@link Evidence#sourceId()} in
 * the result appears in the metadata of at least one {@code DOCUMENT_READ_COMPLETED} or
 * {@code EVIDENCE_RETRIEVED} trace event (keyed by {@link TraceEventMetadata#SOURCE_ID}).
 * A framework cannot satisfy this assertion by hard-coding evidence without performing
 * real {@code KnowledgeRepository} calls.
 */
public final class ControlledLocalEvidenceContract {

    /** The run ID used for TC-UC-001 experiment requests. */
    public static final String RUN_ID = "tc-uc-001";

    /** The primary source document that must appear in the evidence list. */
    public static final String PRIMARY_EVIDENCE_SOURCE_ID = "exp-001";

    /**
     * The canonical experiment question for the Controlled Local Evidence RAG use case.
     */
    public static final String EXPERIMENT_QUESTION =
            "When comparing agentic Java frameworks, why should Argonaut use controlled local " +
            "evidence before introducing web search, vector databases, or external observability tools?";

    private ControlledLocalEvidenceContract() {}

    /**
     * Returns the canonical TC-UC-001 experiment request.
     * Framework test classes should submit this request to their implementation under test.
     */
    public static ExperimentRequest request() {
        return ExperimentRequest.of(RUN_ID, EXPERIMENT_QUESTION);
    }

    /**
     * Convenience method: executes the request using the given executor and asserts all
     * TC-UC-001 success criteria on the result.
     *
     * @throws AssertionError if any criterion is not satisfied
     */
    public static void verify(ExperimentExecutor executor) {
        assertSatisfied(executor.execute(request()));
    }

    /**
     * Asserts that {@code result} satisfies all nine TC-UC-001 success criteria.
     *
     * <p>Throws {@link AssertionError} with a clear message if any criterion fails.
     * All criteria are checked in order; the first failure stops the assertion chain.
     *
     * @throws AssertionError if any criterion is not satisfied
     */
    public static void assertSatisfied(ExperimentResult result) {
        assertNotNull(result, "ExperimentResult must not be null");

        // SC1 — status
        assertEqual(RunStatus.COMPLETED, result.status(),
                "SC1: result.status must be COMPLETED");

        // SC2 — final answer present (also enforced by ExperimentResult.completed factory)
        assertNotBlank(result.finalAnswer(),
                "SC2: finalAnswer must not be blank for a COMPLETED result");

        List<ExecutionEvent> events = result.trace().events();
        assertFalse(events.isEmpty(), "SC5: trace must not be empty");

        // SC5 — required event types
        assertEventPresent(events, ExecutionEventType.RUN_STARTED,
                "SC5: trace must contain RUN_STARTED");
        assertEventPresent(events, ExecutionEventType.KNOWLEDGE_SEARCH_STARTED,
                "SC5: trace must contain KNOWLEDGE_SEARCH_STARTED");
        assertEventPresent(events, ExecutionEventType.KNOWLEDGE_SEARCH_COMPLETED,
                "SC5: trace must contain KNOWLEDGE_SEARCH_COMPLETED");
        assertEventPresent(events, ExecutionEventType.DOCUMENT_READ_STARTED,
                "SC5: trace must contain DOCUMENT_READ_STARTED");
        assertEventPresent(events, ExecutionEventType.DOCUMENT_READ_COMPLETED,
                "SC5: trace must contain DOCUMENT_READ_COMPLETED");
        assertEventPresent(events, ExecutionEventType.EVIDENCE_RETRIEVED,
                "SC5: trace must contain EVIDENCE_RETRIEVED");
        assertEventPresent(events, ExecutionEventType.EVIDENCE_SELECTED,
                "SC5: trace must contain EVIDENCE_SELECTED");
        assertEventPresent(events, ExecutionEventType.ANSWER_SYNTHESIZED,
                "SC5: trace must contain ANSWER_SYNTHESIZED");
        assertEventPresent(events, ExecutionEventType.RUN_COMPLETED,
                "SC5: trace must contain RUN_COMPLETED");

        // SC6 — trace ordering
        assertEqual(ExecutionEventType.RUN_STARTED, events.getFirst().type(),
                "SC6: first trace event must be RUN_STARTED");
        assertEqual(ExecutionEventType.RUN_COMPLETED, events.getLast().type(),
                "SC6: last trace event must be RUN_COMPLETED");
        assertBefore(events, ExecutionEventType.KNOWLEDGE_SEARCH_STARTED,
                ExecutionEventType.KNOWLEDGE_SEARCH_COMPLETED,
                "SC6: KNOWLEDGE_SEARCH_STARTED must precede KNOWLEDGE_SEARCH_COMPLETED");
        assertBefore(events, ExecutionEventType.DOCUMENT_READ_STARTED,
                ExecutionEventType.DOCUMENT_READ_COMPLETED,
                "SC6: DOCUMENT_READ_STARTED must precede DOCUMENT_READ_COMPLETED");
        assertBefore(events, ExecutionEventType.EVIDENCE_RETRIEVED,
                ExecutionEventType.EVIDENCE_SELECTED,
                "SC6: EVIDENCE_RETRIEVED must precede EVIDENCE_SELECTED");
        assertBefore(events, ExecutionEventType.EVIDENCE_SELECTED,
                ExecutionEventType.ANSWER_SYNTHESIZED,
                "SC6: EVIDENCE_SELECTED must precede ANSWER_SYNTHESIZED");

        // SC7 — STARTED/COMPLETED pairing
        int searchStarted = countEvents(events, ExecutionEventType.KNOWLEDGE_SEARCH_STARTED);
        int searchCompleted = countEvents(events, ExecutionEventType.KNOWLEDGE_SEARCH_COMPLETED);
        assertTrue(searchCompleted >= searchStarted,
                "SC7: KNOWLEDGE_SEARCH_COMPLETED count (" + searchCompleted +
                ") must be >= KNOWLEDGE_SEARCH_STARTED count (" + searchStarted + ")");

        int readStarted = countEvents(events, ExecutionEventType.DOCUMENT_READ_STARTED);
        int readCompleted = countEvents(events, ExecutionEventType.DOCUMENT_READ_COMPLETED);
        assertTrue(readCompleted >= readStarted,
                "SC7: DOCUMENT_READ_COMPLETED count (" + readCompleted +
                ") must be >= DOCUMENT_READ_STARTED count (" + readStarted + ")");

        // SC8 — metrics consistency
        assertTrue(result.metrics().knowledgeSearches() >= 1,
                "SC8: metrics.knowledgeSearches must be >= 1");
        assertTrue(result.metrics().documentReads() >= 1,
                "SC8: metrics.documentReads must be >= 1");
        assertEqual(result.evidence().size(), result.metrics().evidenceCount(),
                "SC8: metrics.evidenceCount must equal result.evidence().size()");
        assertEqual(result.errors().size(), result.metrics().errors(),
                "SC8: metrics.errors must equal result.errors().size()");
        assertTrue(result.metrics().durationMs() >= 0,
                "SC8: metrics.durationMs must be >= 0");

        // SC9 — no errors for a successful baseline run
        assertTrue(result.errors().isEmpty(),
                "SC9: errors must be empty for a successful baseline run, got: " + result.errors());

        // SC3 — evidence includes exp-001
        boolean hasExpEvidence = result.evidence().stream()
                .anyMatch(e -> PRIMARY_EVIDENCE_SOURCE_ID.equals(e.sourceId()));
        assertTrue(hasExpEvidence,
                "SC3: evidence must include at least one item with sourceId=" + PRIMARY_EVIDENCE_SOURCE_ID +
                "; actual sourceIds: " + result.evidence().stream().map(Evidence::sourceId).toList());

        // SC4 — evidence traceability (anti-gaming)
        assertEvidenceTraceable(result, events);
    }

    // -------------------------------------------------------------------------
    // Internal assertion helpers
    // -------------------------------------------------------------------------

    private static void assertEvidenceTraceable(ExperimentResult result, List<ExecutionEvent> events) {
        // Collect sourceIds declared in DOCUMENT_READ_COMPLETED or EVIDENCE_RETRIEVED events
        Set<String> tracedSourceIds = events.stream()
                .filter(e -> e.type() == ExecutionEventType.DOCUMENT_READ_COMPLETED
                        || e.type() == ExecutionEventType.EVIDENCE_RETRIEVED)
                .flatMap(e -> extractSourceIds(e).stream())
                .collect(Collectors.toSet());

        for (Evidence evidence : result.evidence()) {
            assertTrue(tracedSourceIds.contains(evidence.sourceId()),
                    "SC4: Evidence.sourceId='" + evidence.sourceId() + "' is not traceable to any " +
                    "DOCUMENT_READ_COMPLETED or EVIDENCE_RETRIEVED event metadata['" +
                    TraceEventMetadata.SOURCE_ID + "']. " +
                    "Traced sourceIds: " + tracedSourceIds + ". " +
                    "This indicates evidence was not obtained through real KnowledgeRepository calls.");
        }
    }

    private static Set<String> extractSourceIds(ExecutionEvent event) {
        String single = event.metadata().get(TraceEventMetadata.SOURCE_ID);
        if (single != null && !single.isBlank()) {
            return Set.of(single.strip());
        }
        String multi = event.metadata().get(TraceEventMetadata.SOURCE_IDS);
        if (multi != null && !multi.isBlank()) {
            return Arrays.stream(multi.split(","))
                    .map(String::strip)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        }
        return Set.of();
    }

    private static void assertEventPresent(List<ExecutionEvent> events, ExecutionEventType type, String message) {
        boolean found = events.stream().anyMatch(e -> e.type() == type);
        if (!found) throw new AssertionError(message);
    }

    private static void assertBefore(List<ExecutionEvent> events, ExecutionEventType first,
                                     ExecutionEventType second, String message) {
        int firstIdx = indexOfFirst(events, first);
        int secondIdx = indexOfFirst(events, second);
        if (firstIdx == -1 || secondIdx == -1 || firstIdx >= secondIdx) {
            throw new AssertionError(message +
                    " (firstIdx=" + firstIdx + ", secondIdx=" + secondIdx + ")");
        }
    }

    private static int indexOfFirst(List<ExecutionEvent> events, ExecutionEventType type) {
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).type() == type) return i;
        }
        return -1;
    }

    private static int countEvents(List<ExecutionEvent> events, ExecutionEventType type) {
        return (int) events.stream().filter(e -> e.type() == type).count();
    }

    private static void assertNotNull(Object value, String message) {
        if (value == null) throw new AssertionError(message);
    }

    private static void assertNotBlank(String value, String message) {
        if (value == null || value.isBlank()) throw new AssertionError(message);
    }

    private static void assertFalse(boolean condition, String message) {
        if (condition) throw new AssertionError(message);
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static <T> void assertEqual(T expected, T actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + " — expected: " + expected + ", actual: " + actual);
        }
    }
}
