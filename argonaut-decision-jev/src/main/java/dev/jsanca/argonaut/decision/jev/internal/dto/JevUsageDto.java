package dev.jsanca.argonaut.decision.jev.internal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Token usage statistics returned by the Jev API.
 */
public class JevUsageDto {

    @JsonProperty("input_tokens")
    private int inputTokens;

    @JsonProperty("output_tokens")
    private int outputTokens;

    public JevUsageDto() {}

    public JevUsageDto(final int inputTokens, final int outputTokens) {
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
    }

    public int getInputTokens() { return inputTokens; }
    public void setInputTokens(final int inputTokens) { this.inputTokens = inputTokens; }

    public int getOutputTokens() { return outputTokens; }
    public void setOutputTokens(final int outputTokens) { this.outputTokens = outputTokens; }
}
