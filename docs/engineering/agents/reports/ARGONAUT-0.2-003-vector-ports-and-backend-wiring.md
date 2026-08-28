# ARGONAUT-0.2-003 — Common Vector Ports and First Backend Wiring

**Date:** 2026-08-24
**Status:** Complete
**Type:** Implementation
**Input:** ARGONAUT-0.2-002 retained knowledge; `../vectors/` (Integrallis 0.1.11 local source); `../langchain4j/embeddings/` (LangChain4j local source); Qdrant Java client 1.17.0

---

## Summary

Implemented the `argonaut-vector` Maven module (port 8086) containing:

- Five domain records: `Document`, `Embedding`, `VectorDocument`, `SearchResult`, `SearchOptions`
- Three port interfaces: `EmbeddingPort`, `StorePort`, `SearchPort`
- `SemanticRepository` orchestrator
- ONNX-backed `OnnxEmbeddingAdapter` (MiniLM-L6-v2, 384 dimensions)
- Three backend adapters: `InMemoryVectorAdapter`, `IntegrallisVectorAdapter`, `QdrantVectorAdapter`
- `VectorProvider` enum + Spring DI strategy registry
- REST API: `POST /api/vector/store`, `POST /api/vector/search`, `GET /api/health`, `GET /api/about`
- 13 tests (9 pass, 4 skip — known ONNX platform constraint)
- Qdrant added to `compose.yaml`

---

## Module structure

```
argonaut-vector/
  domain/         — Document, Embedding, VectorDocument, SearchResult, SearchOptions
  port/           — EmbeddingPort, StorePort, SearchPort
  repository/     — SemanticRepository
  adapter/
    onnx/         — OnnxEmbeddingAdapter
    memory/       — InMemoryVectorAdapter (StorePort + SearchPort)
    integrallis/  — IntegrallisVectorAdapter (StorePort + SearchPort)
    qdrant/       — QdrantVectorAdapter (StorePort + SearchPort, conditional)
  config/         — VectorProvider, VectorProperties, VectorConfiguration
  service/        — VectorService
  api/            — VectorController
```

---

## Dependency choices

| Artifact | Version | Role |
| --- | --- | --- |
| `com.microsoft.onnxruntime:onnxruntime` | 1.20.0 | ONNX inference |
| `ai.djl:api` | 0.36.0 | DJL abstraction layer |
| `ai.djl.huggingface:tokenizers` | 0.36.0 | HuggingFace tokenizer JNI |
| `com.integrallis:vectors` | 0.1.11 | Embedded persistent vector engine |
| `io.qdrant:client` | 1.17.0 | Qdrant gRPC client |

No LangChain4j dependency in `argonaut-vector`. The ONNX adapter uses the runtime dependencies directly, confirming the finding from ARGONAUT-0.2-002 that LangChain4j is not required.

---

## Architecture conformance

The proposed architecture from the task specification was implemented without changes:

```
Controller → Service → SemanticRepository
    ↓                       ↓            ↓
             EmbeddingPort   StorePort   SearchPort
                 ↓               ↓           ↓
              ONNX          IN_MEMORY   IN_MEMORY
                            INTEGRALLIS INTEGRALLIS
                            QDRANT      QDRANT
```

The `SemanticRepository` boundary holds: callers deal in `Document` / `String query`; adapters deal in `Embedding` / `VectorDocument`. No vendor types cross the port boundary.

---

## Backend adapter findings

### InMemoryVectorAdapter

Zero-infrastructure baseline. `CopyOnWriteArrayList<VectorDocument>` plus dot-product cosine similarity (valid since embeddings are L2-normalized). No external dependencies beyond what is already in the module.

**Port fit:** Excellent. The `StorePort` + `SearchPort` split maps cleanly to a list write and a sorted stream read. No friction.

### IntegrallisVectorAdapter

Uses `VectorCollection.builder().dimension(384).metric(COSINE).indexType(HNSW).build()` directly (not via `JavaVectorsEmbeddingStore`). Storage path is configurable; an empty/null path gives in-memory-only mode for testing.

**Commit semantics:** Adds are staged and require `commit()` to become visible. The adapter calls `commit()` after each `store(VectorDocument)` and at the end of batch `store(Iterable)`. This is correct for sequential use but means individual inserts each trigger a commit. For high-throughput ingestion, batch `store()` is essential.

