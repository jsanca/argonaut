package dev.jsanca.argonaut.decision.systemone.internal.mapping;

import dev.jsanca.argonaut.decision.systemone.internal.error.SystemOneValidationException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bidirectional mapping between typed Choice candidates and their wire-format string keys.
 *
 * <p>Keys are derived from {@code candidate.toString()}. Duplicate keys (two candidates
 * whose {@code toString()} is identical) are rejected at construction time with an
 * {@link IllegalArgumentException}, since such ambiguity cannot be resolved during
 * response mapping.</p>
 *
 * @param <T> the candidate type
 */
public final class CandidateKey<T> {

    private final Map<String, T> keyToCandidate;
    private final Map<T, String> candidateToKey;

    private CandidateKey(final Map<String, T> keyToCandidate,
                         final Map<T, String> candidateToKey) {
        this.keyToCandidate = Map.copyOf(keyToCandidate);
        this.candidateToKey = Map.copyOf(candidateToKey);
    }

    /**
     * Builds a {@code CandidateKey} for the given candidate list.
     *
     * @throws IllegalArgumentException if two candidates have the same {@code toString()} value
     */
    public static <T> CandidateKey<T> of(final List<T> candidates) {
        final Map<String, T> keyToCandidate = new LinkedHashMap<>();
        final Map<T, String> candidateToKey = new LinkedHashMap<>();

        for (final T candidate : candidates) {
            final String key = candidate.toString();
            if (keyToCandidate.containsKey(key)) {
                throw new IllegalArgumentException(
                        "Duplicate candidate key '" + key + "' in Choice candidates: "
                        + "two candidates have the same toString() value");
            }
            keyToCandidate.put(key, candidate);
            candidateToKey.put(candidate, key);
        }

        return new CandidateKey<>(keyToCandidate, candidateToKey);
    }

    /** Returns an unmodifiable view of the key-to-candidate mapping. */
    public Map<String, T> keyToCandidate() {
        return keyToCandidate;
    }

    /** Returns the string key for the given candidate. */
    public String keyFor(final T candidate) {
        return candidateToKey.get(candidate);
    }

    /**
     * Returns the candidate for the given string key.
     *
     * @throws SystemOneValidationException if the key is not present
     */
    public T candidateFor(final String key) {
        final T result = keyToCandidate.get(key);
        if (result == null) {
            throw new SystemOneValidationException(
                    "Unknown choice candidate key: '" + key + "'", 0);
        }
        return result;
    }

    /** Returns the set of all known string keys. */
    public Set<String> keys() {
        return keyToCandidate.keySet();
    }
}
