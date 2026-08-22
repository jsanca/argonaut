package dev.jsanca.argonaut.springai.api;

import dev.jsanca.argonaut.core.experiment.ArgonautInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AboutController {

    @GetMapping("/about")
    public ArgonautInfo about() {
        return new ArgonautInfo(
                "spring-ai",
                "Spring AI",
                "2.0.0",
                "OpenRouter",
                null,
                List.of("controlled-local-evidence-rag", "tool-calling", "observability"));
    }
}
