package dev.jsanca.argonaut.decision.jev.internal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Wire representation of a single answer in a Jev API response.
 *
 * <p>The {@code type} field discriminates between {@code "choice"}, {@code "score"},
 * and {@code "noul"} answers. All fields other than {@code type} are nullable.</p>
 */
public class JevAnswerDto {

    private String type;

    /** Populated for {@code "choice"} answers. */
    private String choice;

    /** Populated for {@code "score"} answers. Fractional 0-indexed probability-weighted position. */
    private Double score;

    /** Populated for {@code "noul"} answers. P(true) in [0,1]. */
    private Double noul;

    /** Nullable — absent means no confidence information available. */
    private Double confidence;

    /** Nullable — absent means no probability distribution available. */
    private Map<String, Double> probabilities;

    /** Populated for {@code "score"} answers when a legend is provided. */
    private List<String> legend;

    public JevAnswerDto() {}

    public String getType() { return type; }
    public void setType(final String type) { this.type = type; }

    public String getChoice() { return choice; }
    public void setChoice(final String choice) { this.choice = choice; }

    public Double getScore() { return score; }
    public void setScore(final Double score) { this.score = score; }

    public Double getNoul() { return noul; }
    public void setNoul(final Double noul) { this.noul = noul; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(final Double confidence) { this.confidence = confidence; }

    public Map<String, Double> getProbabilities() { return probabilities; }
    public void setProbabilities(final Map<String, Double> probabilities) {
        this.probabilities = probabilities;
    }

    public List<String> getLegend() { return legend; }
    public void setLegend(final List<String> legend) { this.legend = legend; }
}
