package dev.jsanca.argonaut.core.error;

/**
 * Structured error classification for Argonaut experiment failures.
 */
public enum ArgonautErrorCode {
    CONFIGURATION_ERROR,
    MODEL_PROVIDER_ERROR,
    MODEL_TIMEOUT,
    TOOL_ERROR,
    KNOWLEDGE_SOURCE_ERROR,
    VALIDATION_ERROR,
    UNSUPPORTED_OPERATION,
    INTERNAL_ERROR
}
