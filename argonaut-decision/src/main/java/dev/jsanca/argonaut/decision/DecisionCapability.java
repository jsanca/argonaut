package dev.jsanca.argonaut.decision;

/**
 * Provider capability vocabulary for {@link DecisionModel#capabilities()}.
 *
 * <p>{@code MULTI_QUESTION} means the API accepts multiple questions per request.
 * {@code NATIVE_BATCH} means the provider executes them as a single native batch
 * (rather than sequentially or concurrently emulated). These are distinct capabilities.</p>
 */
public enum DecisionCapability {
    CHOICE,
    SCORE,
    NOUL,
    MULTI_QUESTION,
    NATIVE_BATCH,
    PROBABILITY,
    PROBABILITY_DISTRIBUTION
}
