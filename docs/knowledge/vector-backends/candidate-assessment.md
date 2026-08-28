# Vector Backend Candidate Assessment

**Last updated:** 2026-08-24
**Task:** ARGONAUT-0.2-002 — Evidence-Guided Vector & Embedding Research
**Scope:** Candidates for Argonaut 0.2 vector storage; external embeddings assumed (ONNX-generated `float[]`)

---

## Candidate Matrix

### LangChain4j `InMemoryEmbeddingStore`

| Field | Value |
| --- | --- |
| **Role** | In-memory vector store |
| **Operational model** | In-process, no persistence, no ANN indexing |
| **External vectors** | YES — accepts `Embedding` (float[] wrapper) directly |
| **Persistence** | None |
| **Java integration** | Zero setup; part of `langchain4j-core` |
| **Infrastructure** | None |
| **Confidence** | HIGH (source verified) |
| **Evidence** | LangChain4j source; UC-001 usage |
| **Materially distinct for Argonaut** | YES — zero-infrastructure in-memory baseline |

**Notes:** Useful as the reference baseline before introducing indexing or persistence. Establishes that embedding storage and similarity search work before any operational complexity is introduced. No deletion API. Cosine similarity. No metadata filtering.

---

### Integrallis Vectors (`com.integrallis:vectors`)

| Field | Value |
| --- | --- |
| **Role** | Embedded persistent vector engine |
| **Operational model** | In-process, in-memory or mmap-persistent, one writer per collection |
| **External vectors** | YES — `VectorCollection.add(id, float[], text)` or `Document.of(id, float[])` |
| **Persistence** | Optional: mmap files, atomic generation commits, crash recovery |
| **Java integration** | `com.integrallis:vectors` (Maven Central, Apache 2.0); JDK 25+; JVM flags required |
| **Infrastructure** | None — embedded in the application JVM |
| **Confidence** | HIGH (source verified at `../vectors/`) |
| **Evidence** | Full source inspection: `VectorCollection`, `Document`, `SearchRequest`, `Filter` APIs, VCR distinction |
| **Materially distinct for Argonaut** | YES — the only candidate offering indexed, durable, in-process operation without a server |

**Notes:** FLAT, HNSW, Vamana, IVF_FLAT, IVF_PQ indexes; 8 quantizers; SIMD via JDK Vector API; metadata filtering. JVM flags needed: `--add-modules jdk.incubator.vector` and `--enable-native-access=ALL-UNNAMED`. Spring AI and LangChain4j adapters exist; direct `VectorCollection` access is also viable with externally supplied vectors.

VCR modules (`vectors-vcr-*`) are a separate testing infrastructure — they do not affect or require the vector engine.

---

### Qdrant

| Field | Value |
| --- | --- |
| **Role** | Remote vector database |
| **Operational model** | Separate server process (Docker or native); REST + gRPC API |
| **External vectors** | YES — Qdrant is embedding-agnostic; collections define vector dimension only |
| **Persistence** | Server-managed disk storage |
| **Java integration** | `io.qdrant:client` (official; actively maintained); LangChain4j `QdrantEmbeddingStore` adapter (not required for direct use) |
| **Infrastructure** | Qdrant server (Docker: `qdrant/qdrant`); port 6333 (REST) / 6334 (gRPC) |
| **Confidence** | HIGH |
| **Evidence** | Well-documented REST + gRPC API; Java client is official |
| **Materially distinct for Argonaut** | YES — establishes the "dedicated vector database server" pattern with the richest filtering model |

**Notes:** Named vectors, payload (metadata) with filters, sparse vectors, cosine/dot/euclidean distance. Best Java client of all server-based candidates. Ideal as the first non-embedded backend candidate. No Java client source inspection needed — API is well-documented.

---

### Pinecone

| Field | Value |
| --- | --- |
| **Role** | Managed cloud vector service |
| **Operational model** | Fully managed SaaS; no local deployment; REST API |
| **External vectors** | YES |
| **Persistence** | Managed by Pinecone infrastructure |
| **Java integration** | `io.pinecone:pinecone-client` (official) |
| **Infrastructure** | Pinecone account + API key required; cloud-only |
| **Confidence** | MEDIUM |
| **Evidence** | Documentation; no source inspection |
| **Materially distinct for Argonaut** | MAYBE — represents "fully managed serverless" pattern, but no local deployment path limits reproducibility |

**Notes:** The cloud-only constraint means experiments cannot run offline or without valid API keys. This limits its value as a reproducible experiment candidate compared with Qdrant (also a dedicated database but locally deployable). Pinecone's differentiated value is its serverless, zero-infra model — architecturally interesting but practically constrained for Argonaut's offline comparison runs.

