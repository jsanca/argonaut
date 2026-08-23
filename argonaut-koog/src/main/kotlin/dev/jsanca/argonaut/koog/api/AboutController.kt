package dev.jsanca.argonaut.koog.api

import dev.jsanca.argonaut.core.experiment.ArgonautInfo
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class AboutController {

    @GetMapping("/about")
    fun about(): ArgonautInfo = ArgonautInfo(
        "koog",
        "Koog",
        "1.1.1",
        "OpenRouter",
        null,
        listOf("UC-001: Controlled Local Evidence RAG")
    )
}
