package dev.jsanca.argonaut.embabel.agent;

import dev.jsanca.argonaut.core.evidence.Evidence;
import dev.jsanca.argonaut.core.metrics.ExecutionMetrics;
import dev.jsanca.argonaut.core.trace.ExecutionTrace;

import java.util.List;

/**
 * Goal type returned by the controlled local evidence agent.
 * The Embabel planner treats completion of the @AchievesGoal action as mission success.
 * The controller maps this to ExperimentResult for the HTTP response.
 */
public record EvidenceAnswer(
        String finalAnswer,
        List<Evidence> evidence,
        ExecutionTrace trace,
        ExecutionMetrics metrics
) {}
