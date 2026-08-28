package dev.jsanca.argonaut.vector.config;

/**
 * Selects which vector store backend is active.
 */
public enum VectorProvider {
    IN_MEMORY,
    INTEGRALLIS,
    QDRANT;

    public String displayName() {
        return switch (this) {
            case IN_MEMORY   -> "InMemory";
            case INTEGRALLIS -> "Integrallis";
            case QDRANT      -> "Qdrant";
        };
    }
}
