package dev.jsanca.argonaut.decision.jev.internal.dto;

import java.util.Map;

/**
 * Wire representation of the response body from {@code POST /v1/systemone}.
 */
public class JevResponseDto {

    private Map<String, JevAnswerDto> answers;
    private JevUsageDto usage;

    public JevResponseDto() {}

    public JevResponseDto(final Map<String, JevAnswerDto> answers, final JevUsageDto usage) {
        this.answers = answers;
        this.usage = usage;
    }

    public Map<String, JevAnswerDto> getAnswers() { return answers; }
    public void setAnswers(final Map<String, JevAnswerDto> answers) { this.answers = answers; }

    public JevUsageDto getUsage() { return usage; }
    public void setUsage(final JevUsageDto usage) { this.usage = usage; }
}
