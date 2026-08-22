package dev.jsanca.argonaut.core.knowledge.local;

/**
 * Thrown when a Markdown corpus file cannot be parsed due to missing or malformed frontmatter.
 */
public final class MarkdownParseException extends RuntimeException {

    public MarkdownParseException(String message) {
        super(message);
    }

    public MarkdownParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
