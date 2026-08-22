package dev.jsanca.argonaut.core.observability;

import dev.jsanca.argonaut.core.trace.ExecutionEvent;
import dev.jsanca.argonaut.core.trace.ExecutionEventType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NoopExecutionObserverTest {

    @Test
    void record_doesNotThrow() {
        var event = new ExecutionEvent("e1", Instant.now(), ExecutionEventType.RUN_STARTED,
                "test", "started", "run started", Map.of(), null);
        assertDoesNotThrow(() -> NoopExecutionObserver.INSTANCE.record(event));
    }

    @Test
    void singleton_instanceIsReusable() {
        assertSame(NoopExecutionObserver.INSTANCE, NoopExecutionObserver.INSTANCE);
    }
}
