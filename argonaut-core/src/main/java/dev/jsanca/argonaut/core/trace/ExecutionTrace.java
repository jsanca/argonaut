package dev.jsanca.argonaut.core.trace;

import java.util.List;

/**
 * The complete ordered sequence of observable events produced during one Argonaut run.
 *
 * <p>Typically built from an {@code InMemoryExecutionObserver} via {@code toTrace()}
 * at the end of execution.</p>
 */
public record ExecutionTrace(List<ExecutionEvent> events) {

    public ExecutionTrace {
        events = events != null ? List.copyOf(events) : List.of();
    }

    public static ExecutionTrace empty() {
        return new ExecutionTrace(List.of());
    }
}
