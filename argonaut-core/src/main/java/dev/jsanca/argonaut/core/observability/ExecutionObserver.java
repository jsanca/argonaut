package dev.jsanca.argonaut.core.observability;

import dev.jsanca.argonaut.core.trace.ExecutionEvent;

/**
 * Receives normalized execution events emitted during an Argonaut run.
 *
 * <p>Implementations must be non-blocking. External observability exporters
 * (Langfuse, LangSmith, OpenTelemetry) are future adapters, not core dependencies.</p>
 */
public interface ExecutionObserver {

    void record(ExecutionEvent event);
}
