package dev.jsanca.argonaut.vector.config;

import java.util.List;

/**
 * Abstraction for reading and switching the currently active {@link VectorProvider} at runtime.
 *
 * <p>The active provider governs which backend ({@code InMemory}, {@code Integrallis}, {@code Qdrant})
 * handles each store/search call. Switching is thread-safe and takes effect immediately.
 */
public interface ActiveVectorProvider {

    /** Returns the currently active provider. */
    VectorProvider current();

    /**
     * Switches the active provider to {@code provider}.
     *
     * @throws IllegalArgumentException if {@code provider} is not available in the registry
     */
    void select(VectorProvider provider);

    /** Returns all providers registered at startup. */
    List<VectorProvider> available();
}
