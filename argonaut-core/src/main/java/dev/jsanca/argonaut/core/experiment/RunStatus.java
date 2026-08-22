package dev.jsanca.argonaut.core.experiment;

/**
 * Lifecycle status of a single Argonaut experiment run.
 */
public enum RunStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    PARTIAL,
    CANCELLED
}
