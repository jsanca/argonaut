package dev.jsanca.argonaut.langgraph4j.api;

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
                "langgraph4j",
                "LangGraph4j",
                "1.8.24",
                "OpenRouter",
                null,
                List.of("UC-001: Controlled Local Evidence RAG"));
    }
}
