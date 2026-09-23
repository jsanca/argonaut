package dev.jsanca.argonaut.decision.jev.internal.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Wire representation of a single question sent to the Jev API.
 *
 * <p>{@code criteria} is {@code null} for Noul questions and is omitted from the
 * serialized JSON via {@link JsonInclude#NON_NULL}. For Choice questions it is a
 * {@code Map<String,String>}; for Score questions it is a {@code List<String>}.</p>
 */
public class JevQuestionDto {

    private String type;
    private String instructions;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object criteria;

    public JevQuestionDto() {}

    public JevQuestionDto(final String type, final String instructions, final Object criteria) {
        this.type = type;
        this.instructions = instructions;
        this.criteria = criteria;
    }

    public String getType() { return type; }
    public void setType(final String type) { this.type = type; }

    public String getInstructions() { return instructions; }
    public void setInstructions(final String instructions) { this.instructions = instructions; }

    public Object getCriteria() { return criteria; }
    public void setCriteria(final Object criteria) { this.criteria = criteria; }
}
