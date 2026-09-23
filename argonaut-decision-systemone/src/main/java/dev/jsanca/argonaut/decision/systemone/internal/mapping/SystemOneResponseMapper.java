package dev.jsanca.argonaut.decision.systemone.internal.mapping;

import dev.jsanca.argonaut.decision.DecisionRequest;
import dev.jsanca.argonaut.decision.DecisionResult;
import dev.jsanca.argonaut.decision.probability.OutcomeProbability;
import dev.jsanca.argonaut.decision.probability.Probability;
import dev.jsanca.argonaut.decision.probability.ProbabilityDistribution;
import dev.jsanca.argonaut.decision.question.Choice;
import dev.jsanca.argonaut.decision.question.Noul;
import dev.jsanca.argonaut.decision.question.Question;
import dev.jsanca.argonaut.decision.question.Score;
import dev.jsanca.argonaut.decision.result.AnswerResult;
import dev.jsanca.argonaut.decision.result.ChoiceResult;
import dev.jsanca.argonaut.decision.result.NoulResult;
import dev.jsanca.argonaut.decision.result.ScoreResult;
import dev.jsanca.argonaut.decision.systemone.internal.dto.SystemOneAnswerDto;
import dev.jsanca.argonaut.decision.systemone.internal.dto.SystemOneResponseDto;
import dev.jsanca.argonaut.decision.systemone.internal.error.SystemOneValidationException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts a {@link SystemOneResponseDto} and the original {@link DecisionRequest} back into
 * a typed {@link DecisionResult}.
 *
 * <p>Validates that:
 * <ul>
 *   <li>All question IDs present in the request are also present in the response.</li>
 *   <li>No extra question IDs are present in the response beyond those in the request.</li>
 *   <li>For Choice answers, the candidate key from the response is known.</li>
 *   <li>All probability values are in [0.0, 1.0] (enforced by {@link Probability#of}).</li>
 * </ul>
 * </p>
 */
public final class SystemOneResponseMapper {

    private SystemOneResponseMapper() {}

    @SuppressWarnings("unchecked")
    public static DecisionResult map(
            final SystemOneResponseDto response,
            final DecisionRequest request,
            final Map<String, CandidateKey<?>> candidateKeys) {

        final Map<String, SystemOneAnswerDto> answers = response.getAnswers();
        final List<Question<?, ?>> questions = request.questions();

        // Validate: no extra keys in response
        final Set<String> requestedIds = new java.util.LinkedHashSet<>();
        for (final Question<?, ?> q : questions) {
            requestedIds.add(q.id());
        }

        for (final String responseId : answers.keySet()) {
            if (!requestedIds.contains(responseId)) {
                throw new SystemOneValidationException(
                        "Unexpected question id in response: '" + responseId
                        + "' was not in the original request", 0);
            }
        }

        // Build typed result map
        final Map<String, AnswerResult<?>> rawAnswers = new LinkedHashMap<>();

        for (final Question<?, ?> question : questions) {
            final String id = question.id();
            final SystemOneAnswerDto answerDto = answers.get(id);

            if (answerDto == null) {
                throw new SystemOneValidationException(
                        "Missing answer for question id: '" + id + "' in System One response", 0);
            }

            final AnswerResult<?> result;
            if (question instanceof Choice<?> choice) {
                result = mapChoiceAnswer(answerDto, (Choice<Object>) choice,
                        (CandidateKey<Object>) candidateKeys.get(id));
            } else if (question instanceof Score<?> score) {
                result = mapScoreAnswer(answerDto, (Score<Object>) score);
            } else if (question instanceof Noul) {
                result = mapNoulAnswer(answerDto);
            } else {
                throw new IllegalArgumentException(
                        "Unsupported question type: " + question.getClass().getName());
            }

            rawAnswers.put(id, result);
        }

        return DecisionResult.forProvider(rawAnswers);
    }

    private static <T> ChoiceResult<T> mapChoiceAnswer(
            final SystemOneAnswerDto dto,
            final Choice<T> question,
            final CandidateKey<T> ck) {

        final T selected = ck.candidateFor(dto.getChoice());
        final Double confidence = dto.getConfidence();

        final ProbabilityDistribution<T> distribution;
        if (dto.getProbabilities() != null) {
            distribution = buildChoiceDistribution(dto.getProbabilities(), ck);
        } else {
            distribution = null;
        }

        return new ChoiceResult<>(selected, confidence, distribution);
    }

    private static <T> ProbabilityDistribution<T> buildChoiceDistribution(
            final Map<String, Double> probabilities,
            final CandidateKey<T> ck) {

        final List<OutcomeProbability<T>> entries = new ArrayList<>();
        for (final Map.Entry<String, Double> entry : probabilities.entrySet()) {
            final T candidate = ck.candidateFor(entry.getKey());
            try {
                entries.add(OutcomeProbability.of(candidate, entry.getValue()));
            } catch (final IllegalArgumentException e) {
                throw new SystemOneValidationException(
                        "Invalid probability value " + entry.getValue()
                        + " for candidate '" + entry.getKey() + "': " + e.getMessage(), 0);
            }
        }
        return ProbabilityDistribution.of(entries);
    }

    private static <T> ScoreResult<T> mapScoreAnswer(
            final SystemOneAnswerDto dto,
            final Score<T> question) {

        final List<T> scale = question.scale();
        final Double rawScore = dto.getScore();
        final Double confidence = dto.getConfidence();

        final T selected;
        if (dto.getProbabilities() != null) {
            selected = selectByArgmax(dto.getProbabilities(), scale);
        } else if (rawScore != null) {
            // fallback: round the fractional index to the nearest integer, clamped to bounds
            final int index = Math.min(Math.max((int) Math.round(rawScore), 0), scale.size() - 1);
            selected = scale.get(index);
        } else {
            // no data — default to first element
            selected = scale.get(0);
        }

        final ProbabilityDistribution<T> distribution;
        if (dto.getProbabilities() != null) {
            distribution = buildScoreDistribution(dto.getProbabilities(), scale);
        } else {
            distribution = null;
        }

        return new ScoreResult<>(selected, scale, rawScore, confidence, distribution);
    }

    private static <T> T selectByArgmax(
            final Map<String, Double> probabilities,
            final List<T> scale) {

        int bestIndex = 0;
        double bestProb = Double.NEGATIVE_INFINITY;

        for (final Map.Entry<String, Double> entry : probabilities.entrySet()) {
            final int index;
            try {
                index = Integer.parseInt(entry.getKey());
            } catch (final NumberFormatException e) {
                continue; // skip non-integer keys
            }
            if (entry.getValue() > bestProb && index >= 0 && index < scale.size()) {
                bestProb = entry.getValue();
                bestIndex = index;
            }
        }

        return scale.get(bestIndex);
    }

    private static <T> ProbabilityDistribution<T> buildScoreDistribution(
            final Map<String, Double> probabilities,
            final List<T> scale) {

        final List<OutcomeProbability<T>> entries = new ArrayList<>();
        for (final Map.Entry<String, Double> entry : probabilities.entrySet()) {
            final int index;
            try {
                index = Integer.parseInt(entry.getKey());
            } catch (final NumberFormatException e) {
                continue; // skip non-integer keys
            }
            if (index < 0 || index >= scale.size()) {
                continue; // ignore out-of-bound indices
            }
            try {
                entries.add(OutcomeProbability.of(scale.get(index), entry.getValue()));
            } catch (final IllegalArgumentException e) {
                throw new SystemOneValidationException(
                        "Invalid probability value " + entry.getValue()
                        + " at score index " + index + ": " + e.getMessage(), 0);
            }
        }
        return ProbabilityDistribution.of(entries);
    }

    private static NoulResult mapNoulAnswer(final SystemOneAnswerDto dto) {
        final Double noulValue = dto.getNoul();
        if (noulValue == null) {
            throw new SystemOneValidationException(
                    "Noul answer is missing the 'noul' probability field", 0);
        }
        try {
            return NoulResult.of(noulValue);
        } catch (final IllegalArgumentException e) {
            throw new SystemOneValidationException(
                    "Invalid noul probability value " + noulValue + ": " + e.getMessage(), 0);
        }
    }
}
