package dev.jsanca.argonaut.core.knowledge.local;

import dev.jsanca.argonaut.core.knowledge.KnowledgeSearchRequest;
import dev.jsanca.argonaut.core.knowledge.KnowledgeSearchResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Invariant tests for the authoritative controlled local evidence corpus.
 *
 * <p>These tests lock the stable IDs, titles, ordering, and retrieval behavior of the
 * Markdown-backed corpus. A failure here means either the corpus documents changed in a
 * breaking way or the loader deviates from the authoritative source.
 *
 * <p>The authoritative corpus is the Markdown files under
 * {@value LocalKnowledgeCorpus#CONTROLLED_CORPUS_CLASSPATH} on the classpath.
 * {@code LocalKnowledgeCorpus.demo()} loads from that same source.
 */
class CorpusInvariantsTest {

    private static final String CLASSPATH_DIR = LocalKnowledgeCorpus.CONTROLLED_CORPUS_CLASSPATH;

    private static final Set<String> EXPECTED_IDS =
            Set.of("exp-001", "rag-001", "obs-001", "vt-001", "sc-001");

    // -------------------------------------------------------------------------
    // Stable ID and title invariants
    // -------------------------------------------------------------------------

    @Test
    void authoritative_corpus_has_exactly_five_documents() {
        assertEquals(5, loadMarkdown().size());
    }

    @Test
    void authoritative_corpus_has_exactly_the_expected_stable_ids() {
        Set<String> actualIds = Set.copyOf(
                loadMarkdown().documents().stream().map(LocalKnowledgeDocument::id).toList());
        assertEquals(EXPECTED_IDS, actualIds,
                "corpus IDs must be exactly {exp-001, rag-001, obs-001, vt-001, sc-001}");
    }

    @Test
    void authoritative_corpus_titles_match_use_case_expectations() {
        LocalKnowledgeCorpus corpus = loadMarkdown();
        assertTitle(corpus, "exp-001", "Controlled Evidence in AI Framework Experiments");
        assertTitle(corpus, "rag-001", "Retrieval-Augmented Generation: Core Concepts");
        assertTitle(corpus, "obs-001", "Observability Fundamentals for Agentic Systems");
        assertTitle(corpus, "vt-001",  "Virtual Threads and Blocking I/O in Java");
        assertTitle(corpus, "sc-001",  "Structured Concurrency in the JVM");
    }

    @Test
    void authoritative_corpus_is_ordered_ascending_by_id() {
        LocalKnowledgeCorpus corpus = loadMarkdown();
        List<String> ids = corpus.documents().stream().map(LocalKnowledgeDocument::id).toList();
        assertEquals(ids.stream().sorted().toList(), ids,
                "corpus must be in ascending source ID order");
    }

    // -------------------------------------------------------------------------
    // demo() parity — Option A: demo() loads the Markdown corpus directly
    // -------------------------------------------------------------------------

    @Test
    void demo_returns_the_markdown_backed_corpus_with_same_ids() {
        LocalKnowledgeCorpus fromDemo   = LocalKnowledgeCorpus.demo();
        LocalKnowledgeCorpus fromLoader = loadMarkdown();
        List<String> demoIds   = fromDemo.documents().stream().map(LocalKnowledgeDocument::id).toList();
        List<String> loaderIds = fromLoader.documents().stream().map(LocalKnowledgeDocument::id).toList();
        assertEquals(loaderIds, demoIds,
                "demo() must return the same document IDs as MarkdownCorpusLoader in the same order");
    }

    @Test
    void demo_returns_the_markdown_backed_corpus_with_same_titles() {
        LocalKnowledgeCorpus fromDemo   = LocalKnowledgeCorpus.demo();
        LocalKnowledgeCorpus fromLoader = loadMarkdown();
        for (LocalKnowledgeDocument demoDoc : fromDemo.documents()) {
            LocalKnowledgeDocument loaderDoc = fromLoader.findById(demoDoc.id()).orElseThrow(
                    () -> new AssertionError("loader missing id: " + demoDoc.id()));
            assertEquals(loaderDoc.title(), demoDoc.title(),
                    "title mismatch for id: " + demoDoc.id());
        }
    }

    @Test
    void demo_returns_the_markdown_backed_corpus_with_same_body_content() {
        LocalKnowledgeCorpus fromDemo   = LocalKnowledgeCorpus.demo();
        LocalKnowledgeCorpus fromLoader = loadMarkdown();
        for (LocalKnowledgeDocument demoDoc : fromDemo.documents()) {
            LocalKnowledgeDocument loaderDoc = fromLoader.findById(demoDoc.id()).orElseThrow(
                    () -> new AssertionError("loader missing id: " + demoDoc.id()));
            assertEquals(normalizeWhitespace(loaderDoc.content()), normalizeWhitespace(demoDoc.content()),
                    "body content mismatch for id: " + demoDoc.id());
        }
    }

    // -------------------------------------------------------------------------
    // Duplicate ID rejection
    // -------------------------------------------------------------------------

    @Test
    void corpus_rejects_duplicate_ids_via_of_list() {
        LocalKnowledgeDocument doc = LocalKnowledgeDocument.of("dup-001", "Title", "Content");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> LocalKnowledgeCorpus.of(List.of(doc, doc)));
        assertTrue(ex.getMessage().contains("dup-001"),
                "exception message should name the duplicate id: " + ex.getMessage());
    }

    @Test
    void corpus_rejects_duplicate_ids_via_of_varargs() {
        LocalKnowledgeDocument doc = LocalKnowledgeDocument.of("dup-002", "Title", "Content");
        assertThrows(IllegalArgumentException.class, () -> LocalKnowledgeCorpus.of(doc, doc));
    }

    @Test
    void markdown_loader_rejects_duplicate_ids_in_directory(@TempDir Path dir) throws IOException {
        // Two files with the same id frontmatter field
        writeMarkdown(dir, "aaa.md", "dup-003", "Title A", "Body A");
        writeMarkdown(dir, "bbb.md", "dup-003", "Title B", "Body B");
        // The duplicate is caught when LocalKnowledgeCorpus.of() is called inside the loader
        assertThrows(IllegalArgumentException.class,
                () -> MarkdownCorpusLoader.fromDirectory(dir));
    }

    // -------------------------------------------------------------------------
    // Empty directory
    // -------------------------------------------------------------------------

    @Test
    void markdown_loader_rejects_empty_directory(@TempDir Path dir) {
        MarkdownParseException ex = assertThrows(MarkdownParseException.class,
                () -> MarkdownCorpusLoader.fromDirectory(dir));
        assertTrue(ex.getMessage().contains("no .md files"),
                "exception message should mention missing files: " + ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // Blank body rejection
    // -------------------------------------------------------------------------

    @Test
    void markdown_loader_rejects_blank_body(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("blank.md"),
                "---\nid: blank-001\ntitle: Blank\n---\n   \n", StandardCharsets.UTF_8);
        MarkdownParseException ex = assertThrows(MarkdownParseException.class,
                () -> MarkdownCorpusLoader.fromDirectory(dir));
        assertTrue(ex.getMessage().toLowerCase().contains("blank"),
                "exception message should mention blank body: " + ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // Golden query tests — exp-001 must be retrievable on mission-relevant queries
    // -------------------------------------------------------------------------

    @Test
    void golden_query_controlled_local_evidence_ranks_exp001_first() {
        assertExp001First("controlled local evidence");
    }

    @Test
    void golden_query_controlled_evidence_framework_comparison_ranks_exp001_first() {
        assertExp001First("controlled evidence framework comparison");
    }

    @Test
    void golden_query_why_controlled_evidence_before_web_search_includes_exp001() {
        assertExp001Present("why controlled evidence before web search");
    }

    @Test
    void golden_query_same_evidence_different_frameworks_includes_exp001() {
        assertExp001Present("same evidence different frameworks");
    }

    // -------------------------------------------------------------------------
    // Noisy query characterization
    // -------------------------------------------------------------------------

    @Test
    void noisy_stop_word_query_returns_all_documents_as_non_discriminative_noise() {
        // "the a and or of" are common English stop words with no semantic meaning.
        // The tokenizer has no stop-word filter, so all docs score on body token matches.
        // This test documents the known limitation: ranking is non-discriminative noise
        // for stop-word-only queries. Do not treat this as a search quality target.
        KnowledgeSearchResponse response = repo().search(
                new KnowledgeSearchRequest("the a and or of", 5));
        assertEquals(5, response.results().size(),
                "all five docs score on common stop words — ranking is non-discriminative");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static LocalKnowledgeCorpus loadMarkdown() {
        return MarkdownCorpusLoader.fromClasspath(CLASSPATH_DIR);
    }

    private static LocalKnowledgeRepository repo() {
        return new LocalKnowledgeRepository(loadMarkdown());
    }

    private static void assertTitle(LocalKnowledgeCorpus corpus, String id, String expectedTitle) {
        LocalKnowledgeDocument doc = corpus.findById(id)
                .orElseThrow(() -> new AssertionError("document not found: " + id));
        assertEquals(expectedTitle, doc.title(), "wrong title for " + id);
    }

    private static void assertExp001First(String query) {
        KnowledgeSearchResponse response = repo().search(new KnowledgeSearchRequest(query, 5));
        assertFalse(response.results().isEmpty(), "query returned no results: " + query);
        assertEquals("exp-001", response.results().getFirst().sourceId(),
                "exp-001 must rank first for query: " + query
                + " — actual ranking: " + response.results().stream()
                        .map(r -> r.sourceId() + "(" + r.score() + ")").toList());
    }

    private static void assertExp001Present(String query) {
        KnowledgeSearchResponse response = repo().search(new KnowledgeSearchRequest(query, 5));
        boolean found = response.results().stream()
                .anyMatch(r -> "exp-001".equals(r.sourceId()));
        assertTrue(found,
                "exp-001 must appear in results for query: " + query
                + " — actual: " + response.results().stream().map(r -> r.sourceId()).toList());
    }

    private static String normalizeWhitespace(String s) {
        return s.strip().replaceAll("\\s+", " ");
    }

    private static void writeMarkdown(Path dir, String filename, String id, String title, String body)
            throws IOException {
        Files.writeString(dir.resolve(filename),
                "---\nid: " + id + "\ntitle: " + title + "\n---\n\n" + body + "\n",
                StandardCharsets.UTF_8);
    }
}
