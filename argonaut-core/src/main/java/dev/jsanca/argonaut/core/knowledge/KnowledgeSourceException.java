package dev.jsanca.argonaut.core.knowledge;

/**
 * Thrown when a {@link KnowledgeRepository} operation cannot be completed.
 *
 * <p>Common causes: document not found by reference, corpus not initialized,
 * or an underlying I/O error in a file-based repository.</p>
 *
 * <p>Callers should map this to {@code ArgonautErrorCode.KNOWLEDGE_SOURCE_ERROR}
 * when building an {@code ArgonautError} for the experiment result.</p>
 */
public class KnowledgeSourceException extends RuntimeException {

    public KnowledgeSourceException(String message) {
        super(message);
    }

    public KnowledgeSourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
