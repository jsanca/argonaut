package dev.jsanca.argonaut.decision.systemone.internal.mapping;

import dev.jsanca.argonaut.decision.DecisionRequest;
import dev.jsanca.argonaut.decision.question.Choice;
import dev.jsanca.argonaut.decision.question.Noul;
import dev.jsanca.argonaut.decision.question.Question;
import dev.jsanca.argonaut.decision.question.Score;
import dev.jsanca.argonaut.decision.systemone.internal.dto.SystemOneQuestionDto;
import dev.jsanca.argonaut.decision.systemone.internal.dto.SystemOneRequestDto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts a {@link DecisionRequest} and a model name into a {@link SystemOneRequestDto}
 * ready for HTTP serialization.
 *
 * <p>Returns a {@link MappingResult} that bundles the DTO with a per-question
 * {@link CandidateKey} map required by {@link SystemOneResponseMapper} during response
 * deserialization.</p>
 */
public final class SystemOneRequestMapper {

    private SystemOneRequestMapper() {}

    /**
     * Bundles the serialisable request DTO with the candidate key maps needed to
     * reverse-map the API response.
     */
    public record MappingResult(
            SystemOneRequestDto request,
            Map<String, CandidateKey<?>> candidateKeys
    ) {}

    /**
     * Maps the request to a wire DTO.
     *
     * @param request   the domain request
     * @param modelName the model name to embed in the DTO (e.g. {@code "jev-latest"})
     * @return a {@link MappingResult} with the DTO and candidate key maps
     */
    public static MappingResult map(final DecisionRequest request, final String modelName) {
        final Map<String, SystemOneQuestionDto> questions = new LinkedHashMap<>();
        final Map<String, CandidateKey<?>> candidateKeys = new LinkedHashMap<>();

        for (final Question<?, ?> question : request.questions()) {
            if (question instanceof Choice<?> choice) {
                final CandidateKey<?> ck = buildChoiceQuestion(choice, questions);
                candidateKeys.put(choice.id(), ck);
            } else if (question instanceof Score<?> score) {
                buildScoreQuestion(score, questions);
            } else if (question instanceof Noul noul) {
                buildNoulQuestion(noul, questions);
            } else {
                throw new IllegalArgumentException(
                        "Unsupported question type: " + question.getClass().getName());
            }
        }

        final SystemOneRequestDto dto = new SystemOneRequestDto(
                modelName,
                request.context().state(),
                questions
        );

        return new MappingResult(dto, Map.copyOf(candidateKeys));
    }

    @SuppressWarnings("unchecked")
    private static <T> CandidateKey<T> buildChoiceQuestion(
            final Choice<T> choice,
            final Map<String, SystemOneQuestionDto> out) {

        final CandidateKey<T> ck = CandidateKey.of(choice.candidates());
        final Map<String, String> criteria = new LinkedHashMap<>();
        for (final T candidate : choice.candidates()) {
            final String key = ck.keyFor(candidate);
            criteria.put(key, key);
        }
        out.put(choice.id(), new SystemOneQuestionDto("choice", choice.instructions(), criteria));
        return ck;
    }

    private static <T> void buildScoreQuestion(
            final Score<T> score,
            final Map<String, SystemOneQuestionDto> out) {

        final List<String> criteria = new ArrayList<>();
        for (final T entry : score.scale()) {
            criteria.add(entry.toString());
        }
        out.put(score.id(), new SystemOneQuestionDto("score", score.instructions(), criteria));
    }

    private static void buildNoulQuestion(
            final Noul noul,
            final Map<String, SystemOneQuestionDto> out) {

        out.put(noul.id(), new SystemOneQuestionDto("noul", noul.instructions(), null));
    }
}
