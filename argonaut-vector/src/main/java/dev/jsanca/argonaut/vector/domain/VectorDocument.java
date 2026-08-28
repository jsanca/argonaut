package dev.jsanca.argonaut.vector.domain;

/**
 * A {@link Document} paired with its pre-computed {@link Embedding}.
 */
public record VectorDocument(Document document, Embedding embedding) {
}