---

### LanceDB

| Field | Value |
| --- | --- |
| **Role** | Embedded persistent vector database (Python/JS/Rust native); remote client only for Java |
| **Operational model** | Local embedded (Python/JS/Rust only); Java = remote API client against LanceDB Cloud or server |
| **External vectors** | YES |
| **Persistence** | File-based (Lance columnar format) — but only accessible locally in Python/JS/Rust |
| **Java integration** | Java client available, but for remote/cloud access only; local embedded mode is not available in Java |
| **Infrastructure** | For Java: requires a running LanceDB server or LanceDB Cloud account |
| **Confidence** | MEDIUM (Java-only gap confirmed by task evidence; not source-verified) |
| **Evidence** | Task context confirms Java/embedded distinction; not independently verified from Java client source |
| **Materially distinct for Argonaut** | NO — from a Java perspective, LanceDB behaves like a remote database client (similar category to Qdrant). The embedded/columnar storage advantage is not available to Java callers. |

**Notes:** LanceDB's primary value proposition (embedded, file-based, columnar storage) is only accessible via its Python/JavaScript/Rust APIs. For a Java project, it functions as a remote client — which Qdrant already covers with better Java API support and local Docker deployment. Unless Argonaut adds a Python service tier, LanceDB adds no architectural novelty over Qdrant for Java backends.

---

### Weaviate

| Field | Value |
| --- | --- |
| **Role** | Remote vector database |
| **Operational model** | Separate server process (Docker or Weaviate Cloud); GraphQL + REST API |
| **External vectors** | YES — explicit support via `vector` field on objects |
| **Persistence** | Server-managed |
| **Java integration** | `io.weaviate:client` (official) |
| **Infrastructure** | Weaviate server (Docker: `semitechnologies/weaviate`) |
| **Confidence** | MEDIUM |
| **Evidence** | Documentation; no source inspection |
| **Materially distinct for Argonaut** | MAYBE — hybrid search (dense + BM25 full-text) is a genuine differentiator; otherwise overlaps with Qdrant in the "dedicated local server" category |

**Notes:** Weaviate adds hybrid search (dense vector + BM25 keyword re-ranking) as a differentiated capability. If Argonaut's UC-002 experiments require hybrid retrieval (e.g., vector similarity + keyword filtering), Weaviate becomes a meaningful distinct candidate. For pure vector similarity only, Qdrant has a better Java integration story and is operationally simpler.

---

### Milvus

| Field | Value |
| --- | --- |
| **Role** | Remote vector database (distributed-capable) |
| **Operational model** | Separate server; Docker single-node or distributed cluster |
| **External vectors** | YES |
| **Persistence** | Server-managed |
| **Java integration** | `io.milvus:milvus-sdk-java` |
| **Infrastructure** | Milvus server (more operationally complex than Qdrant) |
| **Confidence** | MEDIUM (concept); LOW (Java SDK API specifics for pre-computed vector ingestion) |
| **Evidence** | Documentation only; Java SDK not source-verified |
| **Materially distinct for Argonaut** | NO at this scale — richer distributed capabilities are not exercised by Argonaut UC-001/UC-002 scope. Adds operational complexity without architectural novelty over Qdrant for the experiment use case. |

**Notes:** Milvus is a strong production choice for large-scale distributed vector search. For Argonaut's experimental use case (single process, local corpus, comparison experiments), the additional operational complexity does not yield architectural insight beyond what Qdrant demonstrates. The Java SDK's pre-computed vector ingestion story is not well-verified.

---

## Deferred candidates

**sqlite-vec**: Deferred from Argonaut 0.2 per task guidance. Main risk: native library distribution via Maven (not verified). Would occupy the "embedded, SQL-queryable" niche distinct from all other candidates, but this is not blocked by Argonaut 0.2 scope.

---

## Recommended shortlist for Argonaut 0.2

These three candidates cover materially distinct architectures:

| Candidate | Architecture | Rationale |
| --- | --- | --- |
| `LangChain4j InMemoryEmbeddingStore` | In-memory, zero-infra baseline | Always available; establishes embedding pipeline before storage complexity |
| `Integrallis Vectors` | Embedded persistent engine, in-process | Unique: indexed, durable, no server, Java 25 native. Directly comparable to external DBs. |
| `Qdrant` | Dedicated vector database, local server | Best-documented Java client; richest filtering; represents the "separate service" pattern cleanly |

Pinecone or Weaviate can be added if a managed/cloud or hybrid-search experiment is desired. LanceDB and Milvus do not currently add architectural diversity for a Java-only project at this scale.
