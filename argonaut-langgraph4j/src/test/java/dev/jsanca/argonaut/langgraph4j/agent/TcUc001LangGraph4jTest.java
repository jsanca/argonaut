package dev.jsanca.argonaut.langgraph4j.agent;

import dev.jsanca.argonaut.core.experiment.ControlledLocalEvidencePrompt;
import dev.jsanca.argonaut.core.experiment.ExperimentRequest;
import dev.jsanca.argonaut.core.experiment.ExperimentResult;
import dev.jsanca.argonaut.core.knowledge.local.LocalKnowledgeRepository;
import dev.jsanca.argonaut.core.metrics.ExecutionMetrics;
import dev.jsanca.argonaut.core.observability.InMemoryExecutionObserver;
import dev.jsanca.argonaut.core.testing.ControlledLocalEvidenceContract;
import dev.jsanca.argonaut.core.testing.ExperimentExecutor;
import dev.jsanca.argonaut.core.trace.ExecutionEventType;
import dev.jsanca.argonaut.langgraph4j.knowledge.KnowledgeTools;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TC-UC-001 contract verification for the LangGraph4j implementation.
 *
 * <p>Uses {@link MockChatModel} to simulate tool-calling LLM behavior without a real model or
 * network call. The mock drives two searches and three document reads, exercising the full
 * agent graph loop and satisfying all nine SC assertions in
 * {@link ControlledLocalEvidenceContract}.</p>
 *
 * <p>The same shared {@link ControlledLocalEvidenceContract} validator is used — no
 * framework-specific weaker contract.</p>
 */
class TcUc001LangGraph4jTest {

    @Test
    void langGraph4jAgent_satisfies_tc_uc_001() {
        ExperimentExecutor executor = buildExecutor();
        assertDoesNotThrow(
                () -> ControlledLocalEvidenceContract.verify(executor),
                "LangGraph4j agent must satisfy all TC-UC-001 assertions");
    }

    @Test
    void langGraph4jAgent_result_is_non_null() {
        var result = buildExecutor().execute(ControlledLocalEvidenceContract.request());
        assertNotNull(result);
    }

    @Test
    void langGraph4jAgent_frameworkId_is_langgraph4j() {
        var result = buildExecutor().execute(ControlledLocalEvidenceContract.request());
        assertEquals("langgraph4j", result.frameworkId());
    }

    @Test
    void langGraph4jAgent_result_contains_exp001_evidence() {
        var result = buildExecutor().execute(ControlledLocalEvidenceContract.request());
        boolean hasExp001 = result.evidence().stream()
                .anyMatch(e -> "exp-001".equals(e.sourceId()));
        assertTrue(hasExp001, "result must contain exp-001 evidence");
    }

    @Test
    void langGraph4jAgent_result_has_non_blank_final_answer() {
        var result = buildExecutor().execute(ControlledLocalEvidenceContract.request());
        assertFalse(
                result.finalAnswer() == null || result.finalAnswer().isBlank(),
                "finalAnswer must not be blank");
    }

    @Test
    void langGraph4jAgent_uses_common_system_prompt() {
        assertFalse(ControlledLocalEvidencePrompt.SYSTEM_PROMPT.isBlank(),
                "common system prompt must not be blank");
        assertTrue(ControlledLocalEvidencePrompt.SYSTEM_PROMPT.contains("searchKnowledge"),
                "common system prompt must reference searchKnowledge");
    }

    @Test
    void langGraph4jAgent_model_calls_are_counted_accurately() {
        var result = buildExecutor().execute(ControlledLocalEvidenceContract.request());
        // MockChatModel runs 4 calls (2 search, 1 search+read, 1 read+read, 1 final answer)
        // LangGraph4j counts model calls directly — unlike Spring AI (hardcoded) or
        // LangChain4j 0.x AiServices (hidden). The actual count should be > 0.
        assertTrue(result.metrics().modelCalls() > 0,
                "LangGraph4j must report actual model call count (> 0)");
    }

    @Test
    void knowledge_search_delegates_to_repository() {
        var repository = LocalKnowledgeRepository.withDemoCorpus();
        var observer = new InMemoryExecutionObserver();
        var reads = new CopyOnWriteArrayList<dev.jsanca.argonaut.core.knowledge.DocumentContent>();
        var tools = new KnowledgeTools(repository, observer, reads);

        String result = tools.searchKnowledge("controlled evidence");

        assertFalse(result.isBlank(), "search must return non-blank results");
        assertTrue(result.contains("exp-001"), "search must return exp-001 for relevant query");
    }

    @Test
    void document_read_delegates_to_repository() {
        var repository = LocalKnowledgeRepository.withDemoCorpus();
        var observer = new InMemoryExecutionObserver();
        var reads = new CopyOnWriteArrayList<dev.jsanca.argonaut.core.knowledge.DocumentContent>();
        var tools = new KnowledgeTools(repository, observer, reads);

        String result = tools.readDocument("exp-001");

        assertFalse(result.isBlank(), "readDocument must return non-blank content");
        assertTrue(result.contains("exp-001"), "result must include source_id");
        assertEquals(1, reads.size(), "read must accumulate into reads list");
    }

    @Test
    void document_read_emits_trace_metadata_for_sc4() {
        var repository = LocalKnowledgeRepository.withDemoCorpus();
        var observer = new InMemoryExecutionObserver();
        var reads = new CopyOnWriteArrayList<dev.jsanca.argonaut.core.knowledge.DocumentContent>();
        var tools = new KnowledgeTools(repository, observer, reads);

        tools.readDocument("exp-001");

        var trace = observer.toTrace();
        boolean hasSourceId = trace.events().stream()
                .filter(e -> e.type() == ExecutionEventType.DOCUMENT_READ_COMPLETED
                          || e.type() == ExecutionEventType.EVIDENCE_RETRIEVED)
                .anyMatch(e -> "exp-001".equals(e.metadata().get(
                        dev.jsanca.argonaut.core.testing.TraceEventMetadata.SOURCE_ID)));
        assertTrue(hasSourceId,
                "DOCUMENT_READ_COMPLETED or EVIDENCE_RETRIEVED must carry sourceId=exp-001 for SC4");
    }

    @Test
    void hollow_result_cannot_bypass_tc_uc_001() {
        assertThrows(AssertionError.class, () -> {
            var trace = new InMemoryExecutionObserver().toTrace();
            var metrics = new ExecutionMetrics(0, 0, 0, 0, 0, 0, 0);
            var hollow = ExperimentResult.failed("run-hollow", "langgraph4j",
                    java.util.List.of(), trace, metrics);
            ControlledLocalEvidenceContract.assertSatisfied(hollow);
        }, "hollow/failed result must not pass TC-UC-001");
    }

    @Test
    void independent_runs_do_not_share_state() {
        var executor = buildExecutor();
        var req1 = ControlledLocalEvidenceContract.request();
        var req2 = ExperimentRequest.of("run-second", "second run question");

        var r1 = executor.execute(req1);
        var r2 = executor.execute(req2);

        assertNotEquals(r1.runId(), r2.runId(), "run IDs must be distinct");
        assertNotEquals(r1.trace(), r2.trace(), "traces must be independent");
    }

    private ExperimentExecutor buildExecutor() {
        var chatModel = new MockChatModel();
        var repository = LocalKnowledgeRepository.withDemoCorpus();
        var agent = new ControlledLocalEvidenceAgent(chatModel, repository);
        return agent::run;
    }
}
