package dev.jsanca.argonaut.core.trace;

/**
 * Observable event types emitted during an Argonaut agentic RAG execution.
 *
 * <p>These types express the normalized observable behavior of the experiment regardless of
 * which framework produced the event. Framework-specific detail may be stored in
 * {@link ExecutionEvent#metadata()}.</p>
 */
public enum ExecutionEventType {

    RUN_STARTED,
    RUN_COMPLETED,
    RUN_FAILED,

    STEP_STARTED,
    STEP_COMPLETED,
    STEP_FAILED,

    TOOL_CALL_STARTED,
    TOOL_CALL_COMPLETED,
    TOOL_CALL_FAILED,

    MODEL_CALL_STARTED,
    MODEL_CALL_COMPLETED,
    MODEL_CALL_FAILED,

    KNOWLEDGE_SEARCH_STARTED,
    KNOWLEDGE_SEARCH_COMPLETED,

    DOCUMENT_READ_STARTED,
    DOCUMENT_READ_COMPLETED,

    EVIDENCE_RETRIEVED,
    EVIDENCE_SELECTED,
    ANSWER_SYNTHESIZED
}
