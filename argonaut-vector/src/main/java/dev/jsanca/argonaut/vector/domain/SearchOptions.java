package dev.jsanca.argonaut.vector.domain;

/**
 * Options controlling a vector similarity search.
 */
public record SearchOptions(int limit) {

    public static SearchOptions top(int k) {
        return new SearchOptions(k);
    }
}
