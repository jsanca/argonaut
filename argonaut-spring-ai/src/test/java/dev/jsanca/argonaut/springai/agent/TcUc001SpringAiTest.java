package dev.jsanca.argonaut.springai.agent;

import dev.jsanca.argonaut.core.knowledge.local.LocalKnowledgeRepository;
import dev.jsanca.argonaut.core.testing.ControlledLocalEvidenceContract;
import dev.jsanca.argonaut.core.testing.ExperimentExecutor;
import dev.jsanca.argonaut.springai.knowledge.KnowledgeTool;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * TC-UC-001 contract verification for the Spring AI implementation.
 *
 * <p>Uses {@link MockRagChatModel} to simulate tool-calling LLM behavior without a real
 * model or network call. The mock drives two search calls and three document reads, which
 * exercises the full agent loop and satisfies all nine SC assertions in
 * {@link ControlledLocalEvidenceContract}.</p>
 */
class TcUc001SpringAiTest {

    @Test
    void springAiAgent_satisfies_tc_uc_001() {
        ExperimentExecutor executor = buildExecutor();
        assertDoesNotThrow(
                () -> ControlledLocalEvidenceContract.verify(executor),
                "Spring AI agent must satisfy all TC-UC-001 assertions");
    }

    @Test
    void springAiAgent_result_is_non_null() {
        ExperimentExecutor executor = buildExecutor();
        var result = executor.execute(ControlledLocalEvidenceContract.request());
        assertNotNull(result);
    }

    @Test
    void springAiAgent_result_contains_exp001_evidence() {
        ExperimentExecutor executor = buildExecutor();
        var result = executor.execute(ControlledLocalEvidenceContract.request());
        boolean hasExp001 = result.evidence().stream()
                .anyMatch(e -> "exp-001".equals(e.sourceId()));
        org.junit.jupiter.api.Assertions.assertTrue(hasExp001,
                "result must contain exp-001 evidence");
    }

    @Test
    void springAiAgent_result_has_non_blank_final_answer() {
        ExperimentExecutor executor = buildExecutor();
        var result = executor.execute(ControlledLocalEvidenceContract.request());
        org.junit.jupiter.api.Assertions.assertFalse(
                result.finalAnswer() == null || result.finalAnswer().isBlank(),
                "finalAnswer must not be blank");
    }

    private ExperimentExecutor buildExecutor() {
        var chatModel = new MockRagChatModel();
        var chatClient = ChatClient.builder(chatModel).build();
        var knowledgeTool = new KnowledgeTool(LocalKnowledgeRepository.withDemoCorpus());
        var agent = new ControlledLocalEvidenceAgent(chatClient, knowledgeTool);
        return agent::run;
    }
}
