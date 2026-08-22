package dev.jsanca.argonaut.core.testing;

/**
 * Standard metadata keys for {@link dev.jsanca.argonaut.core.trace.ExecutionEvent#metadata()}.
 *
 * <p>Framework implementations should use these keys when recording trace events so that
 * {@link ControlledLocalEvidenceContract} can verify evidence traceability without coupling
 * to any specific framework's internal model.
 *
 * <h3>Required for TC-UC-001 validation</h3>
 * <ul>
 *   <li>{@code KNOWLEDGE_SEARCH_COMPLETED} — should carry {@link #SOURCE_IDS} when practical.</li>
 *   <li>{@code DOCUMENT_READ_COMPLETED} — must carry {@link #SOURCE_ID}.</li>
 *   <li>{@code EVIDENCE_RETRIEVED} — must carry {@link #SOURCE_ID}.</li>
 *   <li>{@code EVIDENCE_SELECTED} — must carry {@link #SOURCE_ID} or {@link #EVIDENCE_ID}.</li>
 * </ul>
 *
 * <p>Other events may use any keys they find useful; only the keys listed above are inspected
 * by the TC-UC-001 validator.
 */
public final class TraceEventMetadata {

    /** The single source document ID involved in this event (e.g. {@code "exp-001"}). */
    public static final String SOURCE_ID = "sourceId";

    /**
     * A comma-separated list of source document IDs returned by a search result
     * (e.g. {@code "exp-001,rag-001,obs-001"}).
     */
    public static final String SOURCE_IDS = "sourceIds";

    /** The search query string submitted to the knowledge repository. */
    public static final String QUERY = "query";

    /** The evidence item ID ({@link dev.jsanca.argonaut.core.evidence.Evidence#id()}) selected. */
    public static final String EVIDENCE_ID = "evidenceId";

    private TraceEventMetadata() {}
}
