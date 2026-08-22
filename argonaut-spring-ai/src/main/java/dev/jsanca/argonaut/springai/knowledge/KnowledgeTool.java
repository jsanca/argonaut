package dev.jsanca.argonaut.springai.knowledge;

import dev.jsanca.argonaut.core.knowledge.DocumentContent;
import dev.jsanca.argonaut.core.knowledge.DocumentReference;
import dev.jsanca.argonaut.core.knowledge.KnowledgeRepository;
import dev.jsanca.argonaut.core.knowledge.KnowledgeSearchRequest;
import dev.jsanca.argonaut.core.knowledge.KnowledgeSearchResult;
import dev.jsanca.argonaut.core.observability.ExecutionObserver;
import dev.jsanca.argonaut.core.testing.TraceEventMetadata;
import dev.jsanca.argonaut.core.trace.ExecutionEvent;
import dev.jsanca.argonaut.core.trace.ExecutionEventType;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Spring AI tool wrapper for {@link KnowledgeRepository}.
 *
 * <p>Each tool method records the required {@link TraceEventMetadata} keys so that
 * {@code ControlledLocalEvidenceContract} SC4 (evidence traceability) is satisfied.</p>
 *
 * <p>The tool context must carry two keys set by {@link dev.jsanca.argonaut.springai.agent.ControlledLocalEvidenceAgent}:
 * <ul>
 *   <li>{@code "observer"} — the per-run {@link ExecutionObserver}</li>
 *   <li>{@code "reads"} — a mutable {@link List}{@code <DocumentContent>} that accumulates read documents</li>
 * </ul>
 */
@Component
public class KnowledgeTool {

    private final KnowledgeRepository repository;

    public KnowledgeTool(KnowledgeRepository repository) {
        this.repository = repository;
    }

    @Tool(name = "searchKnowledge",
          description = "Search the controlled knowledge corpus for documents relevant to a query. " +
                        "Returns ranked results with source IDs, titles, and excerpts.")
    public String searchKnowledge(
            @ToolParam(description = "the search query") String query,
            ToolContext toolContext) {

        ExecutionObserver observer = observer(toolContext);

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

    @Tool(name = "readDocument",
          description = "Read the full content of a document from the knowledge corpus by its source ID. " +
                        "Use source IDs returned by searchKnowledge.")
    public String readDocument(
            @ToolParam(description = "the source document ID to read (e.g. exp-001)") String sourceId,
            ToolContext toolContext) {

        ExecutionObserver observer = observer(toolContext);
        List<DocumentContent> reads = reads(toolContext);

        observer.record(event(ExecutionEventType.DOCUMENT_READ_STARTED,
                Map.of(TraceEventMetadata.SOURCE_ID, sourceId)));

        DocumentContent content = repository.read(new DocumentReference(sourceId, sourceId));

        observer.record(event(ExecutionEventType.DOCUMENT_READ_COMPLETED,
                Map.of(TraceEventMetadata.SOURCE_ID, sourceId)));

        observer.record(event(ExecutionEventType.EVIDENCE_RETRIEVED,
                Map.of(TraceEventMetadata.SOURCE_ID, sourceId,
                       TraceEventMetadata.EVIDENCE_ID, "ev-" + sourceId)));

        if (reads != null) {
            reads.add(content);
        }

        return "source_id=" + sourceId +
               "\n\n" + content.content();
    }

    private ExecutionObserver observer(ToolContext ctx) {
        return (ExecutionObserver) ctx.getContext().get("observer");
    }

    @SuppressWarnings("unchecked")
    private List<DocumentContent> reads(ToolContext ctx) {
        return (List<DocumentContent>) ctx.getContext().get("reads");
    }

    private ExecutionEvent event(ExecutionEventType type, Map<String, String> metadata) {
        return new ExecutionEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                type,
                "KnowledgeTool",
                type.name().toLowerCase(),
                type.name(),
                metadata,
                null);
    }
}
