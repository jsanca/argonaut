package dev.jsanca.argonaut.embabel.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.OperationContext;
import dev.jsanca.argonaut.core.evidence.Evidence;
import dev.jsanca.argonaut.core.evidence.EvidenceKind;
import dev.jsanca.argonaut.core.experiment.ControlledLocalEvidencePrompt;
import dev.jsanca.argonaut.core.knowledge.DocumentContent;
import dev.jsanca.argonaut.core.knowledge.DocumentReference;
import dev.jsanca.argonaut.core.knowledge.KnowledgeRepository;
import dev.jsanca.argonaut.core.knowledge.KnowledgeSearchRequest;
import dev.jsanca.argonaut.core.knowledge.KnowledgeSearchResult;
import dev.jsanca.argonaut.core.metrics.ExecutionMetrics;
import dev.jsanca.argonaut.core.observability.InMemoryExecutionObserver;
import dev.jsanca.argonaut.core.testing.TraceEventMetadata;
import dev.jsanca.argonaut.core.trace.ExecutionEvent;
import dev.jsanca.argonaut.core.trace.ExecutionEventType;
import dev.jsanca.argonaut.core.trace.ExecutionTrace;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Embabel implementation of the Controlled Local Evidence RAG use case (UC-001).
 *
 * <p>Uses Embabel's planner-driven model: a single {@link AchievesGoal} action performs
 * knowledge retrieval programmatically (Java code, not LLM tool calls) and then delegates
 * to the LLM only for synthesis. This is the natural Embabel pattern for RAG: the planner
 * drives retrieval deterministically; the LLM contributes intelligence at the synthesis step.
 *
 * <p>The action is testable without the full Embabel platform: callers can pass a
 * {@code FakeOperationContext} to mock the LLM synthesis while exercising real
 * {@link KnowledgeRepository} calls and trace-event emission.
 */
@Agent(
        description = "Answers questions using controlled local evidence; retrieval is planner-driven, synthesis is LLM-driven"
)
public class ControlledLocalEvidenceAgent {

    static final String FRAMEWORK_ID = "embabel";

    private final KnowledgeRepository knowledgeRepository;

    public ControlledLocalEvidenceAgent(KnowledgeRepository knowledgeRepository) {
        this.knowledgeRepository = knowledgeRepository;
    }

    @AchievesGoal(description = "Answer the experiment question using only controlled local evidence")
    @Action
    public EvidenceAnswer answer(EvidenceQuestion question, OperationContext context) {

        final long startMs = System.currentTimeMillis();
        final var observer = new InMemoryExecutionObserver();
        final var reads = new ArrayList<DocumentContent>();

        observer.record(event(ExecutionEventType.RUN_STARTED, Map.of()));

        // --- Retrieval phase (planner-driven, not LLM tool-call-driven) ---

        observer.record(event(ExecutionEventType.KNOWLEDGE_SEARCH_STARTED,
                Map.of(TraceEventMetadata.QUERY, question.question())));

        var searchResponse = knowledgeRepository.search(
                new KnowledgeSearchRequest(question.question(), 5));

        String foundIds = searchResponse.results().stream()
                .map(KnowledgeSearchResult::sourceId)
                .collect(Collectors.joining(","));

        observer.record(event(ExecutionEventType.KNOWLEDGE_SEARCH_COMPLETED,
                Map.of(TraceEventMetadata.SOURCE_IDS, foundIds,
                       TraceEventMetadata.QUERY, question.question())));

        // Read the top 3 documents from the search results
        for (KnowledgeSearchResult result : searchResponse.results().stream().limit(3).toList()) {
            observer.record(event(ExecutionEventType.DOCUMENT_READ_STARTED,
                    Map.of(TraceEventMetadata.SOURCE_ID, result.sourceId())));

            DocumentContent content = knowledgeRepository.read(
                    new DocumentReference(result.sourceId(), result.title()));
            reads.add(content);

            observer.record(event(ExecutionEventType.DOCUMENT_READ_COMPLETED,
                    Map.of(TraceEventMetadata.SOURCE_ID, result.sourceId())));

            observer.record(event(ExecutionEventType.EVIDENCE_RETRIEVED,
                    Map.of(TraceEventMetadata.SOURCE_ID, result.sourceId(),
                           TraceEventMetadata.EVIDENCE_ID, "ev-" + result.sourceId())));
        }

        // --- Synthesis phase (LLM-driven) ---

        String evidenceText = reads.stream()
                .map(doc -> "### Source: " + doc.reference().sourceId() + "\n\n" + doc.content())
                .collect(Collectors.joining("\n\n---\n\n"));

        String prompt = ControlledLocalEvidencePrompt.SYSTEM_PROMPT + "\n\n" +
                "Available evidence:\n\n" + evidenceText + "\n\n" +
                "Question: " + question.question();

        AnswerText answerText = context.ai()
                .withDefaultLlm()
                .createObject(prompt, AnswerText.class);

        String finalAnswer = (answerText != null && answerText.answer() != null)
                ? answerText.answer()
                : "";

        // --- Evidence and trace finalisation ---

        List<Evidence> evidences = buildEvidence(reads);

        for (Evidence evidence : evidences) {
            observer.record(event(ExecutionEventType.EVIDENCE_SELECTED,
                    Map.of(TraceEventMetadata.SOURCE_ID, evidence.sourceId(),
                           TraceEventMetadata.EVIDENCE_ID, evidence.id())));
        }
        observer.record(event(ExecutionEventType.ANSWER_SYNTHESIZED, Map.of()));
        observer.record(event(ExecutionEventType.RUN_COMPLETED, Map.of()));

        ExecutionTrace trace = observer.toTrace();
        ExecutionMetrics metrics = buildMetrics(trace, evidences, System.currentTimeMillis() - startMs);

        return new EvidenceAnswer(finalAnswer, evidences, trace, metrics);
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
                            "retrieved by Embabel planner action");
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

    private ExecutionEvent event(ExecutionEventType type, Map<String, String> metadata) {
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
