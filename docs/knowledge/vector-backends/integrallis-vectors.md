# Integrallis Vectors

## Summary

`vectors` (published as `com.integrallis:vectors`) is a full-featured embedded vector search and persistence engine for Java 25. It runs in-process with no separate server process. The VCR modules (`vectors-vcr-*`) are a separate testing infrastructure for recording and replaying LLM/embedding calls; they are architecturally independent of the vector engine.

Local source: `../vectors/` (sibling of Argonaut in the workspace).

Maven Central: `com.integrallis` namespace. Version at inspection: **0.1.11**.

## What it is

An indexed local vector store that fills the gap between:
- In-memory stores (`LangChain4j InMemoryEmbeddingStore`, Spring AI `SimpleVectorStore`) — no persistence, no ANN indexing, no metadata filtering
- External vector databases (Qdrant, Weaviate, Milvus) — separate server process, network hop, operational overhead

`vectors` gives HNSW/Vamana/IVF indexing and durable mmap-backed persistence inside the application JVM, with no service to operate.

## Core API

Entry point: `com.integrallis.vectors.db.VectorCollection` (interface, `AutoCloseable`).

```java
// Create — omit storagePath for in-memory; include absolute path for persistent
try (VectorCollection collection = VectorCollection.builder()
        .dimension(384)
        .metric(SimilarityFunction.COSINE)
        .indexType(IndexType.HNSW)
        .storagePath(Path.of("/absolute/path/to/data"))   // omit for in-memory
        .build()) {

    // Add (staged, not visible until commit)
    collection.add("doc-1", vector1, "optional text");
    collection.add(Document.of("doc-2", vector2, "text", metadata));
    collection.commit();

    // Search
    SearchResult result = collection.search(
        SearchRequest.builder(queryVector, 5).build());

    // Filtered search
    SearchResult filtered = collection.search(
        SearchRequest.builder(queryVector, 5)
            .filter(Filters.eq("sourceId", "exp-001"))
            .build());

    // Delete (tombstone; takes effect on next commit)
    collection.delete("doc-1");
    collection.commit();

    // Upsert
    collection.upsert(Document.of("doc-1", updatedVector, "new text"));
    collection.commit();
}
```

### `Document` record

```java
public record Document(String id, float[] vector, String text, Map<String, MetadataValue> metadata)
```

Factory methods: `Document.of(id, vector)`, `Document.of(id, vector, text)`, full constructor.

Vectors passed to `add()`/`upsert()` are **defensively cloned** at the staging boundary — the caller's `float[]` buffer can be safely reused. Vectors *returned* in search results are references to internal storage and must not be mutated.

### `SearchRequest`

Built via `SearchRequest.builder(queryVector, k)`. Optional parameters:
- `.filter(Filter)` — metadata predicate (see Filter section)
- `.minScore(float)` — minimum similarity score threshold
- `.includeVector(bool)`, `.includeText(bool)`, `.includeMetadata(bool)` — projection control
- `.overQueryFactor(float)` — extra candidates for quantized rescoring
- `.filterExpansion(float)` — extra candidates fetched when a metadata filter is active

### Similarity functions

`SimilarityFunction` enum: `COSINE`, `DOT_PRODUCT`, `EUCLIDEAN`, `MIPS`.

Scores are normalized so that higher = more similar.

### Index types

`IndexType` enum: `FLAT`, `HNSW`, `VAMANA`, `IVF_FLAT`, `IVF_PQ`.

`HNSW` is the recommended default for most use cases (fast ANN, good recall). `FLAT` is a brute-force scan useful for small collections and correctness testing.

## Commit semantics and persistence

Adds, upserts, and deletes are **staged** in a buffer and are invisible to search until `commit()`. `commit()` atomically publishes a new generation via a volatile snapshot, so concurrent readers are never blocked.

If `autoCommitThreshold` is configured on the builder, staged inserts auto-commit when the buffer reaches that size.

**Persistence** (`storagePath` set): mmap files, atomic generation commits with crash-recovery walk-back. The storage engine retains the last `retainGenerations` (default: 2) committed generations on disk for recovery; older ones are reclaimed by background compaction. Format carries explicit version checks — back up data before upgrading across format versions.

**In-memory** (`storagePath` absent): no files created; all data lost on `close()`.

## Metadata

`Document.metadata` is `Map<String, MetadataValue>`. `MetadataValue` is a sealed type with variants: `Str(String)`, `Num(double)`, `Bool(boolean)`.

The `Filter` API (`com.integrallis.vectors.core.filter`) supports:
- Equality: `Filters.eq(field, value)` (string, long, double, boolean)
- Comparison: `Filters.gt`, `Filters.gte`, `Filters.lt`, `Filters.lte`
- Not-equal: `Filters.ne`
- Set membership: `Filters.in`, `Filters.nin`
- Logical: `Filters.and(...)`, `Filters.or(...)`, `Filters.not(...)`
- Match-all: `Filters.all()`

