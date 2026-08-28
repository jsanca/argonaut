package dev.jsanca.argonaut.vector.api;

import dev.jsanca.argonaut.vector.config.ActiveVectorProvider;
import dev.jsanca.argonaut.vector.config.VectorProvider;
import dev.jsanca.argonaut.vector.domain.SearchResult;
import dev.jsanca.argonaut.vector.service.VectorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST API for the argonaut-vector service (port 8086).
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code GET  /api/health}           — liveness probe
 *   <li>{@code GET  /api/about}            — module metadata including active/available providers
 *   <li>{@code POST /api/vector/store}     — embed and store a document
 *   <li>{@code POST /api/vector/search}    — embed a query and return ranked results
 *   <li>{@code GET  /api/vector/provider}  — current and available providers
 *   <li>{@code PUT  /api/vector/provider}  — switch the active provider without restart
 * </ul>
 */
@RestController
@RequestMapping("/api")
public class VectorController {

    private final VectorService vectorService;
    private final ActiveVectorProvider activeProvider;

    public VectorController(VectorService vectorService, ActiveVectorProvider activeProvider) {
        this.vectorService = vectorService;
        this.activeProvider = activeProvider;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    @GetMapping("/about")
    public ResponseEntity<Map<String, Object>> about() {
        return ResponseEntity.ok(Map.ofEntries(
                Map.entry("module", "argonaut-vector"),
                Map.entry("version", "0.1.0-SNAPSHOT"),
                Map.entry("port", 8086),
                Map.entry("description", "Common vector ports — ONNX embedding, InMemory, Integrallis, Qdrant"),
                Map.entry("activeProvider", activeProvider.current().name()),
                Map.entry("availableProviders", activeProvider.available().stream().map(VectorProvider::name).toList())
        ));
    }

    @PostMapping("/vector/store")
    public ResponseEntity<Map<String, String>> store(@RequestBody StoreRequest request) {
        vectorService.storeDocument(request.id(), request.content());
        return ResponseEntity.ok(Map.of("status", "stored", "id", request.id()));
    }

    @PostMapping("/vector/search")
    public ResponseEntity<List<SearchResultResponse>> search(@RequestBody SearchRequest request) {
        int limit = request.limit() > 0 ? request.limit() : 5;
        List<SearchResult> results = vectorService.search(request.query(), limit);
        List<SearchResultResponse> response = results.stream()
                .map(r -> new SearchResultResponse(r.document().id(), r.document().content(), r.score()))
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/vector/provider")
    public ResponseEntity<Map<String, Object>> getProvider() {
        return ResponseEntity.ok(Map.of(
                "active", activeProvider.current().name(),
                "available", activeProvider.available().stream().map(VectorProvider::name).toList()
        ));
    }

    @PutMapping("/vector/provider")
    public ResponseEntity<?> selectProvider(@RequestBody ProviderRequest request) {
        if (request.provider() == null || request.provider().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "provider must not be blank"));
        }
        VectorProvider next;
        try {
            next = VectorProvider.valueOf(request.provider().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Unknown provider: " + request.provider(),
                    "available", activeProvider.available().stream().map(VectorProvider::name).toList()
            ));
        }
        VectorProvider previous = activeProvider.current();
        try {
            activeProvider.select(next);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.ok(Map.of("previous", previous.name(), "active", next.name()));
    }

    // ---- request/response records ----

    public record StoreRequest(String id, String content) {}

    public record SearchRequest(String query, int limit) {}

    public record SearchResultResponse(String id, String content, float score) {}

    public record ProviderRequest(String provider) {}
}
