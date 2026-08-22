package dev.jsanca.argonaut.core.knowledge.local;

import dev.jsanca.argonaut.core.knowledge.DocumentContent;
import dev.jsanca.argonaut.core.knowledge.DocumentReference;
import dev.jsanca.argonaut.core.knowledge.KnowledgeRepository;
import dev.jsanca.argonaut.core.knowledge.KnowledgeSearchRequest;
import dev.jsanca.argonaut.core.knowledge.KnowledgeSearchResponse;
import dev.jsanca.argonaut.core.knowledge.KnowledgeSearchResult;
import dev.jsanca.argonaut.core.knowledge.KnowledgeSourceException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A deterministic, in-memory {@link KnowledgeRepository} backed by a {@link LocalKnowledgeCorpus}.
 *
 * <h3>Search</h3>
 * <p>Uses simple lexical scoring. For each unique query term:</p>
 * <ul>
 *   <li>title match → 2.0 points</li>
 *   <li>content-only match → 1.0 point</li>
 * </ul>
 * <p>Score is normalized to [0.0, 1.0] by dividing raw points by
 * {@code queryTermCount × 2.0}. Documents with a score of zero are omitted.
 * Results are returned in descending score order; ties are broken by {@code sourceId}
 * ascending to ensure determinism.</p>
 *
 * <h3>Read</h3>
 * <p>Looks up a document by {@code DocumentReference.sourceId()}. Throws
 * {@link KnowledgeSourceException} when the document is not found.</p>
 *
 * <h3>What this is not</h3>
 * <p>This is not production-grade search. It is a stable, explainable foundation that
 * allows every Argonaut framework implementation to retrieve evidence from the same
 * controlled corpus without external dependencies. Lucene, Qdrant, and vector-based
 * retrieval are future extensions.</p>
 */
public final class LocalKnowledgeRepository implements KnowledgeRepository {

    private static final int EXCERPT_WINDOW = 250;
    private static final int EXCERPT_LEAD = 80;

    private final LocalKnowledgeCorpus corpus;

    public LocalKnowledgeRepository(LocalKnowledgeCorpus corpus) {
        this.corpus = Objects.requireNonNull(corpus, "corpus must not be null");
    }

    public static LocalKnowledgeRepository withDemoCorpus() {
        return new LocalKnowledgeRepository(LocalKnowledgeCorpus.demo());
    }

    @Override
    public KnowledgeSearchResponse search(KnowledgeSearchRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        Set<String> queryTerms = tokenize(request.query());
        if (queryTerms.isEmpty()) {
            return new KnowledgeSearchResponse(request.query(), List.of());
        }

        List<ScoredResult> scored = new ArrayList<>();
        for (LocalKnowledgeDocument doc : corpus.documents()) {
            double score = score(doc, queryTerms);
            if (score > 0.0) {
                String excerpt = excerpt(doc, queryTerms);
                scored.add(new ScoredResult(doc, score, excerpt));
            }
        }

        scored.sort(Comparator
                .comparingDouble(ScoredResult::score).reversed()
                .thenComparing(r -> r.doc().id()));

        List<KnowledgeSearchResult> results = scored.stream()
                .limit(request.topK())
                .map(r -> new KnowledgeSearchResult(
                        r.doc().id(),
                        r.doc().title(),
                        r.excerpt(),
                        r.score()))
                .collect(Collectors.toList());

        return new KnowledgeSearchResponse(request.query(), results);
    }

    @Override
    public DocumentContent read(DocumentReference reference) {
        Objects.requireNonNull(reference, "reference must not be null");

        return corpus.findById(reference.sourceId())
                .map(doc -> new DocumentContent(reference, doc.content()))
                .orElseThrow(() -> new KnowledgeSourceException(
                        "Document not found: " + reference.sourceId()));
    }

    // --- scoring ---

    private double score(LocalKnowledgeDocument doc, Set<String> queryTerms) {
        Set<String> titleTokens = tokenize(doc.title());
        Set<String> contentTokens = tokenize(doc.content());

        double raw = 0.0;
        for (String term : queryTerms) {
            if (titleTokens.contains(term)) {
                raw += 2.0;
            } else if (contentTokens.contains(term)) {
                raw += 1.0;
            }
        }
        return Math.min(1.0, raw / (queryTerms.size() * 2.0));
    }

    private String excerpt(LocalKnowledgeDocument doc, Set<String> queryTerms) {
        String content = doc.content();
        String lower = content.toLowerCase(Locale.ROOT);

        int firstMatch = queryTerms.stream()
                .mapToInt(lower::indexOf)
                .filter(idx -> idx >= 0)
                .min()
                .orElse(0);

        int start = Math.max(0, firstMatch - EXCERPT_LEAD);
        int end = Math.min(content.length(), start + EXCERPT_WINDOW);
        String excerpt = content.substring(start, end).trim();
        return start > 0 ? "…" + excerpt : excerpt;
    }

    private static Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) return Set.of();
        return java.util.Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(t -> !t.isEmpty())
                .collect(Collectors.toSet());
    }

    private record ScoredResult(LocalKnowledgeDocument doc, double score, String excerpt) {}
}
