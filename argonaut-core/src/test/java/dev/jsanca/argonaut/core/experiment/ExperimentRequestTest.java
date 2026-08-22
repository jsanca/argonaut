package dev.jsanca.argonaut.core.experiment;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExperimentRequestTest {

    @Test
    void factoryMethod_createsRequestWithEmptyParameters() {
        var request = ExperimentRequest.of("run-1", "What is the answer?");

        assertEquals("run-1", request.runId());
        assertEquals("What is the answer?", request.question());
        assertTrue(request.parameters().isEmpty());
    }

    @Test
    void constructor_storesProvidedParameters() {
        var params = Map.of("model", "gpt-4o", "temperature", "0.7");
        var request = new ExperimentRequest("run-2", "Another question", params);

        assertEquals("gpt-4o", request.parameters().get("model"));
        assertEquals("0.7", request.parameters().get("temperature"));
    }

    @Test
    void constructor_makesParametersImmutable() {
        var mutableParams = new java.util.HashMap<String, String>();
        mutableParams.put("k", "v");
        var request = new ExperimentRequest("run-3", "Question?", mutableParams);
        mutableParams.put("other", "extra");

        assertFalse(request.parameters().containsKey("other"));
    }

    @Test
    void constructor_rejectsBlankRunId() {
        assertThrows(IllegalArgumentException.class,
                () -> ExperimentRequest.of("  ", "Question?"));
    }

    @Test
    void constructor_rejectsNullRunId() {
        assertThrows(IllegalArgumentException.class,
                () -> new ExperimentRequest(null, "Question?", Map.of()));
    }

    @Test
    void constructor_rejectsBlankQuestion() {
        assertThrows(IllegalArgumentException.class,
                () -> ExperimentRequest.of("run-4", ""));
    }

    @Test
    void constructor_treatsNullParametersAsEmpty() {
        var request = new ExperimentRequest("run-5", "Question?", null);
        assertTrue(request.parameters().isEmpty());
    }
}