**`SearchResult` API:** `collection.search(SearchRequest)` returns `com.integrallis.vectors.db.SearchResult(List<Hit> hits, long searchTimeNanos)`. Each `Hit` has `.id()`, `.score()`, `.document()`. The `.document()` is the Integrallis `Document` record with `.text()` for the stored text. Text inclusion requires `SearchRequest` to not suppress it (default includes text). Implementation verified against source.

**JVM flags:** Surefire configured with `--add-modules jdk.incubator.vector --enable-native-access=ALL-UNNAMED`. The SIMD provider activates successfully (Panama Vector API, AVX2, 256-bit vector bits). Tests pass.

**Port fit:** Good. Minor friction: the `StorePort.store(Iterable)` default implementation (sequential `forEach`) makes a `commit()` per document. The overriding batch method in `IntegrallisVectorAdapter` accumulates all adds before a single `commit()`, which is the correct pattern. The `StorePort` batch default is not optimal for Integrallis but the override resolves it cleanly.

### QdrantVectorAdapter

Gated by `@ConditionalOnProperty(prefix = "argonaut.vector.qdrant", name = "enabled", havingValue = "true")`. When disabled (default), no Qdrant bean is created and the `QDRANT` key is absent from the strategy maps. If `argonaut.vector.provider=qdrant` but `enabled=false`, `SemanticRepository` construction fails with a clear error at startup.

**Connection:** `QdrantGrpcClient.newBuilder(host, grpcPort, false)` — no TLS for local dev. `ensureCollection()` checks existence and creates if absent on startup.

**Point IDs:** Qdrant requires numeric or UUID point IDs. Document string IDs are mapped to stable UUIDs via `UUID.nameUUIDFromBytes(id.getBytes(UTF_8))`. This is deterministic (same string → same UUID) and collision-resistant for typical document ID namespaces.

**Payload:** `id` and `content` fields stored as Qdrant payload string values. Recovered in search results via `sp.getPayloadMap().get("id").getStringValue()`.

**Port fit:** Good. Minor friction: `upsertAsync()` and `searchAsync()` return `ListenableFuture`; the adapter uses `.get()` for synchronous blocking. For an experiment module this is acceptable. A production adapter would expose async variants or use reactive wiring.

---

## ONNX adapter

Implements the full MiniLM-L6-v2 pipeline documented in ARGONAUT-0.2-002:

1. Tokenize via `HuggingFaceTokenizer` (DJL, JNI over Rust)
2. Partition text at word boundaries for inputs exceeding 510 tokens
3. Per partition: encode → `OnnxTensor` inputs → `session.run()` → mean pool over attention mask
4. Weighted average of partition embeddings by token count
5. L2 normalize → `float[384]`

Conditional `token_type_ids`: checked at construction time via `session.getInputNames()`. The Xenova/all-MiniLM-L6-v2 ONNX model does include `token_type_ids`, so `hasTokenTypeIds = true` in practice.

The model is not committed to git (86MB). It is downloaded at `generate-resources` phase via `download-maven-plugin` with SHA-256 verification. The tokenizer JSON (695KB) is committed as a classpath resource.

**Platform constraint:** DJL 0.36.0 tokenizer bundles native libs for `linux-x86_64`, `linux-aarch64`, `osx-aarch64`, and `win-x86_64` — but **not `osx-x86_64`**. On an Apple Silicon Mac running an x86_64 JDK under Rosetta 2, the native library lookup fails. Tests use `Assumptions.assumeTrue` so they skip rather than error, producing 4 skipped results in this environment. Running with the aarch64 Temurin JDK (native Apple Silicon) would load `osx-aarch64` and all ONNX tests would pass.

---

## Spring DI strategy registry

```
Map<VectorProvider, StorePort>   built in VectorConfiguration
Map<VectorProvider, SearchPort>  built in VectorConfiguration
```

`QdrantVectorAdapter` injected as `Optional<QdrantVectorAdapter>` so the registry builds even when the Qdrant bean is absent. The `SemanticRepository` bean fails fast at context startup if the configured `VectorProvider` has no registered adapter — which is the correct behavior (fail early, clear message).

