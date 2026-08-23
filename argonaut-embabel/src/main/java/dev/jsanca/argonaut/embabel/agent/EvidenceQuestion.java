package dev.jsanca.argonaut.embabel.agent;

/**
 * Blackboard input type for the controlled local evidence agent.
 * The Embabel planner puts this on the blackboard; the single @AchievesGoal action consumes it.
 */
public record EvidenceQuestion(String runId, String question) {}
