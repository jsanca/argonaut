package dev.jsanca.argonaut.core.observability;

import dev.jsanca.argonaut.core.trace.ExecutionEvent;
import dev.jsanca.argonaut.core.trace.ExecutionEventType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CompositeExecutionObserverTest {

    private static ExecutionEvent event(String id, ExecutionEventType type) {
        return new ExecutionEvent(id, Instant.now(), type, "test", id, id, Map.of(), null);
    }

    @Test
    void forwardsEventToAllDelegates() {
        var first = new InMemoryExecutionObserver();
        var second = new InMemoryExecutionObserver();
        var composite = CompositeExecutionObserver.of(first, second);

        composite.record(event("e1", ExecutionEventType.RUN_STARTED));

        assertEquals(1, first.events().size());
        assertEquals(1, second.events().size());
        assertEquals("e1", first.events().get(0).id());
        assertEquals("e1", second.events().get(0).id());
    }

    @Test
    void forwardsMultipleEventsInOrder() {
        var observer = new InMemoryExecutionObserver();
        var composite = CompositeExecutionObserver.of(observer);

        composite.record(event("e1", ExecutionEventType.RUN_STARTED));
        composite.record(event("e2", ExecutionEventType.RUN_COMPLETED));

        assertEquals(2, observer.events().size());
        assertEquals("e1", observer.events().get(0).id());
        assertEquals("e2", observer.events().get(1).id());
    }

    @Test
    void worksWithNoDelegates() {
        var composite = CompositeExecutionObserver.of();
        assertDoesNotThrow(() -> composite.record(event("e1", ExecutionEventType.RUN_STARTED)));
    }

    @Test
    void ofVarargs_createsCorrectly() {
        var a = new InMemoryExecutionObserver();
        var b = new InMemoryExecutionObserver();
        var composite = CompositeExecutionObserver.of(a, b);
        composite.record(event("e1", ExecutionEventType.ANSWER_SYNTHESIZED));

        assertEquals(1, a.events().size());
        assertEquals(1, b.events().size());
    }
}
