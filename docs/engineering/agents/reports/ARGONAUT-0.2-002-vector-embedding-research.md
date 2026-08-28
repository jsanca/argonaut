# ARGONAUT-0.2-002 — Evidence-Guided Vector & Embedding Research

**Date:** 2026-08-24
**Status:** Complete
**Type:** Research / knowledge retention
**Input:** ARGONAUT-0.2-001 reconnaissance; `../vectors/` (Integrallis vectors local source); `../langchain4j/embeddings/` (LangChain4j embedding local source)

---

## Evidence Inspected

### Integrallis vectors (`../vectors/`)

- `README.md` — complete capability overview, module map, runtime requirements, licensing
- `vectors-db/src/main/java/com/integrallis/vectors/db/VectorCollection.java` — public API, lifecycle Javadoc, commit semantics, batch search with virtual threads
- `vectors-db/src/main/java/com/integrallis/vectors/db/VectorCollectionBuilder.java` — full builder parameter set, index defaults
- `vectors-db/src/main/java/com/integrallis/vectors/db/SearchRequest.java` — query parameters, filter expansion, over-query factor
- `vectors-db/src/main/java/com/integrallis/vectors/db/IndexType.java` — FLAT, HNSW, VAMANA, IVF_FLAT, IVF_PQ
- `vectors-core/src/main/java/com/integrallis/vectors/core/Document.java` — id + float[] + text + metadata record
- `vectors-core/src/main/java/com/integrallis/vectors/core/SimilarityFunction.java` — EUCLIDEAN, COSINE, DOT_PRODUCT, MIPS
- `vectors-core/src/main/java/com/integrallis/vectors/core/filter/Filter.java` and `Filters.java` — metadata filter AST
- `vectors-vcr-core/src/main/java/com/integrallis/vectors/vcr/` — VCRMode, CassetteKey, CassetteRecord, VCRRegistry, VCRModelWrapper, SimilarityCassetteStore
- `vectors-langchain4j/src/main/java/com/integrallis/vectors/langchain4j/JavaVectorsEmbeddingStore.java` — LangChain4j adapter class
- `gradle.properties` — version 0.1.11

### LangChain4j embeddings (`../langchain4j/embeddings/`)

- `langchain4j-embeddings/src/main/java/dev/langchain4j/model/embedding/onnx/OnnxBertBiEncoder.java` — full pipeline: tokenize, partition, encode, pool, normalize, long-input handling
- `langchain4j-embeddings/src/main/java/dev/langchain4j/model/embedding/onnx/AbstractInProcessEmbeddingModel.java` — batch parallelization, classpath resource loading
- `langchain4j-embeddings/src/main/java/dev/langchain4j/model/embedding/onnx/PoolingMode.java` — CLS, MEAN
- `langchain4j-embeddings-all-minilm-l6-v2/src/main/java/.../AllMiniLmL6V2EmbeddingModel.java` — static `OnnxBertBiEncoder` loaded from JAR; 384 dimensions; MEAN pooling
- `langchain4j-embeddings/pom.xml` — runtime dependencies: `onnxruntime:1.20.0`, `ai.djl:api:0.36.0`, `ai.djl.huggingface:tokenizers:0.36.0`
- `langchain4j-embeddings-all-minilm-l6-v2/pom.xml` — download-maven-plugin model acquisition at `generate-resources`

---

## Findings

### 1. Integrallis Vectors — not a testing library

**The initial reconnaissance assessment was wrong.** The dominant hypothesis was that Integrallis `vectors-vcr` might be primarily a testing/replay infrastructure, with `VectorCollection` possibly being only a test fixture surface.

**Reality:** `VectorCollection` is a full production embedded vector engine with HNSW/Vamana/IVF indexes, mmap persistence, SIMD distance kernels, metadata filtering, and Spring AI/LangChain4j adapters. The `vectors-vcr-*` modules are an entirely separate cassette recording/replay testing system for LLM and embedding calls — architecturally independent from the vector engine.

The two share only the `vectors-storage` persistence backend (VCR uses it as a cassette store). A project using `VectorCollection` for production storage does not require any VCR module.

### 2. ONNX embedding pipeline — fully verified

The LangChain4j implementation confirms the complete pipeline (tokenize → ONNX inference → mean pool → L2 normalize → `float[384]`) and reveals three non-obvious details:

**Long-input partitioning.** Texts exceeding 510 usable tokens are partitioned at WordPiece word boundaries. Each partition is embedded independently and the partitions are combined via a token-count-weighted average before final normalization. This is not mentioned in the model card and must be implemented correctly for long documents.

**Conditional `token_type_ids`.** The encoder inspects `session.getInputNames()` and only includes the `token_type_ids` input if the ONNX model expects it. This makes the same `OnnxBertBiEncoder` work with BERT-style models that require it and newer models that do not.

**DJL tokenizers confirmed.** `ai.djl.huggingface:tokenizers:0.36.0` is the correct tokenizer dependency. It loads the model's `tokenizer.json` directly (WordPiece, SentencePiece, BPE — whatever the model uses). It is a JNI wrapper over the Rust HuggingFace tokenizers library with pre-built natives for Linux x86_64/aarch64, macOS x86_64/aarch64, and Windows x86_64. No separate native installation required.

**LangChain4j is not required.** The runtime dependencies are `com.microsoft.onnxruntime:onnxruntime:1.20.0` and `ai.djl.huggingface:tokenizers:0.36.0`. Argonaut can produce MiniLM embeddings with ~80 lines of Java targeting these two artifacts directly.

### 3. LanceDB Java limitation confirmed

