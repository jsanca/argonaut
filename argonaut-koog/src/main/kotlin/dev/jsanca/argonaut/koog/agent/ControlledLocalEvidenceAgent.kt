package dev.jsanca.argonaut.koog.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import dev.jsanca.argonaut.core.evidence.Evidence
import dev.jsanca.argonaut.core.evidence.EvidenceKind
import dev.jsanca.argonaut.core.experiment.ControlledLocalEvidencePrompt
import dev.jsanca.argonaut.core.experiment.ExperimentRequest
import dev.jsanca.argonaut.core.experiment.ExperimentResult
import dev.jsanca.argonaut.core.knowledge.DocumentContent
import dev.jsanca.argonaut.core.knowledge.KnowledgeRepository
import dev.jsanca.argonaut.core.metrics.ExecutionMetrics
import dev.jsanca.argonaut.core.observability.InMemoryExecutionObserver
import dev.jsanca.argonaut.core.testing.TraceEventMetadata
import dev.jsanca.argonaut.core.trace.ExecutionEvent
import dev.jsanca.argonaut.core.trace.ExecutionEventType
import dev.jsanca.argonaut.core.trace.ExecutionTrace
import dev.jsanca.argonaut.koog.knowledge.KnowledgeTools
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * Koog implementation of the Controlled Local Evidence RAG use case (UC-001).
 *
 * Uses [AIAgent] with [AIAgentSimpleStrategies.singleRunStrategy] (default ReAct loop):
 * LLM call → tool calls → tool results → LLM call → … → final text answer.
 *
 * Per-run state injection: a new [KnowledgeTools] instance and [ToolRegistry] are created
 * per [run] invocation, with the [InMemoryExecutionObserver] and reads accumulator injected
 * at construction. This mirrors the LangChain4j 0.x constructor-injection pattern.
 *
 * [MODEL_CALL_STARTED][ExecutionEventType.MODEL_CALL_STARTED] and
 * [MODEL_CALL_COMPLETED][ExecutionEventType.MODEL_CALL_COMPLETED] events are observed via
 * Koog's [handleEvents] feature block — making Koog the second framework (after LangGraph4j)
 * to emit these events accurately.
 *
 * [AIAgent.run] and [AIAgent.close] are suspend functions; a [kotlinx.coroutines.runBlocking]
 * bridge is used from synchronous Spring MVC handlers.
 */
@Service
class ControlledLocalEvidenceAgent(
    private val executor: PromptExecutor,
    private val llmModel: LLModel,
    private val repository: KnowledgeRepository
) {

    fun run(request: ExperimentRequest): ExperimentResult {
        val startMs = System.currentTimeMillis()
        val observer = InMemoryExecutionObserver()
        val reads: MutableList<DocumentContent> = CopyOnWriteArrayList()
        val modelCallCounter = AtomicInteger(0)

        observer.record(agentEvent(ExecutionEventType.RUN_STARTED, emptyMap()))

        val knowledgeTools = KnowledgeTools(repository, observer, reads)
        val toolRegistry = ToolRegistry { tools(knowledgeTools) }

        val agent = AIAgent(
            promptExecutor = executor,
            llmModel = llmModel,
            toolRegistry = toolRegistry,
            systemPrompt = ControlledLocalEvidencePrompt.SYSTEM_PROMPT,
            maxIterations = 10
        ) {
            handleEvents {
                onLLMCallStarting { _ ->
                    observer.record(agentEvent(ExecutionEventType.MODEL_CALL_STARTED, emptyMap()))
                    modelCallCounter.incrementAndGet()
                }
                onLLMCallCompleted { _ ->
                    observer.record(agentEvent(ExecutionEventType.MODEL_CALL_COMPLETED, emptyMap()))
                }
            }
        }

        try {
            val answer: String = runBlocking { agent.run(request.question()) }
                ?: throw IllegalStateException("agent produced no answer")

            if (answer.isBlank()) {
                throw IllegalStateException("agent produced blank answer")
            }

            val evidence = buildEvidence(reads)
            for (ev in evidence) {
                observer.record(agentEvent(ExecutionEventType.EVIDENCE_SELECTED,
                    mapOf(TraceEventMetadata.SOURCE_ID to ev.sourceId(),
                          TraceEventMetadata.EVIDENCE_ID to ev.id())))
            }
            observer.record(agentEvent(ExecutionEventType.ANSWER_SYNTHESIZED, emptyMap()))
            observer.record(agentEvent(ExecutionEventType.RUN_COMPLETED, emptyMap()))

            val trace = observer.toTrace()
            val metrics = buildMetrics(trace, evidence, System.currentTimeMillis() - startMs, modelCallCounter.get())

            return ExperimentResult.completed(request.runId(), FRAMEWORK_ID, answer, evidence, trace, metrics)

        } catch (e: Exception) {
            observer.record(agentEvent(ExecutionEventType.RUN_FAILED, emptyMap()))
            val trace = observer.toTrace()
            val durationMs = System.currentTimeMillis() - startMs
            val metrics = ExecutionMetrics(durationMs, 0, 0, 0, 0, 0, 1)
            return ExperimentResult.failed(request.runId(), FRAMEWORK_ID, emptyList(), trace, metrics)
        } finally {
            runBlocking { try { agent.close() } catch (_: Exception) { } }
        }
    }

    private fun buildEvidence(reads: List<DocumentContent>): List<Evidence> =
        reads.map { doc ->
            val sourceId = doc.reference().sourceId()
            val body = doc.content()
            val excerpt = body.substring(0, minOf(200, body.length)).trim()
            Evidence(
                "ev-$sourceId",
                EvidenceKind.SUPPORTING,
                sourceId,
                sourceId,
                excerpt,
                1.0,
                "retrieved and read by Koog agent"
            )
        }

    private fun buildMetrics(
        trace: ExecutionTrace,
        evidence: List<Evidence>,
        durationMs: Long,
        modelCalls: Int
    ): ExecutionMetrics {
        val searches = countEvents(trace, ExecutionEventType.KNOWLEDGE_SEARCH_COMPLETED)
        val docReads = countEvents(trace, ExecutionEventType.DOCUMENT_READ_COMPLETED)
        return ExecutionMetrics(durationMs, modelCalls, 0, searches, docReads, evidence.size, 0)
    }

    private fun countEvents(trace: ExecutionTrace, type: ExecutionEventType): Int =
        trace.events().count { it.type() == type }

    private fun agentEvent(type: ExecutionEventType, metadata: Map<String, String>): ExecutionEvent =
        ExecutionEvent(
            UUID.randomUUID().toString(),
            Instant.now(),
            type,
            FRAMEWORK_ID,
            type.name.lowercase(),
            type.name,
            metadata,
            null
        )

    companion object {
        const val FRAMEWORK_ID = "koog"
    }
}
