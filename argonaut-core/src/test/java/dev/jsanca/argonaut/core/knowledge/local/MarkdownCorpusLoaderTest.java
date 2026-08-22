package dev.jsanca.argonaut.core.knowledge.local;

import dev.jsanca.argonaut.core.knowledge.KnowledgeRepository;
import dev.jsanca.argonaut.core.knowledge.KnowledgeSearchRequest;
import dev.jsanca.argonaut.core.knowledge.KnowledgeSearchResponse;
import dev.jsanca.argonaut.core.knowledge.KnowledgeSearchResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownCorpusLoaderTest {

    private static final String CLASSPATH_DIR = "knowledge/controlled-local-evidence";

    // -------------------------------------------------------------------------
    // Classpath loading — full five-document corpus
    // -------------------------------------------------------------------------

    @Test
    void loadsAllFiveDocumentsFromClasspath() {
        LocalKnowledgeCorpus corpus = MarkdownCorpusLoader.fromClasspath(CLASSPATH_DIR);
        assertEquals(5, corpus.size());
    }

    @Test
    void documentIdsMatchExpectedControlledCorpus() {
        LocalKnowledgeCorpus corpus = MarkdownCorpusLoader.fromClasspath(CLASSPATH_DIR);
        List<String> ids = corpus.documents().stream().map(LocalKnowledgeDocument::id).toList();
        assertTrue(ids.contains("exp-001"), "missing exp-001");
        assertTrue(ids.contains("rag-001"), "missing rag-001");
        assertTrue(ids.contains("obs-001"), "missing obs-001");
        assertTrue(ids.contains("vt-001"),  "missing vt-001");
        assertTrue(ids.contains("sc-001"),  "missing sc-001");
    }

    @Test
    void titlesMatchUseCase() {
        LocalKnowledgeCorpus corpus = MarkdownCorpusLoader.fromClasspath(CLASSPATH_DIR);
        assertTitle(corpus, "exp-001", "Controlled Evidence in AI Framework Experiments");
        assertTitle(corpus, "rag-001", "Retrieval-Augmented Generation: Core Concepts");
        assertTitle(corpus, "obs-001", "Observability Fundamentals for Agentic Systems");
        assertTitle(corpus, "vt-001",  "Virtual Threads and Blocking I/O in Java");
        assertTitle(corpus, "sc-001",  "Structured Concurrency in the JVM");
    }

    @Test
    void metadataIncludesTopicAndVersion() {
        LocalKnowledgeCorpus corpus = MarkdownCorpusLoader.fromClasspath(CLASSPATH_DIR);
        LocalKnowledgeDocument exp = findById(corpus, "exp-001");
        assertEquals("experiment-design", exp.metadata().get("topic"));
        assertEquals("1", exp.metadata().get("version"));
    }

    @Test
    void bodyContentIsNotBlankForAnyDocument() {
        LocalKnowledgeCorpus corpus = MarkdownCorpusLoader.fromClasspath(CLASSPATH_DIR);
        for (LocalKnowledgeDocument doc : corpus.documents()) {
            assertFalse(doc.content().isBlank(), "blank content in: " + doc.id());
        }
    }

    @Test
    void documentsAreReturnedInDeterministicIdOrder() {
        LocalKnowledgeCorpus corpus = MarkdownCorpusLoader.fromClasspath(CLASSPATH_DIR);
        List<String> ids = corpus.documents().stream().map(LocalKnowledgeDocument::id).toList();
        List<String> sorted = ids.stream().sorted().toList();
        assertEquals(sorted, ids, "corpus is not in ascending id order");
    }

    // -------------------------------------------------------------------------
    // Search integration
    // -------------------------------------------------------------------------

    @Test
    void loadedCorpusCanBeSearchedThroughLocalKnowledgeRepository() {
        LocalKnowledgeCorpus corpus = MarkdownCorpusLoader.fromClasspath(CLASSPATH_DIR);
        KnowledgeRepository repo = new LocalKnowledgeRepository(corpus);
        KnowledgeSearchResponse response = repo.search(
                new KnowledgeSearchRequest("controlled evidence experiment", 5));
        assertFalse(response.results().isEmpty(), "expected at least one result");
    }

    @Test
    void controlledEvidenceQueryRanksExpOneFirst() {
        LocalKnowledgeCorpus corpus = MarkdownCorpusLoader.fromClasspath(CLASSPATH_DIR);
        KnowledgeRepository repo = new LocalKnowledgeRepository(corpus);
        KnowledgeSearchResponse response = repo.search(
                new KnowledgeSearchRequest("controlled evidence experiment", 5));
        String topId = response.results().getFirst().sourceId();
        assertEquals("exp-001", topId, "exp-001 should rank first for controlled-evidence query");
    }

    @Test
    void observabilityQueryRetrievesObsOne() {
        LocalKnowledgeCorpus corpus = MarkdownCorpusLoader.fromClasspath(CLASSPATH_DIR);
        KnowledgeRepository repo = new LocalKnowledgeRepository(corpus);
        KnowledgeSearchResponse response = repo.search(
                new KnowledgeSearchRequest("observability agentic systems trace events", 5));
        List<String> ids = response.results().stream()
                .map(KnowledgeSearchResult::sourceId)
                .toList();
        assertTrue(ids.contains("obs-001"), "obs-001 must appear for observability query; got: " + ids);
    }

    // -------------------------------------------------------------------------
    // Error handling — malformed frontmatter
    // -------------------------------------------------------------------------

    @Test
    void missingIdFieldThrowsMarkdownParseException(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("bad.md");
        Files.writeString(file, """
                ---
                title: Some Title
                topic: test
                ---

                Body content here.
                """, StandardCharsets.UTF_8);
        MarkdownParseException ex = assertThrows(MarkdownParseException.class,
                () -> MarkdownCorpusLoader.fromDirectory(dir));
        assertTrue(ex.getMessage().contains("id"), "message should mention 'id': " + ex.getMessage());
    }

    @Test
    void missingTitleFieldThrowsMarkdownParseException(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("bad.md");
        Files.writeString(file, """
                ---
                id: test-001
                topic: test
                ---

                Body content here.
                """, StandardCharsets.UTF_8);
        MarkdownParseException ex = assertThrows(MarkdownParseException.class,
                () -> MarkdownCorpusLoader.fromDirectory(dir));
        assertTrue(ex.getMessage().contains("title"), "message should mention 'title': " + ex.getMessage());
    }

    @Test
    void missingFrontmatterDelimiterThrowsMarkdownParseException(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("bad.md");
        Files.writeString(file, "# No frontmatter here\n\nJust a body.", StandardCharsets.UTF_8);
        assertThrows(MarkdownParseException.class, () -> MarkdownCorpusLoader.fromDirectory(dir));
    }

    // -------------------------------------------------------------------------
    // Determinism — repeated loads produce the same ordering
    // -------------------------------------------------------------------------

    @Test
    void repeatedLoadsProduceIdenticalOrdering() {
        LocalKnowledgeCorpus first  = MarkdownCorpusLoader.fromClasspath(CLASSPATH_DIR);
        LocalKnowledgeCorpus second = MarkdownCorpusLoader.fromClasspath(CLASSPATH_DIR);
        List<String> firstIds  = first.documents().stream().map(LocalKnowledgeDocument::id).toList();
        List<String> secondIds = second.documents().stream().map(LocalKnowledgeDocument::id).toList();
        assertEquals(firstIds, secondIds, "ordering changed between loads");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static LocalKnowledgeDocument findById(LocalKnowledgeCorpus corpus, String id) {
        return corpus.findById(id)
                .orElseThrow(() -> new AssertionError("document not found: " + id));
    }

    private static void assertTitle(LocalKnowledgeCorpus corpus, String id, String expectedTitle) {
        LocalKnowledgeDocument doc = findById(corpus, id);
        assertEquals(expectedTitle, doc.title(), "wrong title for " + id);
    }
}
