package dev.jsanca.argonaut.koog.knowledge

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import dev.jsanca.argonaut.core.knowledge.DocumentContent
import dev.jsanca.argonaut.core.knowledge.DocumentReference
import dev.jsanca.argonaut.core.knowledge.KnowledgeRepository
import dev.jsanca.argonaut.core.knowledge.KnowledgeSearchRequest
import dev.jsanca.argonaut.core.knowledge.KnowledgeSearchResult
import dev.jsanca.argonaut.core.observability.ExecutionObserver
import dev.jsanca.argonaut.core.testing.TraceEventMetadata
import dev.jsanca.argonaut.core.trace.ExecutionEvent
import dev.jsanca.argonaut.core.trace.ExecutionEventType
import java.time.Instant
import java.util.UUID

/**
 * Per-run ToolSet for the Koog controlled evidence implementation.
 *
 * Implements [ToolSet] so that Koog discovers [@Tool][Tool]-annotated methods via reflection
 * and registers them with [ai.koog.agents.core.tools.ToolRegistry].
 *
 * Per-run state (observer, reads accumulator) is injected at construction time and captured in
 * the instance — the same pattern as LangChain4j 0.x (constructor injection) and distinct from
 * Spring AI (ToolContext side-channel) and LangGraph4j (node closure).
 *
 * Tool descriptions and output formats are identical to the Spring AI, LangChain4j, and
 * LangGraph4j counterparts — deliberate experiment constant.
 */
class KnowledgeTools(
    private val repository: KnowledgeRepository,
    private val observer: ExecutionObserver,
    private val reads: MutableList<DocumentContent>
) : ToolSet {

    @Tool
    @LLMDescription(
        "Search the knowledge corpus for documents relevant to the given query. " +
        "Returns a ranked list of source IDs, titles, and relevance scores. " +
        "Use this before readDocument to find which documents to retrieve."
    )
    fun searchKnowledge(@LLMDescription("search query") query: String): String {
        observer.record(event(ExecutionEventType.KNOWLEDGE_SEARCH_STARTED,
            mapOf(TraceEventMetadata.QUERY to query)))

        val response = repository.search(KnowledgeSearchRequest(query, 5))
        val results: List<KnowledgeSearchResult> = response.results()

        val sourceIds = results.joinToString(",") { it.sourceId() }

        observer.record(event(ExecutionEventType.KNOWLEDGE_SEARCH_COMPLETED,
            mapOf(TraceEventMetadata.QUERY to query, TraceEventMetadata.SOURCE_IDS to sourceIds)))

        if (results.isEmpty()) {
            return "No results found for: $query"
        }

        return results.joinToString("\n") { r ->
            "source_id=${r.sourceId()} | title=${r.title()} | score=${"%.2f".format(r.score())}"
        }
    }

    @Tool
    @LLMDescription(
        "Read the full content of a specific document by its source ID. " +
        "Returns the document content. Call searchKnowledge first to identify " +
        "relevant source IDs, then call this to retrieve full document text."
    )
    fun readDocument(@LLMDescription("source ID of the document to read") sourceId: String): String {
        observer.record(event(ExecutionEventType.DOCUMENT_READ_STARTED,
            mapOf(TraceEventMetadata.SOURCE_ID to sourceId)))

        val doc = repository.read(DocumentReference(sourceId, sourceId))
        reads.add(doc)

        observer.record(event(ExecutionEventType.DOCUMENT_READ_COMPLETED,
            mapOf(TraceEventMetadata.SOURCE_ID to sourceId)))
        observer.record(event(ExecutionEventType.EVIDENCE_RETRIEVED,
            mapOf(TraceEventMetadata.SOURCE_ID to sourceId,
                  TraceEventMetadata.EVIDENCE_ID to "ev-$sourceId")))

        return "source_id=$sourceId\n\n${doc.content()}"
    }

    private fun event(type: ExecutionEventType, metadata: Map<String, String>): ExecutionEvent =
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
