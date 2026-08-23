package dev.jsanca.argonaut.langchain4j.agent;

import dev.jsanca.argonaut.core.evidence.Evidence;
import dev.jsanca.argonaut.core.evidence.EvidenceKind;
import dev.jsanca.argonaut.core.experiment.ControlledLocalEvidencePrompt;
import dev.jsanca.argonaut.core.experiment.ExperimentRequest;
import dev.jsanca.argonaut.core.experiment.ExperimentResult;
import dev.jsanca.argonaut.core.knowledge.DocumentContent;
import dev.jsanca.argonaut.core.knowledge.KnowledgeRepository;
import dev.jsanca.argonaut.core.metrics.ExecutionMetrics;
import dev.jsanca.argonaut.core.observability.InMemoryExecutionObserver;
import dev.jsanca.argonaut.core.testing.TraceEventMetadata;
import dev.jsanca.argonaut.core.trace.ExecutionEvent;
import dev.jsanca.argonaut.core.trace.ExecutionEventType;
import dev.jsanca.argonaut.core.trace.ExecutionTrace;
import dev.jsanca.argonaut.langchain4j.knowledge.KnowledgeTools;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * LangChain4j agentic implementation of the Controlled Local Evidence RAG use case (UC-001).
 *
 * <p>Uses {@link AiServices} with a per-run {@link KnowledgeTools} instance to answer the
 * experiment question using only the controlled local evidence corpus. The model drives
 * retrieval by invoking {@code searchKnowledge} and {@code readDocument} tool calls.
 *
 * <p>Unlike Spring AI's {@code ControlledLocalEvidenceAgent} (which uses {@code ChatClient}
 * and {@code ToolContext} to pass per-run state to the tool), this implementation creates a
 * fresh {@link KnowledgeTools} per run with the observer and reads accumulator as constructor
 * arguments. The {@link AiServices} is also built per run. This is an intentional adapter
 * difference: LangChain4j's {@code @Tool} methods receive no side-channel context.
 *
 * <p>Trace events are emitted from two layers:
 * <ul>
 *   <li>{@link KnowledgeTools} — KNOWLEDGE_SEARCH_*, DOCUMENT_READ_*, EVIDENCE_RETRIEVED</li>
 *   <li>this agent — RUN_*, EVIDENCE_SELECTED, ANSWER_SYNTHESIZED</li>
 * </ul>
 *
 * <p>Model-call count is reported as 0 (unknown) because {@link AiServices} hides the
 * internal loop count. See the ARGONAUT-009 engineering report for the metric ambiguity note.
 */
@Service
public class ControlledLocalEvidenceAgent {

    static final String FRAMEWORK_ID = "langchain4j";

    private final ChatLanguageModel chatModel;
    private final KnowledgeRepository repository;

    public ControlledLocalEvidenceAgent(final ChatLanguageModel chatModel,
                                        final KnowledgeRepository repository) {

        this.chatModel = chatModel;
        this.repository = repository;
    }

    /**
     * LangChain4j AI service interface for the controlled evidence assistant.
     * The system message is supplied at runtime via {@code systemMessageProvider} so that
     * both Spring AI and LangChain4j consume the exact same prompt from
     * {@link ControlledLocalEvidencePrompt}.
     */
    interface KnowledgeAssistant {
        @SystemMessage(ControlledLocalEvidencePrompt.SYSTEM_PROMPT)
        String answer(String question);
    }

    public ExperimentResult run(final ExperimentRequest request) {

        final long startMs = System.currentTimeMillis();
        final var observer = new InMemoryExecutionObserver();
        final List<DocumentContent> reads = new CopyOnWriteArrayList<>();

        observer.record(agentEvent(ExecutionEventType.RUN_STARTED, Map.of()));

        try {
            final var tools = new KnowledgeTools(repository, observer, reads);

            final var assistant = AiServices.builder(KnowledgeAssistant.class)
                    .chatLanguageModel(chatModel)
                    .tools(tools)
                    .build();

            final String answer = assistant.answer(request.question());

            if (answer == null || answer.isBlank()) {
                throw new IllegalStateException("model produced no answer");
            }

            final List<Evidence> evidence = buildEvidence(reads);

            for (final Evidence ev : evidence) {
                observer.record(agentEvent(ExecutionEventType.EVIDENCE_SELECTED,
                        Map.of(TraceEventMetadata.SOURCE_ID, ev.sourceId(),
                               TraceEventMetadata.EVIDENCE_ID, ev.id())));
            }
            observer.record(agentEvent(ExecutionEventType.ANSWER_SYNTHESIZED, Map.of()));
            observer.record(agentEvent(ExecutionEventType.RUN_COMPLETED, Map.of()));

            final ExecutionTrace trace = observer.toTrace();
            final ExecutionMetrics metrics = buildMetrics(trace, evidence, System.currentTimeMillis() - startMs);

            return ExperimentResult.completed(request.runId(), FRAMEWORK_ID, answer, evidence, trace, metrics);

        } catch (Exception e) {
            observer.record(agentEvent(ExecutionEventType.RUN_FAILED, Map.of()));
            ExecutionTrace trace = observer.toTrace();
            long durationMs = System.currentTimeMillis() - startMs;
            ExecutionMetrics metrics = new ExecutionMetrics(durationMs, 0, 0, 0, 0, 0, 1);
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
                            "retrieved and read by LangChain4j agent");
                })
                .collect(Collectors.toList());
    }

    private ExecutionMetrics buildMetrics(ExecutionTrace trace, List<Evidence> evidence, long durationMs) {
        int searches = countEvents(trace, ExecutionEventType.KNOWLEDGE_SEARCH_COMPLETED);
        int docReads = countEvents(trace, ExecutionEventType.DOCUMENT_READ_COMPLETED);
        // modelCalls: AiServices hides the internal loop count; 0 = not reliably measurable
        return new ExecutionMetrics(durationMs, 0, 0, searches, docReads, evidence.size(), 0);
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
