package dev.jsanca.argonaut.decision.jev.internal.dto;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Wire representation of the request body sent to {@code POST /v1/systemone}.
 */
public class JevRequestDto {

    private String model;
    private String state;
    private Map<String, JevQuestionDto> questions;

    public JevRequestDto() {}

    public JevRequestDto(final String model, final String state,
                         final Map<String, JevQuestionDto> questions) {
        this.model = model;
        this.state = state;
        this.questions = new LinkedHashMap<>(questions);
    }

    public String getModel() { return model; }
    public void setModel(final String model) { this.model = model; }

    public String getState() { return state; }
    public void setState(final String state) { this.state = state; }

    public Map<String, JevQuestionDto> getQuestions() { return questions; }
    public void setQuestions(final Map<String, JevQuestionDto> questions) {
        this.questions = questions;
    }
}
