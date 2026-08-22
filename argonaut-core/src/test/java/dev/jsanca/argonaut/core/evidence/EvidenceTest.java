package dev.jsanca.argonaut.core.evidence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EvidenceTest {

    @Test
    void validEvidence_createsSuccessfully() {
        var e = new Evidence("e1", EvidenceKind.SUPPORTING, "doc-1", "Title", "excerpt", 0.95, "for answer");
        assertEquals("e1", e.id());
        assertEquals(EvidenceKind.SUPPORTING, e.kind());
        assertEquals(0.95, e.score());
    }

    @Test
    void rejectsInfiniteScore() {
        assertThrows(IllegalArgumentException.class,
                () -> new Evidence("e1", EvidenceKind.NEUTRAL, "doc-1", null, null, Double.POSITIVE_INFINITY, null));
    }

    @Test
    void rejectsNaNScore() {
        assertThrows(IllegalArgumentException.class,
                () -> new Evidence("e1", EvidenceKind.NEUTRAL, "doc-1", null, null, Double.NaN, null));
    }

    @Test
    void zeroScore_isValid() {
        assertDoesNotThrow(() -> new Evidence("e1", EvidenceKind.UNKNOWN, "doc-1", null, null, 0.0, null));
    }

    @Test
    void allEvidenceKindsAreFrameworkNeutral() {
        for (var kind : EvidenceKind.values()) {
            assertNotNull(kind.name());
        }
    }
}
