package dev.jsanca.argonaut.core.knowledge.local;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * An immutable, in-memory collection of {@link LocalKnowledgeDocument} instances.
 *
 * <p><strong>Authoritative corpus:</strong> The controlled local evidence corpus is maintained as
 * Markdown files under {@code docs/knowledge/corpus/controlled-local-evidence/} and is loaded
 * into this class at runtime from the classpath path {@value #CONTROLLED_CORPUS_CLASSPATH}.
 *
 * <p>Use {@link #demo()} to obtain the authoritative corpus. Use {@link #of(LocalKnowledgeDocument...)}
 * to construct custom corpora for tests.
 *
 * <p>Duplicate source IDs within a corpus are rejected at construction time.
 */
public final class LocalKnowledgeCorpus {

    /**
     * Classpath path to the controlled local evidence corpus files.
     * Resolves from {@code argonaut-core/src/main/resources/knowledge/controlled-local-evidence/}.
     */
    public static final String CONTROLLED_CORPUS_CLASSPATH = "knowledge/controlled-local-evidence";

    private final List<LocalKnowledgeDocument> documents;

    private LocalKnowledgeCorpus(List<LocalKnowledgeDocument> documents) {
        this.documents = List.copyOf(documents);
    }

    /**
     * Construct a corpus from a list of documents. Duplicate source IDs are rejected.
     *
     * @throws IllegalArgumentException if any two documents share the same ID
     */
    public static LocalKnowledgeCorpus of(List<LocalKnowledgeDocument> documents) {
        Objects.requireNonNull(documents, "documents must not be null");
        Set<String> seen = new HashSet<>();
        for (LocalKnowledgeDocument doc : documents) {
            if (!seen.add(doc.id())) {
                throw new IllegalArgumentException("duplicate document id in corpus: " + doc.id());
            }
        }
        return new LocalKnowledgeCorpus(documents);
    }

    /** Construct a corpus from varargs documents. Duplicate source IDs are rejected. */
    public static LocalKnowledgeCorpus of(LocalKnowledgeDocument... documents) {
        return of(Arrays.asList(documents));
    }

    public List<LocalKnowledgeDocument> documents() {
        return documents;
    }

    public Optional<LocalKnowledgeDocument> findById(String id) {
        return documents.stream().filter(d -> d.id().equals(id)).findFirst();
    }

    public int size() {
        return documents.size();
    }

    /**
     * Returns the authoritative controlled corpus for all Argonaut framework implementations.
     *
     * <p>This corpus is loaded from Markdown files on the classpath at
     * {@value #CONTROLLED_CORPUS_CLASSPATH}. The Markdown files are the human-editable
     * source of truth; this method loads them on each call.
     *
     * <p>All four Argonaut framework implementations must use this corpus (or an equivalent
     * loaded from the same Markdown source) to satisfy the "same evidence" principle.
     *
     * <p><strong>Limitation:</strong> This method uses filesystem-based classpath loading
     * ({@code resource.toURI()}). It works when resources are on the local filesystem
     * (standard Maven layout) but will not work for resources inside JAR files. See
     * {@link MarkdownCorpusLoader} for the JAR limitation details.
     */
    public static LocalKnowledgeCorpus demo() {
        return MarkdownCorpusLoader.fromClasspath(
                CONTROLLED_CORPUS_CLASSPATH,
                LocalKnowledgeCorpus.class.getClassLoader());
    }
}
