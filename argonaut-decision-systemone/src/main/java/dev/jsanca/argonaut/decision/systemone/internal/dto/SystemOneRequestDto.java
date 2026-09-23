package dev.jsanca.argonaut.decision.systemone.internal.dto;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Wire representation of the request body sent to {@code POST /v1/systemone}.
 */
public class SystemOneRequestDto {

    private String model;
    private String state;
    private Map<String, SystemOneQuestionDto> questions;

    public SystemOneRequestDto() {}

    public SystemOneRequestDto(final String model, final String state,
                               final Map<String, SystemOneQuestionDto> questions) {
        this.model = model;
        this.state = state;
        this.questions = new LinkedHashMap<>(questions);
    }

    public String getModel() { return model; }
    public void setModel(final String model) { this.model = model; }

    public String getState() { return state; }
    public void setState(final String state) { this.state = state; }

    public Map<String, SystemOneQuestionDto> getQuestions() { return questions; }
    public void setQuestions(final Map<String, SystemOneQuestionDto> questions) {
        this.questions = questions;
    }
}
