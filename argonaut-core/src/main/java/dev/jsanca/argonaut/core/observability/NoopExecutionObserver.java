package dev.jsanca.argonaut.core.observability;

import dev.jsanca.argonaut.core.trace.ExecutionEvent;

/**
 * An {@link ExecutionObserver} that silently discards all events.
 *
 * <p>Useful in tests and framework implementations where observability is not yet wired.</p>
 */
public final class NoopExecutionObserver implements ExecutionObserver {

    public static final NoopExecutionObserver INSTANCE = new NoopExecutionObserver();

    private NoopExecutionObserver() {}

    @Override
    public void record(ExecutionEvent event) {
        // intentionally empty
    }
}
