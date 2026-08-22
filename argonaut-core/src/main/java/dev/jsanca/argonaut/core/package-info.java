/**
 * Argonaut Core — shared observable contracts for the Argonaut agentic RAG experiment.
 *
 * <p>Sub-packages:</p>
 * <ul>
 *   <li>{@code experiment} — ExperimentRequest, ExperimentResult, ArgonautInfo, RunStatus</li>
 *   <li>{@code evidence} — Evidence, EvidenceKind</li>
 *   <li>{@code knowledge} — KnowledgeRepository, KnowledgeSearchRequest/Response, DocumentReference/Content</li>
 *   <li>{@code trace} — ExecutionTrace, ExecutionEvent, ExecutionEventType</li>
 *   <li>{@code observability} — ExecutionObserver, Noop/InMemory/Composite implementations</li>
 *   <li>{@code metrics} — ExecutionMetrics</li>
 *   <li>{@code error} — ArgonautError, ArgonautErrorCode</li>
 * </ul>
 *
 * <p>This module must not depend on any framework (Spring AI, LangChain4j, LangGraph4j, Embabel)
 * and must not define orchestration concepts (Agent, Graph, Node, Planner, Workflow).</p>
 */
package dev.jsanca.argonaut.core;
