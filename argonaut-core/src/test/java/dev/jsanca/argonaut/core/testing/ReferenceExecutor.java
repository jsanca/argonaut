package dev.jsanca.argonaut.core.testing;

import dev.jsanca.argonaut.core.evidence.Evidence;
import dev.jsanca.argonaut.core.evidence.EvidenceKind;
import dev.jsanca.argonaut.core.experiment.ExperimentRequest;
import dev.jsanca.argonaut.core.experiment.ExperimentResult;
import dev.jsanca.argonaut.core.knowledge.DocumentContent;
import dev.jsanca.argonaut.core.knowledge.DocumentReference;
import dev.jsanca.argonaut.core.knowledge.KnowledgeRepository;
import dev.jsanca.argonaut.core.knowledge.KnowledgeSearchRequest;
import dev.jsanca.argonaut.core.knowledge.KnowledgeSearchResponse;
import dev.jsanca.argonaut.core.knowledge.KnowledgeSearchResult;
import dev.jsanca.argonaut.core.knowledge.local.LocalKnowledgeRepository;
import dev.jsanca.argonaut.core.metrics.ExecutionMetrics;
import dev.jsanca.argonaut.core.observability.InMemoryExecutionObserver;
import dev.jsanca.argonaut.core.trace.ExecutionEvent;
import dev.jsanca.argonaut.core.trace.ExecutionEventType;
import dev.jsanca.argonaut.core.trace.ExecutionTrace;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A deterministic reference executor for TC-UC-001 — test fixture only.
 *
 * <p>This executor uses only the authoritative controlled corpus via
 * {@link LocalKnowledgeRepository#withDemoCorpus()}. It performs real search and read calls
 * against the corpus and constructs a deterministic final answer from the retrieved evidence.
 * It does not call any external model or service.
 *
 * <p>This is not a framework implementation. It is a test fixture that proves
 * {@link ControlledLocalEvidenceContract} passes when behavior is honest — i.e., when
 * evidence genuinely comes from {@code KnowledgeRepository} calls recorded in the trace.
 */
final class ReferenceExecutor implements ExperimentExecutor {

    private static final String ACTOR = "reference-executor";
    private static final int TOP_K = 3;

    @Override
    public ExperimentResult execute(ExperimentRequest request) {
        KnowledgeRepository repo = LocalKnowledgeRepository.withDemoCorpus();
        InMemoryExecutionObserver observer = new InMemoryExecutionObserver();
        ExecutionMetrics.Builder metrics = ExecutionMetrics.builder();
        long startMs = System.currentTimeMillis();

        observer.record(event(ExecutionEventType.RUN_STARTED, "run.started", "Experiment run started", Map.of()));

        // --- search 1: main question terms ---
        String query1 = "controlled evidence framework comparison";
        observer.record(event(ExecutionEventType.KNOWLEDGE_SEARCH_STARTED, "search.started",
                "Searching for: " + query1, Map.of(TraceEventMetadata.QUERY, query1)));
        KnowledgeSearchResponse response1 = repo.search(new KnowledgeSearchRequest(query1, TOP_K));
        metrics.incrementKnowledgeSearches();
        String sourceIds1 = joinSourceIds(response1);
        observer.record(event(ExecutionEventType.KNOWLEDGE_SEARCH_COMPLETED, "search.completed",
                "Search returned " + response1.results().size() + " results",
                Map.of(TraceEventMetadata.QUERY, query1, TraceEventMetadata.SOURCE_IDS, sourceIds1)));

        // --- search 2: secondary question terms ---
        String query2 = "controlled local evidence reproducible deterministic";
        observer.record(event(ExecutionEventType.KNOWLEDGE_SEARCH_STARTED, "search.started",
                "Searching for: " + query2, Map.of(TraceEventMetadata.QUERY, query2)));
        KnowledgeSearchResponse response2 = repo.search(new KnowledgeSearchRequest(query2, TOP_K));
        metrics.incrementKnowledgeSearches();
        String sourceIds2 = joinSourceIds(response2);
        observer.record(event(ExecutionEventType.KNOWLEDGE_SEARCH_COMPLETED, "search.completed",
                "Search returned " + response2.results().size() + " results",
                Map.of(TraceEventMetadata.QUERY, query2, TraceEventMetadata.SOURCE_IDS, sourceIds2)));

        // --- read top documents ---
        List<KnowledgeSearchResult> topResults = mergeTopResults(response1, response2, TOP_K);
        List<Evidence> selectedEvidence = new ArrayList<>();

        for (KnowledgeSearchResult searchResult : topResults) {
            String srcId = searchResult.sourceId();
            observer.record(event(ExecutionEventType.DOCUMENT_READ_STARTED, "read.started",
                    "Reading document: " + srcId, Map.of(TraceEventMetadata.SOURCE_ID, srcId)));
            DocumentContent content = repo.read(new DocumentReference(srcId, searchResult.title()));
            metrics.incrementDocumentReads();
            observer.record(event(ExecutionEventType.DOCUMENT_READ_COMPLETED, "read.completed",
                    "Read document: " + srcId,
                    Map.of(TraceEventMetadata.SOURCE_ID, srcId)));

            String evidenceId = "ev-" + srcId;
            observer.record(event(ExecutionEventType.EVIDENCE_RETRIEVED, "evidence.retrieved",
                    "Evidence candidate from: " + srcId,
                    Map.of(TraceEventMetadata.SOURCE_ID, srcId, TraceEventMetadata.EVIDENCE_ID, evidenceId)));

            Evidence evidence = new Evidence(
                    evidenceId,
                    EvidenceKind.SUPPORTING,
                    srcId,
                    searchResult.title(),
                    searchResult.excerpt(),
                    searchResult.score(),
                    "Supports the answer to the experiment question");
            selectedEvidence.add(evidence);

            observer.record(event(ExecutionEventType.EVIDENCE_SELECTED, "evidence.selected",
                    "Selected evidence from: " + srcId,
                    Map.of(TraceEventMetadata.SOURCE_ID, srcId, TraceEventMetadata.EVIDENCE_ID, evidenceId)));
        }

        // --- synthesize answer deterministically from evidence ---
        String finalAnswer = synthesizeAnswer(selectedEvidence);
        observer.record(event(ExecutionEventType.ANSWER_SYNTHESIZED, "answer.synthesized",
                "Answer synthesized from " + selectedEvidence.size() + " evidence items", Map.of()));

        long durationMs = System.currentTimeMillis() - startMs;
        metrics.durationMs(durationMs).addEvidence(selectedEvidence.size());

        observer.record(event(ExecutionEventType.RUN_COMPLETED, "run.completed",
                "Experiment run completed", Map.of()));

        ExecutionTrace trace = observer.toTrace();
        return ExperimentResult.completed(
                request.runId(), ACTOR, finalAnswer, selectedEvidence, trace, metrics.build());
    }

    private static String synthesizeAnswer(List<Evidence> evidence) {
        StringBuilder sb = new StringBuilder(
                "Argonaut uses controlled local evidence before external retrieval because:");
        for (Evidence e : evidence) {
            sb.append("\n\n[").append(e.sourceId()).append("] ").append(e.title())
              .append(": ").append(e.excerpt());
        }
        sb.append("\n\nControlled evidence ensures all frameworks receive identical source material, " +
                  "making framework orchestration the only variable under study.");
        return sb.toString();
    }

    private static String joinSourceIds(KnowledgeSearchResponse response) {
        return response.results().stream()
                .map(KnowledgeSearchResult::sourceId)
                .reduce((a, b) -> a + "," + b)
                .orElse("");
    }

    private static List<KnowledgeSearchResult> mergeTopResults(
            KnowledgeSearchResponse r1, KnowledgeSearchResponse r2, int topK) {
        List<KnowledgeSearchResult> merged = new ArrayList<>(r1.results());
        for (KnowledgeSearchResult r : r2.results()) {
            boolean alreadySeen = merged.stream().anyMatch(m -> m.sourceId().equals(r.sourceId()));
            if (!alreadySeen) merged.add(r);
        }
        merged.sort((a, b) -> Double.compare(b.score(), a.score()));
        return merged.subList(0, Math.min(topK, merged.size()));
    }

    private static ExecutionEvent event(ExecutionEventType type, String name,
                                        String summary, Map<String, String> metadata) {
        return new ExecutionEvent(UUID.randomUUID().toString(), Instant.now(), type,
                ACTOR, name, summary, metadata, null);
    }
}
