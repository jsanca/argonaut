package dev.jsanca.argonaut.vector.port;

import dev.jsanca.argonaut.vector.domain.VectorDocument;

/**
 * Port for persisting {@link VectorDocument} instances into a vector store.
 */
public interface StorePort {

    void store(VectorDocument document);

    default void store(Iterable<VectorDocument> documents) {
        documents.forEach(this::store);
    }
}
