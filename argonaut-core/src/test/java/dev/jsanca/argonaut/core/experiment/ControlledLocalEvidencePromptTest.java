package dev.jsanca.argonaut.core.experiment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ControlledLocalEvidencePromptTest {

    @Test
    void systemPrompt_is_not_blank() {
        assertFalse(ControlledLocalEvidencePrompt.SYSTEM_PROMPT.isBlank());
    }

    @Test
    void systemPrompt_instructs_tool_use() {
        String prompt = ControlledLocalEvidencePrompt.SYSTEM_PROMPT;
        assertTrue(prompt.contains("searchKnowledge"),
                "prompt must reference the searchKnowledge tool so all frameworks use consistent tool names");
        assertTrue(prompt.contains("readDocument"),
                "prompt must reference the readDocument tool so all frameworks use consistent tool names");
    }

    @Test
    void systemPrompt_instructs_at_least_two_searches() {
        assertTrue(ControlledLocalEvidencePrompt.SYSTEM_PROMPT.contains("at least two searches"),
                "prompt must instruct the model to issue at least two searches to satisfy TC-UC-001 SC5");
    }

    @Test
    void systemPrompt_constrains_evidence_to_corpus_only() {
        String prompt = ControlledLocalEvidencePrompt.SYSTEM_PROMPT;
        assertTrue(prompt.toLowerCase().contains("only"),
                "prompt must instruct the model to use only corpus evidence");
    }
}
