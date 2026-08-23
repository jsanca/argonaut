package dev.jsanca.argonaut.embabel.agent;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Structured LLM output for the synthesis step.
 * Embabel's createObject() asks the LLM to populate this schema.
 */
public record AnswerText(
        @JsonPropertyDescription("The synthesized answer to the question, grounded in the provided evidence")
        String answer
) {}
