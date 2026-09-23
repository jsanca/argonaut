package dev.jsanca.argonaut.decision.systemone.internal.dto;

import java.util.Map;

/**
 * Wire representation of the response body from {@code POST /v1/systemone}.
 *
 * <p>The {@code model} field echoes the model name used for the evaluation.
 * {@code answers} maps each question id to its typed answer.
 * {@code usage} carries token consumption statistics.</p>
 */
public class SystemOneResponseDto {

    private String model;
    private Map<String, SystemOneAnswerDto> answers;
    private SystemOneUsageDto usage;

    public SystemOneResponseDto() {}

    public SystemOneResponseDto(final String model,
                                final Map<String, SystemOneAnswerDto> answers,
                                final SystemOneUsageDto usage) {
        this.model = model;
        this.answers = answers;
        this.usage = usage;
    }

    public String getModel() { return model; }
    public void setModel(final String model) { this.model = model; }

    public Map<String, SystemOneAnswerDto> getAnswers() { return answers; }
    public void setAnswers(final Map<String, SystemOneAnswerDto> answers) { this.answers = answers; }

    public SystemOneUsageDto getUsage() { return usage; }
    public void setUsage(final SystemOneUsageDto usage) { this.usage = usage; }
}
