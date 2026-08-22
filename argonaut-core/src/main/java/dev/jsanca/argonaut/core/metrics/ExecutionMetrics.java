package dev.jsanca.argonaut.core.metrics;

/**
 * Basic comparable metrics for a single Argonaut experiment run.
 *
 * <p>These metrics are intentionally simple — enough for the Vue experiment console to
 * compare runs across frameworks visually. They are not production telemetry.</p>
 */
public record ExecutionMetrics(
        long durationMs,
        int modelCalls,
        int toolCalls,
        int knowledgeSearches,
        int documentReads,
        int evidenceCount,
        int errors
) {
    public static ExecutionMetrics empty() {
        return new ExecutionMetrics(0, 0, 0, 0, 0, 0, 0);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private long durationMs;
        private int modelCalls;
        private int toolCalls;
        private int knowledgeSearches;
        private int documentReads;
        private int evidenceCount;
        private int errors;

        private Builder() {}

        public Builder durationMs(long value) { this.durationMs = value; return this; }
        public Builder incrementModelCalls() { this.modelCalls++; return this; }
        public Builder incrementToolCalls() { this.toolCalls++; return this; }
        public Builder incrementKnowledgeSearches() { this.knowledgeSearches++; return this; }
        public Builder incrementDocumentReads() { this.documentReads++; return this; }
        public Builder addEvidence(int count) { this.evidenceCount += count; return this; }
        public Builder incrementErrors() { this.errors++; return this; }

        public ExecutionMetrics build() {
            return new ExecutionMetrics(durationMs, modelCalls, toolCalls,
                    knowledgeSearches, documentReads, evidenceCount, errors);
        }
    }
}
