package dev.jsanca.argonaut.embabel.api;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import dev.jsanca.argonaut.core.experiment.ExperimentRequest;
import dev.jsanca.argonaut.core.experiment.ExperimentResult;
import dev.jsanca.argonaut.embabel.agent.EvidenceAnswer;
import dev.jsanca.argonaut.embabel.agent.EvidenceQuestion;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/experiment")
public class ExperimentController {

    private static final String FRAMEWORK_ID = "embabel";

    private final AgentPlatform agentPlatform;

    public ExperimentController(AgentPlatform agentPlatform) {
        this.agentPlatform = agentPlatform;
    }

    @PostMapping("/run")
    public ExperimentResult run(@RequestBody ExperimentRequest request) {
        var question = new EvidenceQuestion(request.runId(), request.question());
        EvidenceAnswer answer = AgentInvocation
                .builder(agentPlatform)
                .build(EvidenceAnswer.class)
                .invoke(question);
        return ExperimentResult.completed(
                request.runId(), FRAMEWORK_ID,
                answer.finalAnswer(), answer.evidence(), answer.trace(), answer.metrics());
    }
}
