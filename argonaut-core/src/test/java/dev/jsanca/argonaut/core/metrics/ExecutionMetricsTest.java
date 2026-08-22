package dev.jsanca.argonaut.core.metrics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionMetricsTest {

    @Test
    void empty_returnsAllZeroes() {
        var metrics = ExecutionMetrics.empty();
        assertEquals(0, metrics.durationMs());
        assertEquals(0, metrics.modelCalls());
        assertEquals(0, metrics.toolCalls());
        assertEquals(0, metrics.knowledgeSearches());
        assertEquals(0, metrics.documentReads());
        assertEquals(0, metrics.evidenceCount());
        assertEquals(0, metrics.errors());
    }

    @Test
    void builder_accumulatesMetricsCorrectly() {
        var metrics = ExecutionMetrics.builder()
                .durationMs(1234)
                .incrementModelCalls()
                .incrementModelCalls()
                .incrementToolCalls()
                .incrementKnowledgeSearches()
                .incrementDocumentReads()
                .incrementDocumentReads()
                .addEvidence(3)
                .incrementErrors()
                .build();

        assertEquals(1234, metrics.durationMs());
        assertEquals(2, metrics.modelCalls());
        assertEquals(1, metrics.toolCalls());
        assertEquals(1, metrics.knowledgeSearches());
        assertEquals(2, metrics.documentReads());
        assertEquals(3, metrics.evidenceCount());
        assertEquals(1, metrics.errors());
    }

    @Test
    void builder_startsFresh() {
        var m1 = ExecutionMetrics.builder().incrementModelCalls().build();
        var m2 = ExecutionMetrics.builder().build();

        assertEquals(1, m1.modelCalls());
        assertEquals(0, m2.modelCalls());
    }
}
