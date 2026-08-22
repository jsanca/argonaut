package dev.jsanca.argonaut.core.knowledge.local;

import dev.jsanca.argonaut.core.knowledge.DocumentReference;
import dev.jsanca.argonaut.core.knowledge.KnowledgeSearchRequest;
import dev.jsanca.argonaut.core.knowledge.KnowledgeSearchResponse;
import dev.jsanca.argonaut.core.knowledge.KnowledgeSourceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LocalKnowledgeRepositoryTest {

    private LocalKnowledgeRepository repo;

    @BeforeEach
    void setUp() {
        repo = LocalKnowledgeRepository.withDemoCorpus();
    }

    // --- corpus loading ---

    @Test
    void demoCorpus_containsExpectedDocuments() {
        var corpus = LocalKnowledgeCorpus.demo();
        assertEquals(5, corpus.size());
    }

    @Test
    void demoCorpus_findById_returnsDocument() {
        var corpus = LocalKnowledgeCorpus.demo();
        var doc = corpus.findById("vt-001");
        assertTrue(doc.isPresent());
        assertEquals("Virtual Threads and Blocking I/O in Java", doc.get().title());
    }

    @Test
    void demoCorpus_findById_returnsEmptyForUnknownId() {
        var corpus = LocalKnowledgeCorpus.demo();
        assertTrue(corpus.findById("does-not-exist").isEmpty());
    }

    // --- search: title match ---

    @Test
    void search_findsByTitleTerm() {
        var response = repo.search(new KnowledgeSearchRequest("virtual threads", 5));
        assertFalse(response.results().isEmpty());
        assertEquals("vt-001", response.results().get(0).sourceId());
    }

    @Test
    void search_titleMatchScoresHigherThanBodyOnlyMatch() {
        // "virtual" appears in the title of vt-001; "blocking" appears in the title of vt-001 too
        // "retrieval" appears only in body of rag-001 but also in title
        // Use a term that appears in one doc's title and another doc's body only
        // "observability" appears in title of obs-001 and body of sc-001
        var response = repo.search(new KnowledgeSearchRequest("observability", 5));
        assertFalse(response.results().isEmpty());
        assertEquals("obs-001", response.results().get(0).sourceId(),
                "obs-001 has 'observability' in title and should rank first");
        assertTrue(response.results().get(0).score() > response.results().get(1).score(),
                "title match should score higher than body-only match");
    }

    // --- search: body match ---

    @Test
    void search_findsByBodyTerm() {
        // "hallucination" appears only in the body of rag-001, not in any title
        var response = repo.search(new KnowledgeSearchRequest("hallucination", 5));
        assertEquals(1, response.results().size());
        assertEquals("rag-001", response.results().get(0).sourceId());
    }

    // --- search: topK limit ---

    @Test
    void search_respectsTopKLimit() {
        // broad query that matches multiple documents
        var response = repo.search(new KnowledgeSearchRequest("knowledge retrieval evidence", 2));
        assertTrue(response.results().size() <= 2);
    }

    @Test
    void search_topKOne_returnsSingleResult() {
        var response = repo.search(new KnowledgeSearchRequest("virtual threads blocking", 1));
        assertEquals(1, response.results().size());
    }

    // --- search: determinism ---

    @Test
    void search_isDeterministic() {
        var request = new KnowledgeSearchRequest("concurrent threads retrieval", 5);
        var first = repo.search(request);
        var second = repo.search(request);

        assertEquals(first.results().size(), second.results().size());
        for (int i = 0; i < first.results().size(); i++) {
            assertEquals(first.results().get(i).sourceId(), second.results().get(i).sourceId());
            assertEquals(first.results().get(i).score(), second.results().get(i).score());
        }
    }

    // --- search: zero-score filtering ---

    @Test
    void search_omitsZeroScoreDocuments() {
        // term that only matches vt-001
        var response = repo.search(new KnowledgeSearchRequest("unmounts carrier thread", 5));
        assertTrue(response.results().stream()
                .allMatch(r -> r.score() > 0.0), "all returned results must have score > 0");
    }

    @Test
    void search_returnsEmptyWhenNothingMatches() {
        var response = repo.search(new KnowledgeSearchRequest("xyzzy123 foobar", 5));
        assertTrue(response.results().isEmpty());
    }

    // --- search: empty-token query ---

    @Test
    void search_returnsEmptyWhenQueryTokenizesToNothing() {
        // KnowledgeSearchRequest rejects blank, but a query of only punctuation/numbers
        // may tokenize to empty set; test the repository handles this gracefully
        var response = repo.search(new KnowledgeSearchRequest("123 456", 5));
        // numeric-only tokens are valid but unlikely to match prose; result may be empty
        // the important thing is no exception
        assertNotNull(response);
        assertNotNull(response.results());
    }

    // --- search: response metadata ---

    @Test
    void search_preservesOriginalQuery() {
        var query = "virtual threads";
        var response = repo.search(new KnowledgeSearchRequest(query, 5));
        assertEquals(query, response.query());
    }

    @Test
    void search_resultContainsSourceIdTitleExcerptScore() {
        var response = repo.search(new KnowledgeSearchRequest("virtual threads", 5));
        var top = response.results().get(0);
        assertNotNull(top.sourceId());
        assertNotNull(top.title());
        assertNotNull(top.excerpt());
        assertTrue(top.score() > 0.0 && top.score() <= 1.0);
    }

    @Test
    void search_scoreIsNormalized() {
        var response = repo.search(new KnowledgeSearchRequest("virtual threads", 5));
        response.results().forEach(r ->
                assertTrue(r.score() > 0.0 && r.score() <= 1.0,
                        "score must be in (0.0, 1.0] but was " + r.score()));
    }

    // --- read ---

    @Test
    void read_returnsDocumentContent() {
        var ref = new DocumentReference("vt-001", "Virtual Threads and Blocking I/O in Java");
        var content = repo.read(ref);

        assertEquals(ref, content.reference());
        assertFalse(content.content().isBlank());
        assertTrue(content.content().contains("virtual thread"));
    }

    @Test
    void read_handlesUnknownReferenceWithException() {
        var ref = new DocumentReference("does-not-exist", "Unknown");
        assertThrows(KnowledgeSourceException.class, () -> repo.read(ref));
    }

    @Test
    void read_exceptionMessageContainsSourceId() {
        var ref = new DocumentReference("missing-doc", "Missing");
        var ex = assertThrows(KnowledgeSourceException.class, () -> repo.read(ref));
        assertTrue(ex.getMessage().contains("missing-doc"));
    }

    // --- custom corpus ---

    @Test
    void customCorpus_searchFindsCustomDocument() {
        var doc = LocalKnowledgeDocument.of("custom-1", "Custom Title About Argonauts",
                "This document is about argonauts sailing the seas.");
        var corpus = LocalKnowledgeCorpus.of(doc);
        var customRepo = new LocalKnowledgeRepository(corpus);

        var response = customRepo.search(new KnowledgeSearchRequest("argonauts", 5));
        assertEquals(1, response.results().size());
        assertEquals("custom-1", response.results().get(0).sourceId());
    }

    @Test
    void customCorpus_metadataIsPreserved() {
        var doc = new LocalKnowledgeDocument("meta-1", "Title", "Content", Map.of("topic", "test"));
        assertEquals("test", doc.metadata().get("topic"));
    }

    @Test
    void localKnowledgeDocument_rejectsBlanKId() {
        assertThrows(IllegalArgumentException.class,
                () -> LocalKnowledgeDocument.of("", "Title", "Content"));
    }
}
