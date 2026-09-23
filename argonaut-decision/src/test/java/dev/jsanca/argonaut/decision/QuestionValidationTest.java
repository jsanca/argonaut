package dev.jsanca.argonaut.decision;

import dev.jsanca.argonaut.decision.question.Choice;
import dev.jsanca.argonaut.decision.question.Noul;
import dev.jsanca.argonaut.decision.question.Score;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuestionValidationTest {

    enum Route { FAST, NORMAL }

    // Choice

    @Test
    void choice_ofEnum_candidatesMatchEnumConstants() {
        var choice = Choice.ofEnum("q", "Select", Route.class);
        assertEquals(List.of(Route.FAST, Route.NORMAL), choice.candidates());
    }

    @Test
    void choice_blankId_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> Choice.ofStrings("  ", "instructions", List.of("a")));
    }

    @Test
    void choice_emptyCandidates_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> Choice.ofStrings("q", "instructions", List.of()));
    }

    @Test
    void choice_candidates_immutable() {
        var mutable = new java.util.ArrayList<>(List.of("a", "b"));
        var choice = Choice.ofStrings("q", "instructions", mutable);
        mutable.add("c");
        assertEquals(2, choice.candidates().size());
    }

    // Score

    @Test
    void score_singleEntryScale_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> Score.of("q", "instructions", List.of("only")));
    }

    @Test
    void score_scale_immutable() {
        var mutable = new java.util.ArrayList<>(List.of("LOW", "HIGH"));
        var score = Score.of("q", "instructions", mutable);
        mutable.add("EXTREME");
        assertEquals(2, score.scale().size());
    }

    @Test
    void score_orderPreserved() {
        var scale = List.of("LOW", "MEDIUM", "HIGH");
        var score = Score.of("q", "instructions", scale);
        assertEquals(scale, score.scale());
    }

    // Noul

    @Test
    void noul_blankInstructions_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> Noul.of("q", "   "));
    }

    @Test
    void noul_factoryMethod_roundTrips() {
        var noul = Noul.of("flag", "Is this flagged?");
        assertEquals("flag", noul.id());
        assertEquals("Is this flagged?", noul.instructions());
    }
}
