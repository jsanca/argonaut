package dev.jsanca.argonaut.core.observability;

import dev.jsanca.argonaut.core.trace.ExecutionEvent;

import java.util.List;
import java.util.Objects;

/**
 * An {@link ExecutionObserver} that forwards each event to a fixed list of delegates in order.
 *
 * <p>Enables composing the in-memory observer with future exporters (Langfuse, LangSmith,
 * OpenTelemetry) without changing the framework-specific wiring.</p>
 */
public final class CompositeExecutionObserver implements ExecutionObserver {

    private final List<ExecutionObserver> delegates;

    public CompositeExecutionObserver(List<ExecutionObserver> delegates) {
        Objects.requireNonNull(delegates, "delegates must not be null");
        this.delegates = List.copyOf(delegates);
    }

    public static CompositeExecutionObserver of(ExecutionObserver... observers) {
        return new CompositeExecutionObserver(List.of(observers));
    }

    @Override
    public void record(ExecutionEvent event) {
        for (ExecutionObserver delegate : delegates) {
            delegate.record(event);
        }
    }
}
