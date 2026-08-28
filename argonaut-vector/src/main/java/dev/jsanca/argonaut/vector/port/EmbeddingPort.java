package dev.jsanca.argonaut.vector.port;

import dev.jsanca.argonaut.vector.domain.Embedding;

/**
 * Port for converting text into a dense vector {@link Embedding}.
 */
public interface EmbeddingPort {

    Embedding embed(String text);
}
