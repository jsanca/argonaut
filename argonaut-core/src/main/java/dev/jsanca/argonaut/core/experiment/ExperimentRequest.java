package dev.jsanca.argonaut.core.experiment;

import java.util.Map;

/**
 * Represents a single Argonaut experiment run request.
 *
 * <p>{@code runId} identifies the execution across all services and the UI.</p>
 * <p>{@code question} is the user-facing mission presented to the agentic framework.</p>
 * <p>{@code parameters} carries optional runtime configuration without requiring
 * contract changes as the experiment evolves.</p>
 */
public record ExperimentRequest(String runId, String question, Map<String, String> parameters) {

    public ExperimentRequest {
        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("runId must not be blank");
        }
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question must not be blank");
        }
        parameters = parameters != null ? Map.copyOf(parameters) : Map.of();
    }

    public static ExperimentRequest of(String runId, String question) {
        return new ExperimentRequest(runId, question, Map.of());
    }
}
