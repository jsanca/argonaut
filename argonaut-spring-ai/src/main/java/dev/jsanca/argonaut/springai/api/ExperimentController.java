package dev.jsanca.argonaut.springai.api;

import dev.jsanca.argonaut.core.experiment.ExperimentRequest;
import dev.jsanca.argonaut.core.experiment.ExperimentResult;
import dev.jsanca.argonaut.springai.agent.ControlledLocalEvidenceAgent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/experiment")
public class ExperimentController {

    private final ControlledLocalEvidenceAgent agent;

    public ExperimentController(ControlledLocalEvidenceAgent agent) {
        this.agent = agent;
    }

    @PostMapping("/run")
    public ExperimentResult run(@RequestBody ExperimentRequest request) {
        return agent.run(request);
    }
}
