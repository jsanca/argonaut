package dev.jsanca.argonaut.langchain4j.agent;

import dev.jsanca.argonaut.core.experiment.ControlledLocalEvidencePrompt;
import dev.jsanca.argonaut.core.knowledge.local.LocalKnowledgeRepository;
import dev.jsanca.argonaut.core.testing.ControlledLocalEvidenceContract;
import dev.jsanca.argonaut.core.testing.ExperimentExecutor;
import dev.jsanca.argonaut.langchain4j.knowledge.KnowledgeTools;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TC-UC-001 contract verification for the LangChain4j implementation.
 *
 * <p>Uses {@link MockChatLanguageModel} to simulate tool-calling LLM behavior without a real
 * model or network call. The mock drives two search calls and three document reads, which
 * exercises the full agent loop and satisfies all nine SC assertions in
 * {@link ControlledLocalEvidenceContract}.</p>
 *
 * <p>The common {@link ControlledLocalEvidenceContract} validator is reused without
 * modification — LangChain4j receives no weaker or framework-specific contract.</p>
 */
class TcUc001LangChain4jTest {

    @Test
    void langChain4jAgent_satisfies_tc_uc_001() {
        ExperimentExecutor executor = buildExecutor();
        assertDoesNotThrow(
                () -> ControlledLocalEvidenceContract.verify(executor),
                "LangChain4j agent must satisfy all TC-UC-001 assertions");
    }

    @Test
    void langChain4jAgent_result_is_non_null() {
        var result = buildExecutor().execute(ControlledLocalEvidenceContract.request());
        assertNotNull(result);
    }

    @Test
    void langChain4jAgent_frameworkId_is_langchain4j() {
        var result = buildExecutor().execute(ControlledLocalEvidenceContract.request());
        assertEquals("langchain4j", result.frameworkId());
    }

    @Test
    void langChain4jAgent_result_contains_exp001_evidence() {
        var result = buildExecutor().execute(ControlledLocalEvidenceContract.request());
        boolean hasExp001 = result.evidence().stream()
                .anyMatch(e -> "exp-001".equals(e.sourceId()));
        assertTrue(hasExp001, "result must contain exp-001 evidence");
    }

    @Test
    void langChain4jAgent_result_has_non_blank_final_answer() {
        var result = buildExecutor().execute(ControlledLocalEvidenceContract.request());
        assertFalse(
                result.finalAnswer() == null || result.finalAnswer().isBlank(),
                "finalAnswer must not be blank");
    }

    @Test
    void langChain4jAgent_uses_common_system_prompt() {
        assertFalse(ControlledLocalEvidencePrompt.SYSTEM_PROMPT.isBlank(),
                "common system prompt must not be blank");
        assertTrue(ControlledLocalEvidencePrompt.SYSTEM_PROMPT.contains("searchKnowledge"),
                "common system prompt must reference searchKnowledge");
    }

    @Test
    void knowledge_search_delegates_to_repository() {
        var repository = LocalKnowledgeRepository.withDemoCorpus();
        var observer = new dev.jsanca.argonaut.core.observability.InMemoryExecutionObserver();
        var reads = new java.util.concurrent.CopyOnWriteArrayList<dev.jsanca.argonaut.core.knowledge.DocumentContent>();
        var tools = new KnowledgeTools(repository, observer, reads);

        String result = tools.searchKnowledge("controlled evidence");

        assertFalse(result.isBlank(), "search must return non-blank results");
        assertTrue(result.contains("exp-001"), "search must return exp-001 for relevant query");
    }

    @Test
    void document_read_delegates_to_repository() {
        var repository = LocalKnowledgeRepository.withDemoCorpus();
        var observer = new dev.jsanca.argonaut.core.observability.InMemoryExecutionObserver();
        var reads = new java.util.concurrent.CopyOnWriteArrayList<dev.jsanca.argonaut.core.knowledge.DocumentContent>();
        var tools = new KnowledgeTools(repository, observer, reads);

        String result = tools.readDocument("exp-001");

        assertFalse(result.isBlank(), "readDocument must return non-blank content");
        assertTrue(result.contains("exp-001"), "result must include source_id");
        assertEquals(1, reads.size(), "read must accumulate into reads list");
    }

    @Test
    void document_read_emits_trace_metadata_for_sc4() {
        var repository = LocalKnowledgeRepository.withDemoCorpus();
        var observer = new dev.jsanca.argonaut.core.observability.InMemoryExecutionObserver();
        var reads = new java.util.concurrent.CopyOnWriteArrayList<dev.jsanca.argonaut.core.knowledge.DocumentContent>();
        var tools = new KnowledgeTools(repository, observer, reads);

        tools.readDocument("exp-001");

        var trace = observer.toTrace();
        boolean hasSourceId = trace.events().stream()
                .filter(e -> e.type() == dev.jsanca.argonaut.core.trace.ExecutionEventType.DOCUMENT_READ_COMPLETED
                          || e.type() == dev.jsanca.argonaut.core.trace.ExecutionEventType.EVIDENCE_RETRIEVED)
                .anyMatch(e -> "exp-001".equals(e.metadata().get(dev.jsanca.argonaut.core.testing.TraceEventMetadata.SOURCE_ID)));
        assertTrue(hasSourceId, "DOCUMENT_READ_COMPLETED or EVIDENCE_RETRIEVED must carry sourceId=exp-001 for SC4");
    }

    private ExperimentExecutor buildExecutor() {
        var chatModel = new MockChatLanguageModel();
        var repository = LocalKnowledgeRepository.withDemoCorpus();
        var agent = new ControlledLocalEvidenceAgent(chatModel, repository);
        return agent::run;
    }
}
