package dev.jsanca.argonaut.core.observability;

import dev.jsanca.argonaut.core.trace.ExecutionEvent;
import dev.jsanca.argonaut.core.trace.ExecutionEventType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryExecutionObserverTest {

    private static ExecutionEvent event(String id, ExecutionEventType type) {
        return new ExecutionEvent(id, Instant.now(), type, "test", id, id, Map.of(), null);
    }

    @Test
    void recordsEventsInInsertionOrder() {
        var observer = new InMemoryExecutionObserver();
        observer.record(event("e1", ExecutionEventType.RUN_STARTED));
        observer.record(event("e2", ExecutionEventType.KNOWLEDGE_SEARCH_STARTED));
        observer.record(event("e3", ExecutionEventType.RUN_COMPLETED));

        var recorded = observer.events();
        assertEquals(3, recorded.size());
        assertEquals("e1", recorded.get(0).id());
        assertEquals("e2", recorded.get(1).id());
        assertEquals("e3", recorded.get(2).id());
    }

    @Test
    void toTrace_returnsSnapshotOfCurrentEvents() {
        var observer = new InMemoryExecutionObserver();
        observer.record(event("e1", ExecutionEventType.RUN_STARTED));
        var trace = observer.toTrace();

        observer.record(event("e2", ExecutionEventType.RUN_COMPLETED));

        assertEquals(1, trace.events().size(), "toTrace snapshot should not include later events");
        assertEquals(2, observer.events().size());
    }

    @Test
    void clear_removesAllEvents() {
        var observer = new InMemoryExecutionObserver();
        observer.record(event("e1", ExecutionEventType.RUN_STARTED));
        observer.clear();

        assertTrue(observer.events().isEmpty());
    }

    @Test
    void events_isUnmodifiable() {
        var observer = new InMemoryExecutionObserver();
        observer.record(event("e1", ExecutionEventType.RUN_STARTED));

        var events = observer.events();
        assertThrows(UnsupportedOperationException.class,
                () -> events.add(event("e2", ExecutionEventType.RUN_COMPLETED)));
    }
}
