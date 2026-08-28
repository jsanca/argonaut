# Local ONNX Embedding Pipeline

## Summary

Argonaut can generate dense text embeddings locally — without a remote embedding provider — using ONNX Runtime and the DJL HuggingFace tokenizer. The LangChain4j `AllMiniLmL6V2EmbeddingModel` is the verified reference implementation; its runtime dependencies can be used directly without depending on any LangChain4j embedding abstraction.

## Reference implementation

`dev.langchain4j:langchain4j-embeddings-all-minilm-l6-v2` (local source: `../langchain4j/embeddings/langchain4j-embeddings-all-minilm-l6-v2/`)

Key classes (all in `dev.langchain4j.model.embedding.onnx`):
- `AllMiniLmL6V2EmbeddingModel` — public entry point; delegates to `OnnxBertBiEncoder`
- `OnnxBertBiEncoder` — full pipeline: tokenize → encode → pool → normalize
- `AbstractInProcessEmbeddingModel` — batch parallelization via executor
- `PoolingMode` — `MEAN` (used by MiniLM) or `CLS`

## Runtime dependencies

| Artifact | Version (at time of inspection) | Role |
| --- | --- | --- |
| `com.microsoft.onnxruntime:onnxruntime` | 1.20.0 | Inference runtime; JNI wrapper over native ONNX Runtime |
| `ai.djl:api` | 0.36.0 | DJL API; required by tokenizers module |
| `ai.djl.huggingface:tokenizers` | 0.36.0 | JNI wrapper over Rust HuggingFace tokenizers; handles WordPiece for BERT models |

LangChain4j version at inspection: `1.20.0-beta30-SNAPSHOT`.

These three artifacts are the full runtime requirement. LangChain4j itself is not required if Argonaut implements the pipeline directly.

## Pipeline in detail

```
String text
  ↓
HuggingFaceTokenizer.tokenize(text)           → List<String> tokens (WordPiece subwords)
  ↓
partition(tokens, 510)                        → List<List<String>> partitions
  ↓  [per partition]
HuggingFaceTokenizer.encode(partitionText)    → Encoding { ids, attentionMask, typeIds }
  ↓
OrtSession.run({ input_ids, attention_mask, token_type_ids })  → Result
  ↓
pool(lastHiddenState, attentionMask)          → float[] per partition
  ↓
weightedAverage(partitionEmbeddings, tokenCounts) → float[384]
  ↓
l2Normalize(vector)                           → float[384] unit vector
```

## Key implementation details

**Max sequence length**: 512 BERT tokens, of which 510 are usable (2 reserved for `[CLS]`/`[SEP]` special tokens added internally).

**Long-input partitioning**: Texts exceeding 510 tokens are partitioned at word boundaries (avoids splitting WordPiece subword tokens across partitions). Each partition is embedded independently. The final embedding is a token-count-weighted average of the partition embeddings, then L2-normalized. This means very long documents produce a single 384-dimensional vector that reflects their full content, though quality degrades at extreme lengths.

**`token_type_ids`**: Conditionally included — `OnnxBertBiEncoder` inspects `session.getInputNames()` and only sends `token_type_ids` if the model expects it. This allows the same encoder to work with models that omit it.

**Tokenizer initialization**: `HuggingFaceTokenizer.newInstance(tokenizerStream, Map.of("padding", "false"))` — padding is disabled because the encoder handles variable-length inputs per-partition with explicit shapes.

**Parallelization**: `AbstractInProcessEmbeddingModel` submits each `TextSegment` as a `CompletableFuture` on a thread pool (default: one thread per available processor, cached for 1 second). Single-segment embeds run in the caller's thread.

**Model resource loading**: `AbstractInProcessEmbeddingModel.loadFromJar()` loads `all-minilm-l6-v2.onnx` and `all-minilm-l6-v2-tokenizer.json` from the classpath. LangChain4j downloads and SHA-256-verifies these at `mvn generate-resources` time via `download-maven-plugin`.

## Embedding characteristics

| Property | Value |
| --- | --- |
| Model | `sentence-transformers/all-MiniLM-L6-v2` |
| Dimensions | 384 |
| Pooling | MEAN over token embeddings |
| Normalization | L2 (unit vector output) |
| Distance | Cosine (dot product of normalized vectors is equivalent) |
| Max input (quality) | ~256 tokens recommended; longer inputs are partitioned but quality degrades |

## Can Argonaut use this without LangChain4j?

**Yes.** The two runtime dependencies (`onnxruntime`, `ai.djl.huggingface:tokenizers`) are published independently of LangChain4j. Argonaut would need to:

1. Add `com.microsoft.onnxruntime:onnxruntime` and `ai.djl.huggingface:tokenizers` as dependencies.
2. Package `all-minilm-l6-v2.onnx` + `tokenizer.json` as classpath resources (download at build time or bundle).
3. Implement ~80 lines of Java: tokenize → build `OnnxTensor` inputs → `session.run()` → mean pool over attention mask → L2 normalize → return `float[384]`.

The LangChain4j implementation adds: long-input partitioning, batch parallelization, and adaptation to the `Embedding` / `Response<List<Embedding>>` wrapper types. An Argonaut-native implementation would only need the partitioning and normalization to be correct.

**Smallest plausible Argonaut surface** (not yet implemented):
```java
public interface EmbeddingFunction {
    float[] embed(String text);                  // single document
    List<float[]> embedAll(List<String> texts);  // batch
}
```

backed by `OnnxBertBiEncoder`-equivalent logic targeting `float[]` directly rather than the LangChain4j `Embedding` type.

## DJL `ai.djl.huggingface:tokenizers` notes

This is a JNI dependency — it wraps the Rust `tokenizers` library from HuggingFace. Native libraries are bundled in the JAR. The DJL version must match across `ai.djl:api` and `ai.djl.huggingface:tokenizers` — mixed versions cause `ClassNotFoundException`.

**Platform coverage for `tokenizers:0.36.0` (verified):**

| Platform | Native lib present |
| --- | --- |
| `linux-x86_64` | YES |
| `linux-aarch64` | YES |
| `osx-aarch64` | YES |
| `osx-x86_64` | **NO** |
| `win-x86_64` | YES |

**`osx-x86_64` gap:** The `osx-x86_64` native lib is absent from `tokenizers:0.36.0`. On an Apple Silicon Mac running an x86_64 JDK under Rosetta 2, DJL selects `osx-x86_64` as the target platform and fails to load the tokenizer. The fix is to use the native `aarch64` Temurin JDK (Rosetta emulation is not needed for M-series Macs). With the `osx-aarch64` JDK, the `osx-aarch64` native lib loads correctly and the full ONNX pipeline works.

No separate native installation is required on supported platforms — the `.dylib`/`.so`/`.dll` is unpacked from the JAR at runtime.

## Source reference

`../langchain4j/embeddings/langchain4j-embeddings/src/main/java/dev/langchain4j/model/embedding/onnx/OnnxBertBiEncoder.java` — complete pipeline implementation including partitioning, pooling, normalization, and conditional `token_type_ids`.
