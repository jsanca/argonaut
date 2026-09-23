package dev.jsanca.argonaut.decision;

import java.util.Set;

/**
 * The central execution abstraction for the decision model.
 *
 * <p>A {@code DecisionModel} evaluates a {@link DecisionRequest} containing a shared
 * context and one or more heterogeneous typed questions, and returns a {@link DecisionResult}
 * from which each question's typed answer can be retrieved without consumer casts.</p>
 *
 * <p>Implementations may process questions as a native batch, an emulated batch, sequentially,
 * or concurrently — the execution strategy is not visible to callers. Native batching may
 * be advertised via {@link #capabilities()}.</p>
 *
 * <p>This interface does not represent a chat model, classifier, router, reranker, or
 * policy engine. Provider implementations belong in separate modules and must never
 * appear in this package.</p>
 */
public interface DecisionModel {

    DecisionResult decide(DecisionRequest request);

    Set<DecisionCapability> capabilities();
}
