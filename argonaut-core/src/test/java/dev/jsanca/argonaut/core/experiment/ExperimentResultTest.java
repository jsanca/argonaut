package dev.jsanca.argonaut.core.experiment;

import dev.jsanca.argonaut.core.error.ArgonautError;
import dev.jsanca.argonaut.core.error.ArgonautErrorCode;
import dev.jsanca.argonaut.core.evidence.Evidence;
import dev.jsanca.argonaut.core.evidence.EvidenceKind;
import dev.jsanca.argonaut.core.metrics.ExecutionMetrics;
import dev.jsanca.argonaut.core.trace.ExecutionTrace;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExperimentResultTest {

    @Test
    void completedFactory_setsCorrectStatusAndAnswer() {
        var result = ExperimentResult.completed(
                "run-1", "spring-ai", "The answer is 42.",
                List.of(), ExecutionTrace.empty(), ExecutionMetrics.empty());

        assertEquals(RunStatus.COMPLETED, result.status());
        assertEquals("The answer is 42.", result.finalAnswer());
        assertTrue(result.isSuccessful());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void failedFactory_setsCorrectStatusAndErrors() {
        var error = new ArgonautError(ArgonautErrorCode.MODEL_TIMEOUT, "timeout after 30s", "model-client", true);
        var result = ExperimentResult.failed(
                "run-2", "langchain4j", List.of(error),
                ExecutionTrace.empty(), ExecutionMetrics.empty());

        assertEquals(RunStatus.FAILED, result.status());
        assertNull(result.finalAnswer());
        assertFalse(result.isSuccessful());
        assertEquals(1, result.errors().size());
        assertEquals(ArgonautErrorCode.MODEL_TIMEOUT, result.errors().get(0).code());
    }

    @Test
    void result_exposesEvidenceList() {
        var evidence = new Evidence("e1", EvidenceKind.SUPPORTING, "doc-1", "Title", "excerpt", 0.9, "for answer");
        var result = ExperimentResult.completed(
                "run-3", "embabel", "Answer.",
                List.of(evidence), ExecutionTrace.empty(), ExecutionMetrics.empty());

        assertEquals(1, result.evidence().size());
        assertEquals("e1", result.evidence().get(0).id());
    }

    @Test
    void constructor_requiresNonNullRunId() {
        assertThrows(NullPointerException.class, () ->
                new ExperimentResult(null, "spring-ai", RunStatus.COMPLETED,
                        "answer", List.of(), ExecutionTrace.empty(), ExecutionMetrics.empty(), List.of()));
    }

    @Test
    void partialStatus_isConsideredSuccessful() {
        var result = new ExperimentResult("run-4", "langgraph4j", RunStatus.PARTIAL,
                "partial answer", List.of(), ExecutionTrace.empty(), ExecutionMetrics.empty(), List.of());
        assertTrue(result.isSuccessful());
    }
}
