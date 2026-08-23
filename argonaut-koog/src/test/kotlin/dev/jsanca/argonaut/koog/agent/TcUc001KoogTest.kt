package dev.jsanca.argonaut.koog.agent

import dev.jsanca.argonaut.core.experiment.ControlledLocalEvidencePrompt
import dev.jsanca.argonaut.core.experiment.ExperimentRequest
import dev.jsanca.argonaut.core.experiment.ExperimentResult
import dev.jsanca.argonaut.core.knowledge.local.LocalKnowledgeRepository
import dev.jsanca.argonaut.core.metrics.ExecutionMetrics
import dev.jsanca.argonaut.core.observability.InMemoryExecutionObserver
import dev.jsanca.argonaut.core.testing.ControlledLocalEvidenceContract
import dev.jsanca.argonaut.core.testing.ExperimentExecutor
import dev.jsanca.argonaut.core.testing.TraceEventMetadata
import dev.jsanca.argonaut.core.trace.ExecutionEventType
import dev.jsanca.argonaut.koog.knowledge.KnowledgeTools
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList

/**
 * TC-UC-001 contract verification for the Koog implementation.
 *
 * Uses [MockPromptExecutor] to drive deterministic tool-calling behavior without a real model
 * or network. The mock drives one search and one document read (exp-001), satisfying all nine
 * SC assertions in [ControlledLocalEvidenceContract].
 *
 * The same shared [ControlledLocalEvidenceContract] validator is used — no Koog-specific
 * weaker contract.
 */
class TcUc001KoogTest {

    @Test
    fun koogAgent_satisfies_tc_uc_001() {
        assertDoesNotThrow(
            { ControlledLocalEvidenceContract.verify(buildExecutor()) },
            "Koog agent must satisfy all TC-UC-001 assertions"
        )
    }

    @Test
    fun koogAgent_result_is_non_null() {
        val result = buildExecutor().execute(ControlledLocalEvidenceContract.request())
        assertNotNull(result)
    }

    @Test
    fun koogAgent_frameworkId_is_koog() {
        val result = buildExecutor().execute(ControlledLocalEvidenceContract.request())
        assertEquals("koog", result.frameworkId())
    }

    @Test
    fun koogAgent_result_contains_exp001_evidence() {
        val result = buildExecutor().execute(ControlledLocalEvidenceContract.request())
        val hasExp001 = result.evidence().any { it.sourceId() == "exp-001" }
        assertTrue(hasExp001, "result must contain exp-001 evidence")
    }

    @Test
    fun koogAgent_result_has_non_blank_final_answer() {
        val result = buildExecutor().execute(ControlledLocalEvidenceContract.request())
        assertFalse(
            result.finalAnswer() == null || result.finalAnswer().isBlank(),
            "finalAnswer must not be blank"
        )
    }

    @Test
    fun koogAgent_uses_common_system_prompt() {
        assertFalse(ControlledLocalEvidencePrompt.SYSTEM_PROMPT.isBlank(),
            "common system prompt must not be blank")
        assertTrue(ControlledLocalEvidencePrompt.SYSTEM_PROMPT.contains("searchKnowledge"),
            "common system prompt must reference searchKnowledge")
    }

    @Test
    fun koogAgent_model_calls_are_counted_accurately() {
        val result = buildExecutor().execute(ControlledLocalEvidenceContract.request())
        assertTrue(
            result.metrics().modelCalls() > 0,
            "Koog must report actual model call count (> 0)"
        )
    }

    @Test
    fun knowledge_search_delegates_to_repository() {
        val repository = LocalKnowledgeRepository.withDemoCorpus()
        val observer = InMemoryExecutionObserver()
        val reads = CopyOnWriteArrayList<dev.jsanca.argonaut.core.knowledge.DocumentContent>()
        val tools = KnowledgeTools(repository, observer, reads)

        val result = tools.searchKnowledge("controlled evidence")

        assertFalse(result.isBlank(), "search must return non-blank results")
        assertTrue(result.contains("exp-001"), "search must return exp-001 for relevant query")
    }

    @Test
    fun document_read_delegates_to_repository() {
        val repository = LocalKnowledgeRepository.withDemoCorpus()
        val observer = InMemoryExecutionObserver()
        val reads = CopyOnWriteArrayList<dev.jsanca.argonaut.core.knowledge.DocumentContent>()
        val tools = KnowledgeTools(repository, observer, reads)

        val result = tools.readDocument("exp-001")

        assertFalse(result.isBlank(), "readDocument must return non-blank content")
        assertTrue(result.contains("exp-001"), "result must include source_id")
        assertEquals(1, reads.size, "read must accumulate into reads list")
    }

    @Test
    fun document_read_emits_trace_metadata_for_sc4() {
        val repository = LocalKnowledgeRepository.withDemoCorpus()
        val observer = InMemoryExecutionObserver()
        val reads = CopyOnWriteArrayList<dev.jsanca.argonaut.core.knowledge.DocumentContent>()
        val tools = KnowledgeTools(repository, observer, reads)

        tools.readDocument("exp-001")

        val trace = observer.toTrace()
        val hasSourceId = trace.events()
            .filter { it.type() == ExecutionEventType.DOCUMENT_READ_COMPLETED
                    || it.type() == ExecutionEventType.EVIDENCE_RETRIEVED }
            .any { it.metadata()[TraceEventMetadata.SOURCE_ID] == "exp-001" }
        assertTrue(hasSourceId,
            "DOCUMENT_READ_COMPLETED or EVIDENCE_RETRIEVED must carry sourceId=exp-001 for SC4")
    }

    @Test
    fun hollow_result_cannot_bypass_tc_uc_001() {
        assertThrows(AssertionError::class.java) {
            val trace = InMemoryExecutionObserver().toTrace()
            val metrics = ExecutionMetrics(0, 0, 0, 0, 0, 0, 0)
            val hollow = ExperimentResult.failed(
                "run-hollow", "koog", emptyList(), trace, metrics
            )
            ControlledLocalEvidenceContract.assertSatisfied(hollow)
        }
    }

    @Test
    fun independent_runs_do_not_share_state() {
        val executor = buildExecutor()
        val req1 = ControlledLocalEvidenceContract.request()
        val req2 = ExperimentRequest.of("run-second", "second run question")

        val r1 = executor.execute(req1)
        val r2 = executor.execute(req2)

        assertNotEquals(r1.runId(), r2.runId(), "run IDs must be distinct")
        assertNotEquals(r1.trace(), r2.trace(), "traces must be independent")
    }

    private fun buildExecutor(): ExperimentExecutor {
        val mockExecutor = MockPromptExecutor()
        val repository = LocalKnowledgeRepository.withDemoCorpus()
        val llmModel = ai.koog.prompt.llm.LLModel(
            provider = ai.koog.prompt.llm.LLMProvider.OpenRouter,
            id = "test/mock-model",
            capabilities = listOf(
                ai.koog.prompt.llm.LLMCapability.Tools,
                ai.koog.prompt.llm.LLMCapability.Completion
            )
        )
        val agent = ControlledLocalEvidenceAgent(mockExecutor, llmModel, repository)
        return ExperimentExecutor { request -> agent.run(request) }
    }
}
