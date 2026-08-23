package dev.jsanca.argonaut.embabel.agent;

import com.embabel.agent.test.unit.FakeOperationContext;
import dev.jsanca.argonaut.core.experiment.ExperimentResult;
import dev.jsanca.argonaut.core.knowledge.local.LocalKnowledgeRepository;
import dev.jsanca.argonaut.core.testing.ControlledLocalEvidenceContract;
import dev.jsanca.argonaut.core.testing.ExperimentExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TC-UC-001 contract verification for the Embabel implementation.
 *
 * <p>Testing strategy: the {@link ControlledLocalEvidenceAgent#answer} action is called
 * directly (bypassing the Embabel platform) with:
 * <ul>
 *   <li>A real {@link LocalKnowledgeRepository} — so retrieval, search events, and
 *       evidence traceability (SC4) reflect genuine repository calls.</li>
 *   <li>A {@link FakeOperationContext} — so LLM synthesis returns a deterministic answer
 *       without a real model call or network.</li>
 * </ul>
 *
 * <p>This mirrors the Embabel-native testing pattern demonstrated in the upstream examples
 * ({@code StarNewsFinderTest}, {@code UserGuideValidatorAgentTest}): actions are regular
 * Java methods that can be called directly with a fake context for unit-level verification.
 */
class TcUc001EmbabelTest {

    private static final String FINAL_ANSWER =
            "Argonaut uses controlled local evidence before introducing web search, vector databases, " +
            "or external observability tools because controlled evidence reduces experimental noise. " +
            "When all framework implementations receive identical documents from the same controlled " +
            "corpus, any observed differences in their answers reflect framework orchestration quality " +
            "rather than retrieval quality. The controlled corpus also makes experiments reproducible: " +
            "the same query produces the same retrieval results on every run, enabling reliable " +
            "comparison across Spring AI, LangChain4j, LangGraph4j, and Embabel. Without controlled " +
            "evidence, differences in results may reflect retrieval noise rather than meaningful " +
            "differences in how each framework orchestrates its agentic behavior.";

    private ControlledLocalEvidenceAgent agent;

    @BeforeEach
    void setUp() {
        agent = new ControlledLocalEvidenceAgent(LocalKnowledgeRepository.withDemoCorpus());
    }

    private ExperimentExecutor buildExecutor() {
        return request -> {
            var fakeContext = new FakeOperationContext();
            fakeContext.expectResponse(new AnswerText(FINAL_ANSWER));
            var question = new EvidenceQuestion(request.runId(), request.question());
            EvidenceAnswer answer = agent.answer(question, fakeContext);
            return ExperimentResult.completed(
                    request.runId(), ControlledLocalEvidenceAgent.FRAMEWORK_ID,
                    answer.finalAnswer(), answer.evidence(), answer.trace(), answer.metrics());
        };
    }

    @Test
    void embabelAgent_satisfies_tc_uc_001() {
        assertDoesNotThrow(
                () -> ControlledLocalEvidenceContract.verify(buildExecutor()),
                "Embabel agent must satisfy all TC-UC-001 assertions");
    }

    @Test
    void embabelAgent_result_is_non_null() {
        var result = buildExecutor().execute(ControlledLocalEvidenceContract.request());
        assertNotNull(result);
    }

    @Test
    void embabelAgent_result_contains_exp001_evidence() {
        var result = buildExecutor().execute(ControlledLocalEvidenceContract.request());
        boolean hasExp001 = result.evidence().stream()
                .anyMatch(e -> "exp-001".equals(e.sourceId()));
        assertTrue(hasExp001, "result must contain exp-001 evidence");
    }

    @Test
    void embabelAgent_result_has_non_blank_final_answer() {
        var result = buildExecutor().execute(ControlledLocalEvidenceContract.request());
        assertFalse(
                result.finalAnswer() == null || result.finalAnswer().isBlank(),
                "finalAnswer must not be blank");
    }

    @Nested
    class ActionShapeTests {

        @Test
        void answer_action_calls_llm_for_synthesis() {
            var fakeContext = new FakeOperationContext();
            fakeContext.expectResponse(new AnswerText("test answer"));
            var question = new EvidenceQuestion("test-run", ControlledLocalEvidenceContract.EXPERIMENT_QUESTION);

            agent.answer(question, fakeContext);

            assertFalse(fakeContext.getLlmInvocations().isEmpty(),
                    "answer action must invoke the LLM for synthesis");
        }

        @Test
        void answer_action_does_not_attach_tool_groups_to_synthesis_prompt() {
            var fakeContext = new FakeOperationContext();
            fakeContext.expectResponse(new AnswerText("test answer"));
            var question = new EvidenceQuestion("test-run", ControlledLocalEvidenceContract.EXPERIMENT_QUESTION);

            agent.answer(question, fakeContext);

            var toolGroups = fakeContext.getLlmInvocations().getFirst().getInteraction().getToolGroups();
            assertTrue(toolGroups.isEmpty(),
                    "synthesis must not attach external tool groups — retrieval is planner-driven");
        }

        @Test
        void answer_action_embeds_question_in_synthesis_prompt() {
            var fakeContext = new FakeOperationContext();
            fakeContext.expectResponse(new AnswerText("test answer"));
            var question = new EvidenceQuestion("test-run", ControlledLocalEvidenceContract.EXPERIMENT_QUESTION);

            agent.answer(question, fakeContext);

            String prompt = fakeContext.getLlmInvocations().getFirst().getMessages().getFirst().getContent();
            assertTrue(prompt.contains(ControlledLocalEvidenceContract.EXPERIMENT_QUESTION),
                    "synthesis prompt must contain the question text");
        }

        @Test
        void answer_action_embeds_evidence_in_synthesis_prompt() {
            var fakeContext = new FakeOperationContext();
            fakeContext.expectResponse(new AnswerText("test answer"));
            var question = new EvidenceQuestion("test-run", ControlledLocalEvidenceContract.EXPERIMENT_QUESTION);

            agent.answer(question, fakeContext);

            String prompt = fakeContext.getLlmInvocations().getFirst().getMessages().getFirst().getContent();
            assertTrue(prompt.contains("exp-001"),
                    "synthesis prompt must embed exp-001 evidence content");
        }
    }
}
