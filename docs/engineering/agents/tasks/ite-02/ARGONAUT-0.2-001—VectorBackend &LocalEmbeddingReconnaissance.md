# ARGONAUT-0.2-001 — Vector Backend & Local Embedding Reconnaissance

## Context

We are planning Argonaut 0.2 as an experiment around multiple vector-storage implementations while keeping the embedding generation consistent across implementations.

One candidate direction is to generate embeddings locally using an ONNX model and allow different storage/search implementations to persist and query those same vectors.

Before designing abstractions or implementing integrations, we want to understand the candidate technologies and, importantly, determine where additional evidence would materially improve your ability to work with them.

This is a **reconnaissance task only**.

Do not implement anything yet.

## Candidates

Evaluate the following:

### Local embedding generation

* ONNX Runtime
* A suitable local embedding model, potentially obtained from Hugging Face and executed directly through ONNX Runtime without requiring LangChain4j

### Vector storage / search

* LangChain4j `InMemoryEmbeddingStore`
* Integrallis `VectorCollection` / vectors-vcr ecosystem
* SQLite + `sqlite-vec`
* Qdrant
* Pinecone
* LanceDB
* Weaviate
* Milvus

For Integrallis, we have seen an API resembling:

```java
try (var collection = VectorCollection.builder()
    .dimension(vectors.getFirst().length)
    .metric(SimilarityFunction.COSINE)
    .indexType(IndexType.FLAT)
    .build()) {

  collection.add(
      Document.of("doc-1", vector, text)
  );

  collection.commit();

  var result = collection.search(
      SearchRequest.builder(query, 1).build()
  );
}
```

and the related testing libraries:

```text
com.integrallis:vectors-vcr-junit5:0.1.11
com.integrallis:vectors-vcr-serde-avaje:0.1.11
com.integrallis:vectors-vcr-langchain4j:0.1.11
```

Do not assume from this snippet that we understand the complete capabilities or intended architecture of the project.

## Questions

For **each candidate**, report:

1. What do you currently know about it?

2. How confident are you in that knowledge?

    * HIGH
    * MEDIUM
    * LOW

3. What role does the technology actually play?

   Examples include, but are not limited to:

    * embedding inference runtime
    * embedded vector store
    * in-memory vector store
    * remote vector database
    * managed vector service
    * testing/replay infrastructure
    * vector search library

4. Can it reasonably participate in an architecture where embeddings are generated externally and supplied as vectors?

5. Does it appear capable of supporting:

    * storing vectors;
    * associated document/content;
    * metadata;
    * similarity search;
    * deletion/update;
    * persistence, if applicable?

6. What appears meaningfully different about it compared with the other candidates?

7. What would be the smallest plausible Argonaut integration?

## Evidence Self-Assessment

This part is especially important.

For each technology, explicitly state whether your existing knowledge is sufficient to make a reliable implementation/design decision.

If it is not, tell us what additional evidence would materially help.

Possible evidence includes:

* official documentation;
* API/Javadocs;
* Maven/Gradle metadata;
* examples;
* tests;
* source repository;
* a locally cloned repository;
* a small API snippet;
* runtime experimentation.

Do **not** request source code merely because it is available.

Explicitly answer:

> Would having this project's source repository locally available materially improve your ability to understand, implement, or verify the integration?

Answer:

```text
YES / NO / MAYBE
```

and explain why.

If YES or MAYBE, state specifically what you would inspect.

Examples:

```text
tests
examples
public interfaces
persistence implementation
JNI/native integration
HTTP/gRPC client
index lifecycle
serialization
```

Also state when downloading/cloning the repository would probably add noise rather than useful evidence.

## ONNX-specific investigation

Pay particular attention to the feasibility of:

```text
text
  ↓
tokenization
  ↓
ONNX Runtime
  ↓
local embedding model
  ↓
float[] / vector
```

without LangChain4j performing the embedding operation.

Explain what pieces would likely be required and what you currently know versus what should be verified.

Do not select an embedding model yet unless you are sufficiently confident to recommend one as an initial experimental baseline.

## Important constraints

Do not:

* write production code;
* add dependencies;
* modify Argonaut;
* clone/download repositories yet;
* design our final common API;
* assume `Store`, `Search`, `VectorStore`, or another abstraction;
* rank vendors simply by popularity;
* choose a winner.

We will decide what evidence to acquire and which candidates to pursue after reviewing this reconnaissance.

## Deliverable

Produce a concise engineering/research report containing:

### Candidate Matrix

For every candidate:

```text
Technology
Role
Knowledge confidence
External embeddings supported?
Storage/search capabilities
Persistence model
Smallest plausible integration
Source inspection: YES / NO / MAYBE
Evidence requested
Reason
```

### ONNX Assessment

Describe the feasibility and unknowns of a LangChain4j-independent local embedding pipeline.

### Knowledge Gaps

Clearly identify claims you are uncertain about.

Do not hide uncertainty behind generic language.

### Recommended Next Evidence

Tell us what you would like us to provide or allow you to inspect next, and why.

Do not perform that investigation yet.

---

The purpose of this task is not only to learn about the technologies.

We also want to observe whether you can accurately identify the limits of your current knowledge and determine which additional evidence would materially improve your next engineering decision.
