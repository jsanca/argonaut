package dev.jsanca.argonaut.core.knowledge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeSearchRequestTest {

    @Test
    void validRequest_createsSuccessfully() {
        var request = new KnowledgeSearchRequest("Java 25 features", 5);
        assertEquals("Java 25 features", request.query());
        assertEquals(5, request.topK());
    }

    @Test
    void rejectsBlankQuery() {
        assertThrows(IllegalArgumentException.class,
                () -> new KnowledgeSearchRequest("  ", 5));
    }

    @Test
    void rejectsNullQuery() {
        assertThrows(IllegalArgumentException.class,
                () -> new KnowledgeSearchRequest(null, 5));
    }

    @Test
    void rejectsZeroTopK() {
        assertThrows(IllegalArgumentException.class,
                () -> new KnowledgeSearchRequest("query", 0));
    }

    @Test
    void rejectsNegativeTopK() {
        assertThrows(IllegalArgumentException.class,
                () -> new KnowledgeSearchRequest("query", -1));
    }
}