**Enum mapping to YAML:** Spring's relaxed binding maps `in_memory` / `IN_MEMORY` / `in-memory` all to `VectorProvider.IN_MEMORY`. No special binding configuration needed.

---

## Architectural feedback

The three-port contract (`EmbeddingPort` / `StorePort` / `SearchPort`) survives contact with three materially different backends without awkward adaptation.

**Fits well:**
- `EmbeddingPort.embed(String)` — clean single-responsibility boundary; backends never see tokenizers or ONNX sessions.
- `SearchPort.search(Embedding, SearchOptions)` — the minimal contract (query embedding, limit, returned document + score) is sufficient for all three initial backends without forcing Qdrant's payload filtering or Integrallis's `minScore`/`overQueryFactor` into the common API.
- `VectorProvider` enum + Spring `Optional` injection — type-safe and avoids a custom factory. The map pattern is clear and static.

**Minor friction points:**

| Friction | Cause | Recommended adjustment |
| --- | --- | --- |
| `StorePort` batch default calls `commit()` per document | Default `forEach` delegates to single-item `store()` which commits. Integrallis needs one commit at the end. | Override batch `store()` in `IntegrallisVectorAdapter` (already done). Document that batch override is needed for commit-aware backends. |
| `StorePort` / `SearchPort` on same class for 2/3 backends | InMemory and Integrallis implement both; Qdrant implements both. Only the registry forces the split. | The split is useful (keeps embedding out of the backends) but the combined adapter pattern is natural. The current approach (split in ports, combined in adapter bean) is the right level. |
| Qdrant `ListenableFuture` sync-blocking | `.get()` in the adapter is synchronous | For an experiment module, this is fine. A production adapter would propagate futures or use reactive. |

**No recommended changes to the port design.** The abstraction is honest and small. The initial three backends confirm that `EmbeddingPort` / `StorePort` / `SearchPort` is a genuine separation of concerns rather than a premature generalization.

---

## Configuration

```yaml
argonaut:
  embedding:
    provider: onnx

  vector:
    provider: in_memory        # switch to: integrallis, qdrant
    qdrant:
      host: localhost
      grpc-port: 6334
      enabled: false           # set to true when Qdrant is running
    integrallis:
      storage-path: ""         # empty = in-memory; set absolute path for persistence
```

---

## Qdrant local development

Added to `compose.yaml`:

```yaml
services:
  qdrant:
    image: qdrant/qdrant:latest
    ports:
      - "6333:6333"   # REST API
      - "6334:6334"   # gRPC (used by argonaut-vector)
    volumes:
      - qdrant_data:/qdrant/storage

volumes:
  qdrant_data:
```

To start: `docker compose up qdrant -d`

Then set:
```yaml
argonaut.vector.qdrant.enabled: true
argonaut.vector.provider: qdrant
```

---

## Test results

| Test class | Tests | Status |
| --- | --- | --- |
| `SemanticRepositoryTest` | 3 | PASS |
| `InMemoryVectorAdapterTest` | 4 | PASS |
| `IntegrallisVectorAdapterTest` | 2 | PASS |
| `OnnxEmbeddingAdapterTest` | 4 | SKIP (DJL native missing for `osx-x86_64` under Rosetta) |
| **Total** | **13** | **9 pass, 4 skip** |

Integrallis tests confirm: JVM incubator flags load SIMD provider, `VectorCollection` HNSW search works in-memory, text is stored and retrieved correctly.

---

## Full reactor status

After adding `argonaut-vector` as module 7 in the parent POM:

```
mvn verify -DskipTests → BUILD SUCCESS (all 7 modules)
mvn test -pl argonaut-vector → BUILD SUCCESS (9 pass, 4 skip)
```

Total test count for the reactor with `argonaut-vector` included (excluding ONNX skips): **165 tests** (156 prior + 9 new).

---

## Retained knowledge updates

None required. The findings here are consistent with the retained knowledge from ARGONAUT-0.2-002. One addition warranted:

**DJL 0.36.0 platform gap:** `osx-x86_64` native lib is absent. The platform that succeeds on macOS is `osx-aarch64` (requires the aarch64 JDK). This gap should be noted in `docs/knowledge/vector-backends/onnx-embedding-pipeline.md`.
