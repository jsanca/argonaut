package dev.jsanca.argonaut.core.knowledge;

import java.util.Objects;

/**
 * A reference to a document in the controlled knowledge corpus.
 */
public record DocumentReference(String sourceId, String title) {

    public DocumentReference {
        Objects.requireNonNull(sourceId, "sourceId must not be null");
    }
}
