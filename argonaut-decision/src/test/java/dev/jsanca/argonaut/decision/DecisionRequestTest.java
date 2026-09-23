package dev.jsanca.argonaut.decision;

import dev.jsanca.argonaut.decision.question.Choice;
import dev.jsanca.argonaut.decision.question.Noul;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DecisionRequestTest {

    enum Route { FAST, NORMAL }

    @Test
    void builder_noContext_rejected() {
        var builder = DecisionRequest.builder()
                .question(Choice.ofEnum("route", "Select", Route.class));
        assertThrows(NullPointerException.class, builder::build);
    }

    @Test
    void builder_noQuestions_rejected() {
        assertThrows(IllegalStateException.class,
                () -> DecisionRequest.builder().context(DecisionContext.of("ctx")).build());
    }

    @Test
    void builder_questions_immutable() {
        var q = Choice.ofEnum("route", "Select", Route.class);
        var request = DecisionRequest.builder()
                .context(DecisionContext.of("ctx"))
                .question(q)
                .build();
        assertThrows(UnsupportedOperationException.class,
                () -> ((List<?>) request.questions()).add(null));
    }

    @Test
    void builder_questionOrderPreserved() {
        var q1 = Choice.ofEnum("route", "Select route", Route.class);
        var q2 = Noul.of("flag", "Is flagged?");
        var request = DecisionRequest.builder()
                .context(DecisionContext.of("ctx"))
                .question(q1)
                .question(q2)
                .build();
        assertEquals(2, request.questions().size());
        assertEquals("route", request.questions().get(0).id());
        assertEquals("flag", request.questions().get(1).id());
    }

    @Test
    void decisionContext_nullState_rejected() {
        assertThrows(NullPointerException.class, () -> DecisionContext.of(null));
    }

    @Test
    void decisionContext_metadata_immutable() {
        var mutable = new java.util.HashMap<String, Object>();
        mutable.put("k", "v");
        var ctx = new DecisionContext("state", mutable);
        mutable.put("other", "extra");
        assertFalse(ctx.metadata().containsKey("other"));
    }
}
