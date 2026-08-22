package dev.jsanca.argonaut.core.knowledge;

import java.util.Objects;

/**
 * The content retrieved from the controlled knowledge corpus for a given document reference.
 */
public record DocumentContent(DocumentReference reference, String content) {

    public DocumentContent {
        Objects.requireNonNull(reference, "reference must not be null");
        Objects.requireNonNull(content, "content must not be null");
    }
}
