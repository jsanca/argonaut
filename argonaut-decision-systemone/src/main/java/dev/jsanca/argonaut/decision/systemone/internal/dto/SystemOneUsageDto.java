package dev.jsanca.argonaut.decision.systemone.internal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Token usage statistics returned by the System One API.
 */
public class SystemOneUsageDto {

    @JsonProperty("input_tokens")
    private int inputTokens;

    @JsonProperty("output_tokens")
    private int outputTokens;

    public SystemOneUsageDto() {}

    public SystemOneUsageDto(final int inputTokens, final int outputTokens) {
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
    }

    public int getInputTokens() { return inputTokens; }
    public void setInputTokens(final int inputTokens) { this.inputTokens = inputTokens; }

    public int getOutputTokens() { return outputTokens; }
    public void setOutputTokens(final int outputTokens) { this.outputTokens = outputTokens; }
}
