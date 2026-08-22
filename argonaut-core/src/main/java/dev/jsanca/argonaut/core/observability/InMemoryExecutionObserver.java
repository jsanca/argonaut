package dev.jsanca.argonaut.core.observability;

import dev.jsanca.argonaut.core.trace.ExecutionEvent;
import dev.jsanca.argonaut.core.trace.ExecutionTrace;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An {@link ExecutionObserver} that stores events in insertion order.
 *
 * <p>Thread-safe. Intended for use within a single experiment run to accumulate events
 * and then produce an {@link ExecutionTrace} via {@link #toTrace()}.</p>
 */
public final class InMemoryExecutionObserver implements ExecutionObserver {

    private final CopyOnWriteArrayList<ExecutionEvent> events = new CopyOnWriteArrayList<>();

    @Override
    public void record(ExecutionEvent event) {
        events.add(event);
    }

    public List<ExecutionEvent> events() {
        return Collections.unmodifiableList(events);
    }

    public ExecutionTrace toTrace() {
        return new ExecutionTrace(List.copyOf(events));
    }

    public void clear() {
        events.clear();
    }
}
