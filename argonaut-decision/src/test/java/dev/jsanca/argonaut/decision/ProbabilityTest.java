package dev.jsanca.argonaut.decision;

import dev.jsanca.argonaut.decision.probability.OutcomeProbability;
import dev.jsanca.argonaut.decision.probability.Probability;
import dev.jsanca.argonaut.decision.probability.ProbabilityDistribution;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProbabilityTest {

    @Test
    void probability_validBoundaries_accepted() {
        assertEquals(0.0, Probability.of(0.0).value(), 1e-15);
        assertEquals(1.0, Probability.of(1.0).value(), 1e-15);
        assertEquals(0.5, Probability.of(0.5).value(), 1e-15);
    }

    @Test
    void probability_belowZero_rejected() {
        assertThrows(IllegalArgumentException.class, () -> Probability.of(-0.001));
    }

    @Test
    void probability_aboveOne_rejected() {
        assertThrows(IllegalArgumentException.class, () -> Probability.of(1.001));
    }

    @Test
    void probability_nan_rejected() {
        assertThrows(IllegalArgumentException.class, () -> Probability.of(Double.NaN));
    }

    @Test
    void probability_infinity_rejected() {
        assertThrows(IllegalArgumentException.class, () -> Probability.of(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> Probability.of(Double.NEGATIVE_INFINITY));
    }

    @Test
    void outcomeProbability_nullOutcome_rejected() {
        assertThrows(NullPointerException.class,
                () -> OutcomeProbability.of(null, 0.5));
    }

    @Test
    void probabilityDistribution_immutableEntries() {
        var mutableList = new java.util.ArrayList<OutcomeProbability<String>>();
        mutableList.add(OutcomeProbability.of("a", 1.0));
        var dist = ProbabilityDistribution.of(mutableList);
        mutableList.add(OutcomeProbability.of("b", 0.0));

        assertEquals(1, dist.entries().size());
    }

    @Test
    void probabilityDistribution_nullEntries_rejected() {
        assertThrows(NullPointerException.class, () -> ProbabilityDistribution.of(null));
    }
}
