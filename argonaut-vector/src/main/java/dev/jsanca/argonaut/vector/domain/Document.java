package dev.jsanca.argonaut.vector.domain;

import java.util.Map;

/**
 * A document with an external string id, body content, and optional metadata.
 */
public record Document(String id, String content, Map<String, Object> metadata) {

    public static Document of(String id, String content) {
        return new Document(id, content, Map.of());
    }
}
