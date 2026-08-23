package dev.jsanca.argonaut.embabel.api;

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
                "embabel",
                "Embabel",
                "1.5.0",
                "OpenRouter",
                null,
                List.of("controlled-local-evidence-rag", "planner-driven-retrieval", "goal-oriented-actions"));
    }
}
