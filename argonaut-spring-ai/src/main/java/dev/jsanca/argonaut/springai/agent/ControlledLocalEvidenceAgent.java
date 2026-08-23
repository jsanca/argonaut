package dev.jsanca.argonaut.springai.agent;

import dev.jsanca.argonaut.core.evidence.Evidence;
import dev.jsanca.argonaut.core.evidence.EvidenceKind;
import dev.jsanca.argonaut.core.experiment.ControlledLocalEvidencePrompt;
import dev.jsanca.argonaut.core.experiment.ExperimentRequest;
import dev.jsanca.argonaut.core.experiment.ExperimentResult;
import dev.jsanca.argonaut.core.knowledge.DocumentContent;
import dev.jsanca.argonaut.core.metrics.ExecutionMetrics;
import dev.jsanca.argonaut.core.observability.InMemoryExecutionObserver;
import dev.jsanca.argonaut.core.testing.TraceEventMetadata;
import dev.jsanca.argonaut.core.trace.ExecutionEvent;
import dev.jsanca.argonaut.core.trace.ExecutionEventType;
import dev.jsanca.argonaut.core.trace.ExecutionTrace;
import dev.jsanca.argonaut.springai.knowledge.KnowledgeTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Spring AI agentic implementation of the Controlled Local Evidence RAG use case (UC-001).
 *
 * <p>Orchestrates a {@link ChatClient} with {@link KnowledgeTool} to answer the experiment
 * question using only the controlled local evidence corpus. Records all required
 * {@link ExecutionEventType} events and emits {@link TraceEventMetadata} keys so that
 * {@code ControlledLocalEvidenceContract} can verify the result.</p>
 *
 * <p>The model (configured via {@code spring.ai.openai.*}) drives retrieval through tool
 * calls. The agent records trace events from two layers:
 * <ul>
 *   <li>{@link KnowledgeTool} — emits KNOWLEDGE_SEARCH_*, DOCUMENT_READ_*, EVIDENCE_RETRIEVED</li>
 *   <li>this agent — emits RUN_*, EVIDENCE_SELECTED, ANSWER_SYNTHESIZED</li>
 * </ul>
 */
@Service
public class ControlledLocalEvidenceAgent {

    static final String FRAMEWORK_ID = "spring-ai";

    private final ChatClient chatClient;
    private final KnowledgeTool knowledgeTool;

    public ControlledLocalEvidenceAgent(final ChatClient chatClient,
                                        final KnowledgeTool knowledgeTool) {
        this.chatClient = chatClient;
        this.knowledgeTool = knowledgeTool;
    }

    public ExperimentResult run(final ExperimentRequest request) {

        final long startMs = System.currentTimeMillis();
        final var observer = new InMemoryExecutionObserver();
        final List<DocumentContent> reads = new CopyOnWriteArrayList<>();

        observer.record(agentEvent(ExecutionEventType.RUN_STARTED, Map.of()));

        try {

            final String answer = chatClient.prompt()
                    .system(ControlledLocalEvidencePrompt.SYSTEM_PROMPT)
                    .user(request.question())
                    .tools(knowledgeTool)
                    .toolContext(Map.of("observer", observer, "reads", reads))
                    .call()
                    .content();

            if (answer == null || answer.isBlank()) {
                throw new IllegalStateException("model produced no answer");
            }

            final List<Evidence> evidences = buildEvidence(reads);

            for (final Evidence evidence : evidences) {

                observer.record(agentEvent(ExecutionEventType.EVIDENCE_SELECTED,
                        Map.of(TraceEventMetadata.SOURCE_ID, evidence.sourceId(),
                               TraceEventMetadata.EVIDENCE_ID, evidence.id())));
            }
            observer.record(agentEvent(ExecutionEventType.ANSWER_SYNTHESIZED, Map.of()));
            observer.record(agentEvent(ExecutionEventType.RUN_COMPLETED, Map.of()));

            final ExecutionTrace trace = observer.toTrace();
            final ExecutionMetrics metrics = buildMetrics(trace, evidences, System.currentTimeMillis() - startMs);

            return ExperimentResult.completed(request.runId(), FRAMEWORK_ID, answer, evidences, trace, metrics);

        } catch (Exception e) {

            observer.record(agentEvent(ExecutionEventType.RUN_FAILED, Map.of()));
            final ExecutionTrace trace = observer.toTrace();
            final long durationMs = System.currentTimeMillis() - startMs;
            final ExecutionMetrics metrics = new ExecutionMetrics(durationMs, 0, 0, 0, 0, 0, 1);

            return ExperimentResult.failed(request.runId(), FRAMEWORK_ID, List.of(), trace, metrics);
        }
    }

    private List<Evidence> buildEvidence(List<DocumentContent> reads) {
        return reads.stream()
                .map(doc -> {
                    String sourceId = doc.reference().sourceId();
                    String body = doc.content();
                    String excerpt = body.substring(0, Math.min(200, body.length())).strip();
                    return new Evidence(
                            "ev-" + sourceId,
                            EvidenceKind.SUPPORTING,
                            sourceId,
                            sourceId,
                            excerpt,
                            1.0,
                            "retrieved and read by Spring AI agent");
                })
                .collect(Collectors.toList());
    }

    private ExecutionMetrics buildMetrics(ExecutionTrace trace, List<Evidence> evidence, long durationMs) {
        int searches = countEvents(trace, ExecutionEventType.KNOWLEDGE_SEARCH_COMPLETED);
        int docReads = countEvents(trace, ExecutionEventType.DOCUMENT_READ_COMPLETED);
        return new ExecutionMetrics(durationMs, 1, 0, searches, docReads, evidence.size(), 0);
    }

    private int countEvents(ExecutionTrace trace, ExecutionEventType type) {
        return (int) trace.events().stream().filter(e -> e.type() == type).count();
    }

    private ExecutionEvent agentEvent(ExecutionEventType type, Map<String, String> metadata) {
        return new ExecutionEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                type,
                FRAMEWORK_ID,
                type.name().toLowerCase(),
                type.name(),
                metadata,
                null);
    }
}
