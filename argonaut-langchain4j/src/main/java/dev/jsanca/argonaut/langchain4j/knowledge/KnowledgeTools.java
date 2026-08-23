package dev.jsanca.argonaut.langchain4j.knowledge;

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
 * LangChain4j tool implementation wrapping {@link KnowledgeRepository}.
 *
 * <p>Instantiated per run by {@link dev.jsanca.argonaut.langchain4j.agent.ControlledLocalEvidenceAgent}
 * so that the {@link ExecutionObserver} and reads accumulator are scoped to a single experiment
 * execution. This avoids the need for thread-locals or shared mutable state.</p>
 *
 * <p>Unlike Spring AI's {@code KnowledgeTool} (which receives per-run state via
 * {@code ToolContext}), LangChain4j passes no side-channel context to {@code @Tool} methods.
 * Per-run state is therefore carried as constructor-injected instance fields — a framework
 * adapter difference, not a domain difference. Both tools ultimately delegate to the same
 * {@link KnowledgeRepository}.</p>
 *
 * <p>Each method populates {@link TraceEventMetadata} keys required by the SC4 anti-gaming
 * assertion in {@code ControlledLocalEvidenceContract}.</p>
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

    @Tool("Search the controlled knowledge corpus for documents relevant to a query. " +
          "Returns ranked results with source IDs, titles, and excerpts.")
    public String searchKnowledge(@P("the search query") String query) {
        observer.record(event(ExecutionEventType.KNOWLEDGE_SEARCH_STARTED,
                Map.of(TraceEventMetadata.QUERY, query)));

        var response = repository.search(new KnowledgeSearchRequest(query, 5));

        String sourceIds = response.results().stream()
                .map(KnowledgeSearchResult::sourceId)
                .collect(Collectors.joining(","));

        observer.record(event(ExecutionEventType.KNOWLEDGE_SEARCH_COMPLETED,
                Map.of(TraceEventMetadata.SOURCE_IDS, sourceIds,
                       TraceEventMetadata.QUERY, query)));

        if (response.results().isEmpty()) {
            return "No results found for: " + query;
        }

        return response.results().stream()
                .map(r -> "source_id=" + r.sourceId() +
                          " | title=" + r.title() +
                          " | score=" + String.format("%.2f", r.score()) +
                          "\n" + r.excerpt())
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    @Tool("Read the full content of a document from the knowledge corpus by its source ID. " +
          "Use source IDs returned by searchKnowledge.")
    public String readDocument(@P("the source document ID to read (e.g. exp-001)") String sourceId) {
        observer.record(event(ExecutionEventType.DOCUMENT_READ_STARTED,
                Map.of(TraceEventMetadata.SOURCE_ID, sourceId)));

        DocumentContent content = repository.read(new DocumentReference(sourceId, sourceId));

        observer.record(event(ExecutionEventType.DOCUMENT_READ_COMPLETED,
                Map.of(TraceEventMetadata.SOURCE_ID, sourceId)));

        observer.record(event(ExecutionEventType.EVIDENCE_RETRIEVED,
                Map.of(TraceEventMetadata.SOURCE_ID, sourceId,
                       TraceEventMetadata.EVIDENCE_ID, "ev-" + sourceId)));

        reads.add(content);

        return "source_id=" + sourceId + "\n\n" + content.content();
    }

    private ExecutionEvent event(ExecutionEventType type, Map<String, String> metadata) {
        return new ExecutionEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                type,
                "KnowledgeTools",
                type.name().toLowerCase(),
                type.name(),
                metadata,
                null);
    }
}