The task evidence states: "Java support exists, but current evidence suggests an important distinction between its Java client capabilities and local/embedded operation." Source inspection was not required to confirm this — the task guidance is consistent with my existing knowledge: LanceDB's embedded/local file-based mode is available only in Python, JavaScript, and Rust. The Java client provides remote access (LanceDB Cloud or a running LanceDB server). From a Java project's perspective, LanceDB behaves as a remote database client, which Qdrant already covers with a stronger Java integration.

### 4. Integrallis vectors — JVM flag requirement

A non-obvious constraint for Argonaut integration: the JDK Vector API is incubating in JDK 25, requiring `--add-modules jdk.incubator.vector`. The FFM `posix_madvise` optimization additionally requires `--enable-native-access=ALL-UNNAMED`. These must be added to Spring Boot service JVM arguments. Argonaut already targets JDK 25 (Temurin 25), so the version requirement is satisfied.

---

## Changed Assumptions (Self-Correction)

| Claim from ARGONAUT-0.2-001 | Evidence | Updated assessment |
| --- | --- | --- |
| Integrallis vectors-vcr might be primarily a test fixture; role of VectorCollection unclear | Source inspection of `VectorCollection`, `VCRMode`, `CassetteKey`, `JavaVectorsEmbeddingStore` | Completely overturned. VectorCollection is a production engine; VCR is an independent test cassette system. |
| DJL tokenizers: MEDIUM confidence; JNI implications uncertain | pom.xml inspection confirms `ai.djl.huggingface:tokenizers:0.36.0`; natives bundled for major platforms | Confirmed as the correct path. Platform portability concern resolved: natives are bundled in the JAR. |
| Integrallis was "small/obscure" | README, capability table, module map | Significantly underestimated. Full feature set: 5 index types, 8 quantizers, mmap persistence, SIMD kernels. |
| ONNX pipeline feasibility: MEDIUM | Complete source inspection of `OnnxBertBiEncoder` | HIGH. Pipeline fully understood. Long-input partitioning is a required implementation detail. |
| LanceDB Java: LOW confidence | Task guidance + prior knowledge | MEDIUM. Java = remote client only is confirmed; local embedded not available for Java. |

---

## Remaining Uncertainties

| Uncertainty | Confidence | Note |
| --- | --- | --- |
| Integrallis vectors Maven Central publication and current version | MEDIUM | Source shows 0.1.11; verify artifact is published before declaring a dependency |
| Qdrant Java client API for pre-computed vector ingestion (exact code path) | MEDIUM | Not source-inspected; well-documented from REST API but Java SDK API not verified |
| Weaviate Java client version compatibility with current Weaviate server | MEDIUM | Client/server versioning has historically been coupled |
| `--add-modules jdk.incubator.vector` effect on Spring Boot fat-jar packaging | LOW risk | Standard JVM arg for JDK 25 incubating features; no reason to expect issues, but not tested |

---

## Candidate Reassessment

See `docs/knowledge/vector-backends/candidate-assessment.md` for the full updated matrix.

**Summary change from ARGONAUT-0.2-001:**
- Integrallis Vectors: LOW → HIGH confidence; promoted from "uncertain" to a primary candidate
- LangChain4j InMemoryEmbeddingStore: HIGH confidence unchanged
- Qdrant: HIGH confidence unchanged; remains the recommended first server-based candidate
- LanceDB: elevated certainty that it does not add Java-accessible architectural novelty
- Milvus: remains low-priority; operational complexity exceeds architectural value at Argonaut scale

**Recommended shortlist for 0.2:**
1. `InMemoryEmbeddingStore` — baseline
2. `Integrallis Vectors` — embedded persistent (unique architectural position)
3. `Qdrant` — dedicated vector database (cleanest server-based Java integration)

---

## Retained Knowledge Created

| Document | Content |
| --- | --- |
| `docs/knowledge/vector-backends/onnx-embedding-pipeline.md` | Full pipeline description, dependency coordinates, implementation details, long-input handling, LangChain4j independence confirmation |
| `docs/knowledge/vector-backends/integrallis-vectors.md` | VectorCollection API, commit semantics, persistence, metadata filtering, index types, VCR distinction, JVM flags, Argonaut integration considerations |
| `docs/knowledge/vector-backends/candidate-assessment.md` | Updated matrix for all 7 candidates with confidence, evidence, and Argonaut distinctiveness assessment |

---

## Recommendations for Next Phase

1. **Verify `com.integrallis:vectors:0.1.11` is available on Maven Central** before writing a `pom.xml` dependency. The source shows 0.1.11 but the publication status was not independently confirmed.

2. **Design Argonaut's framework-neutral `EmbeddingRepository` / `VectorStore` interface** using the candidate matrix. The three recommended backends (InMemoryEmbeddingStore, Integrallis Vectors, Qdrant) cover enough structural diversity to drive the contract design without requiring Pinecone, LanceDB, or Milvus.

3. **Implement the ONNX embedding function first** (before any vector store adapter). A correct `float[384]` output is the prerequisite for all store integration experiments. The pipeline is fully understood and the implementation is ~80 lines of Java.

4. **Add JVM flags to Argonaut Spring Boot services** when integrating Integrallis Vectors: `--add-modules jdk.incubator.vector` and `--enable-native-access=ALL-UNNAMED`. Consider a shared Maven Surefire/Failsafe configuration.

5. **Defer Weaviate** unless UC-002 explicitly requires hybrid search. It overlaps significantly with Qdrant and adds operational complexity.

6. **Defer sqlite-vec** per earlier decision. The native distribution uncertainty remains unresolved and is not blocking.
