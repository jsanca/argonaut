package dev.jsanca.argonaut.koog.agent

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutorOperation
import ai.koog.prompt.executor.model.ResolvedModel
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.Prompt
import ai.koog.utils.time.KoogClock
import java.util.concurrent.atomic.AtomicInteger

/**
 * Deterministic mock for [ai.koog.prompt.executor.model.PromptExecutor] used in TC-UC-001 tests.
 *
 * 3-stage state machine (no real LLM or network):
 * - Call 1: searchKnowledge tool call (searches for the query)
 * - Call 2: readDocument tool call (reads exp-001)
 * - Call 3+: final text answer referencing exp-001
 *
 * Extends [MultiLLMPromptExecutor] with an empty client map so all boilerplate methods have
 * non-crashing default implementations. Overrides [resolveModel] to bypass client-map lookup
 * and the [ResolvedModel]-based [execute] overload as recommended for custom subclasses.
 */
class MockPromptExecutor : MultiLLMPromptExecutor(emptyMap()) {

    private val callCount = AtomicInteger(0)

    override suspend fun resolveModel(
        model: LLModel,
        promptExecutorOperation: PromptExecutorOperation
    ): ResolvedModel = ResolvedModel(effectiveModel = model)

    override suspend fun execute(
        prompt: Prompt,
        model: ResolvedModel,
        tools: List<ToolDescriptor>
    ): Message.Assistant {
        val call = callCount.incrementAndGet()
        val meta = ResponseMetaInfo.create(KoogClock.System)
        return when (call) {
            1 -> Message.Assistant(
                parts = listOf(
                    MessagePart.Tool.Call(
                        id = "mock-call-1",
                        tool = "searchKnowledge",
                        args = """{"query":"carbon fiber aerospace materials"}"""
                    )
                ),
                metaInfo = meta,
                finishReason = "tool_calls"
            )
            2 -> Message.Assistant(
                parts = listOf(
                    MessagePart.Tool.Call(
                        id = "mock-call-2",
                        tool = "readDocument",
                        args = """{"sourceId":"exp-001"}"""
                    )
                ),
                metaInfo = meta,
                finishReason = "tool_calls"
            )
            else -> Message.Assistant(
                parts = listOf(
                    MessagePart.Text(
                        "Based on the knowledge corpus (exp-001), the primary material used in " +
                        "aerospace structures is carbon fiber reinforced polymer (CFRP). " +
                        "Source: exp-001."
                    )
                ),
                metaInfo = meta,
                finishReason = "stop"
            )
        }
    }
}
