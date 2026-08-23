package dev.jsanca.argonaut.koog.api

import dev.jsanca.argonaut.core.experiment.ExperimentRequest
import dev.jsanca.argonaut.core.experiment.ExperimentResult
import dev.jsanca.argonaut.koog.agent.ControlledLocalEvidenceAgent
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/experiment")
class ExperimentController(private val agent: ControlledLocalEvidenceAgent) {

    @PostMapping("/run")
    fun run(@RequestBody request: ExperimentRequest): ExperimentResult = agent.run(request)
}
