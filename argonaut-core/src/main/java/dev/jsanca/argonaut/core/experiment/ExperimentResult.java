package dev.jsanca.argonaut.core.experiment;

import dev.jsanca.argonaut.core.error.ArgonautError;
import dev.jsanca.argonaut.core.evidence.Evidence;
import dev.jsanca.argonaut.core.metrics.ExecutionMetrics;
import dev.jsanca.argonaut.core.trace.ExecutionTrace;

import java.util.List;
import java.util.Objects;

/**
 * The complete observable result of one Argonaut experiment run.
 *
 * <p>This is the primary payload that the Vue experiment console consumes to compare
 * framework executions. It must be interpretable without knowledge of which framework
 * produced it.</p>
 */
public record ExperimentResult(
        String runId,
        String frameworkId,
        RunStatus status,
        String finalAnswer,
        List<Evidence> evidence,
        ExecutionTrace trace,
        ExecutionMetrics metrics,
        List<ArgonautError> errors
) {
    public ExperimentResult {
        Objects.requireNonNull(runId, "runId must not be null");
        Objects.requireNonNull(frameworkId, "frameworkId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(metrics, "metrics must not be null");
        evidence = evidence != null ? List.copyOf(evidence) : List.of();
        trace = trace != null ? trace : ExecutionTrace.empty();
        errors = errors != null ? List.copyOf(errors) : List.of();
    }

    public static ExperimentResult completed(
            String runId,
            String frameworkId,
            String finalAnswer,
            List<Evidence> evidence,
            ExecutionTrace trace,
            ExecutionMetrics metrics) {
        if (finalAnswer == null || finalAnswer.isBlank()) {
            throw new IllegalArgumentException("finalAnswer must not be blank for a COMPLETED result");
        }
        return new ExperimentResult(runId, frameworkId, RunStatus.COMPLETED,
                finalAnswer, evidence, trace, metrics, List.of());
    }

    public static ExperimentResult failed(
            String runId,
            String frameworkId,
            List<ArgonautError> errors,
            ExecutionTrace trace,
            ExecutionMetrics metrics) {
        return new ExperimentResult(runId, frameworkId, RunStatus.FAILED,
                null, List.of(), trace, metrics, errors);
    }

    public boolean isSuccessful() {
        return status == RunStatus.COMPLETED || status == RunStatus.PARTIAL;
    }
}