## Quantization

Optional: SQ8, SQ4, FP16, PQ, BQ/BBQ, RaBitQ, NVQ, TurboQuant. Configured on the builder. Not required for an initial Argonaut integration.

## Batch and parallel search

`VectorCollection.searchBatch(List<SearchRequest>)` dispatches each query to a virtual-thread executor, collecting results in request order. Single-query searches use the caller's thread.

## Framework adapters

**LangChain4j**: `com.integrallis:vectors-langchain4j` provides `JavaVectorsEmbeddingStore`, a drop-in `EmbeddingStore<TextSegment>` replacement for `InMemoryEmbeddingStore`.

```java
EmbeddingStore<TextSegment> store =
    JavaVectorsEmbeddingStore.builder(collection).build();
```

**Spring AI**: `com.integrallis:vectors-spring-ai` provides `JavaVectorsVectorStore`.

**Spring Boot auto-config**: `com.integrallis:vectors-spring-boot-starter`.

For Argonaut's framework-neutral design, the adapter modules are not required — `VectorCollection` can be used directly with externally supplied `float[]` vectors.

## VCR modules (separate from vector engine)

`vectors-vcr-*` is a test cassette recording/replay system for LLM and embedding API calls, inspired by vcr.py/WireMock for HTTP. It intercepts embedding and chat model calls, serializes them with configurable serde (Avaje Jsonb, Jackson), and replays them in subsequent test runs.

VCR modes: `PLAYBACK`, `RECORD`, `RECORD_NEW`, `PASSTHROUGH`.

Cassette keys: `CassetteKey(type, testId, callIndex)` — stored as `vcr:<type>:<testId>:<callIndex>`.

VCR uses the `vectors-storage` persistence backend as its cassette store. The testing modules (`vectors-vcr-junit5`, `vectors-vcr-testng`) provide annotations (`@VCRModel`, `@VCRRecord`, `@VCRDisabled`) and test extensions.

**VCR is architecturally independent of `VectorCollection`.** A project using `VectorCollection` does not require any VCR module, and vice versa.

## Runtime requirements and JVM flags

- **JDK 25+** (required — built on JDK Vector API for SIMD distance kernels and FFM API)
- **No JNI or native libraries** — pure Java, except that the FFM API calls `posix_madvise` for an optional mmap optimization
- Required JVM arguments:
  ```
  --add-modules jdk.incubator.vector
  --enable-native-access=ALL-UNNAMED
  ```
  Argonaut already targets Java 25 (JDK 25, Temurin 25) ✓. The JVM flags must be added to the service startup configuration.

## SIMD performance

The JDK Vector API SIMD distance kernels run 4.4–6.8× faster than scalar equivalents on the committed AVX2 baseline for 768- and 1536-dimensional float vectors. Performance is hardware- and JDK-version-specific.

## Licensing

Core engine (`vectors`, `vectors-core`, `vectors-db`, `vectors-hnsw`, `vectors-vamana`, `vectors-ivf`, `vectors-storage`, `vectors-langchain4j`, `vectors-spring-ai`, `vectors-spring-boot-starter`, `vectors-vcr-*`): **Apache 2.0**.

Distributed/cluster/server/GPU tier (`vectors-distributed`, `vectors-cluster`, `vectors-server`, `vectors-gpu`): FSL-1.1-ALv2 (converts to Apache 2.0 on documented change date). Not relevant for Argonaut's local use case.

## Argonaut integration considerations

- `VectorCollection` operates with externally supplied `float[]` vectors — compatible with ONNX-generated embeddings without requiring any framework adapter.
- The `storagePath` builder parameter makes persistence opt-in; in-memory mode is available for testing.
- JVM flags (`--add-modules`, `--enable-native-access`) must be added to service startup. Spring Boot services started via the Maven plugin or `java -jar` need them in `JAVA_OPTS` or `spring-boot-maven-plugin` JVM args configuration.
- One collection instance per storage directory; multi-process write is not supported by design.
- The `com.integrallis:vectors` umbrella dependency pulls only the local engine and SLF4J (< 1 MiB total). Framework adapter modules are additive.

## Source reference

`../vectors/` — inspect `vectors-db/src/main/java/com/integrallis/vectors/db/` for `VectorCollection`, `VectorCollectionBuilder`, `SearchRequest`, `IndexType`; `vectors-core/src/main/java/com/integrallis/vectors/core/` for `Document`, `SimilarityFunction`, `Filter`/`Filters`/`MetadataValue`.
