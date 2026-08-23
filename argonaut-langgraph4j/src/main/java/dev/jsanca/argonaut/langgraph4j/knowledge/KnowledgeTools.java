package dev.jsanca.argonaut.langgraph4j.knowledge;

import dev.jsanca.argonaut.core.knowledge.DocumentContent;
import dev.jsanca.argonaut.core.knowledge.DocumentReference;
import dev.jsanca.argonaut.core.knowledge.KnowledgeRepository;
import dev.jsanca.argonaut.core.knowledge.KnowledgeSearchRequest;
import dev.jsanca.argonaut.core.knowledge.KnowledgeSearchResult;
import dev.jsanca.argonaut.core.observability.ExecutionObserver;
import dev.jsanca.argonaut.core.testing.TraceEventMetadata;
import dev.jsanca.argonaut.core.trace.ExecutionEvent;
import dev.jsanca.argonaut.core.trace.ExecutionEventType;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Per-run @Tool-annotated POJO for the LangGraph4j controlled evidence implementation.
 *
 * <p>Constructed per run with the {@link ExecutionObserver} and reads accumulator injected
 * at construction time. The {@link dev.langchain4j.agent.tool.Tool} annotations are
 * discovered by {@code LC4jToolService} via reflection.
 *
 * <p>The tool descriptions and output formats are identical to the Spring AI and LangChain4j
 * counterparts — a deliberate experiment constant. The per-run state injection mechanism is
 * LangGraph4j-specific: the object is captured in the tools-node closure rather than
 * passed through ToolContext (Spring AI) or rebuilt per AiServices run (LangChain4j 0.x).
 */
public class KnowledgeTools {

    private final KnowledgeRepository repository;
    private final ExecutionObserver observer;
    private final List<DocumentContent> reads;

    public KnowledgeTools(KnowledgeRepository repository,
                          ExecutionObserver observer,
                          List<DocumentContent> reads) {
        this.repository = repository;
        this.observer = observer;
        this.reads = reads;
    }

    @Tool("Search the knowledge corpus for documents relevant to the given query. " +
          "Returns a ranked list of source IDs, titles, and relevance scores. " +
          "Use this before readDocument to find which documents to retrieve.")
    public String searchKnowledge(@P("search query") String query) {

        observer.record(event(ExecutionEventType.KNOWLEDGE_SEARCH_STARTED,
                Map.of(TraceEventMetadata.QUERY, query)));

        var response = repository.search(new KnowledgeSearchRequest(query, 5));
        List<KnowledgeSearchResult> results = response.results();

        String sourceIds = results.stream()
                .map(KnowledgeSearchResult::sourceId)
                .collect(Collectors.joining(","));

        observer.record(event(ExecutionEventType.KNOWLEDGE_SEARCH_COMPLETED,
                Map.of(TraceEventMetadata.QUERY, query,
                       TraceEventMetadata.SOURCE_IDS, sourceIds)));

        if (results.isEmpty()) {
            return "No results found for: " + query;
        }

        return results.stream()
                .map(r -> "source_id=" + r.sourceId()
                        + " | title=" + r.title()
                        + " | score=" + String.format("%.2f", r.score()))
                .collect(Collectors.joining("\n"));
    }

    @Tool("Read the full content of a specific document by its source ID. " +
          "Returns the document content. Call searchKnowledge first to identify " +
          "relevant source IDs, then call this to retrieve full document text.")
    public String readDocument(@P("source ID of the document to read") String sourceId) {

        observer.record(event(ExecutionEventType.DOCUMENT_READ_STARTED,
                Map.of(TraceEventMetadata.SOURCE_ID, sourceId)));

        var doc = repository.read(new DocumentReference(sourceId, sourceId));
        reads.add(doc);

        observer.record(event(ExecutionEventType.DOCUMENT_READ_COMPLETED,
                Map.of(TraceEventMetadata.SOURCE_ID, sourceId)));
        observer.record(event(ExecutionEventType.EVIDENCE_RETRIEVED,
                Map.of(TraceEventMetadata.SOURCE_ID, sourceId,
                       TraceEventMetadata.EVIDENCE_ID, "ev-" + sourceId)));

        return "source_id=" + sourceId + "\n\n" + doc.content();
    }

    private ExecutionEvent event(ExecutionEventType type, Map<String, String> metadata) {
        return new ExecutionEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                type,
                "langgraph4j",
                type.name().toLowerCase(),
                type.name(),
                metadata,
                null);
    }
}
