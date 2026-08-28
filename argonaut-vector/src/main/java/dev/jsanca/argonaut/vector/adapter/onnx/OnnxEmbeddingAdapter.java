package dev.jsanca.argonaut.vector.adapter.onnx;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import dev.jsanca.argonaut.vector.domain.Embedding;
import dev.jsanca.argonaut.vector.port.EmbeddingPort;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link EmbeddingPort} backed by the all-MiniLM-L6-v2 ONNX model loaded from the classpath.
 *
 * <p>The model produces 384-dimensional L2-normalized embeddings. Long inputs are partitioned at
 * word boundaries to stay within the 512-token BERT limit (510 usable tokens, leaving room for
 * [CLS] and [SEP]). Partition embeddings are weighted-averaged by token count and re-normalized.
 *
 * <p>Bean wiring is done in {@link dev.jsanca.argonaut.vector.config.VectorConfiguration};
 * this class is not annotated with {@code @Component} to avoid duplicate bean registration.
 */
public class OnnxEmbeddingAdapter implements EmbeddingPort, AutoCloseable {

    private static final int MAX_TOKENS_PER_PARTITION = 510;
    private static final int EMBEDDING_DIMS = 384;

    private final OrtEnvironment env;
    private final OrtSession session;
    private final HuggingFaceTokenizer tokenizer;
    private final boolean hasTokenTypeIds;

    public OnnxEmbeddingAdapter() throws OrtException, IOException {
        this.env = OrtEnvironment.getEnvironment();

        InputStream modelStream = getClass().getResourceAsStream("/all-minilm-l6-v2.onnx");
        if (modelStream == null) {
            throw new IOException("ONNX model not found on classpath: /all-minilm-l6-v2.onnx");
        }
        byte[] modelBytes = modelStream.readAllBytes();
        modelStream.close();
        this.session = env.createSession(modelBytes);
        this.hasTokenTypeIds = session.getInputNames().contains("token_type_ids");

        InputStream tokenizerStream = getClass().getResourceAsStream("/all-minilm-l6-v2-tokenizer.json");
        if (tokenizerStream == null) {
            throw new IOException("Tokenizer not found on classpath: /all-minilm-l6-v2-tokenizer.json");
        }
        this.tokenizer = HuggingFaceTokenizer.newInstance(tokenizerStream,
                Map.of("padding", "false", "truncation", "false"));
    }

    // note: ONNX implementation is functionally inspired by the reference, not behaviorally identical to LangChain4j's partitioner.
    @Override
    public Embedding embed(String text) {
        if (text == null || text.isBlank()) {
            return new Embedding(new float[EMBEDDING_DIMS]);
        }

        List<String> partitions = partition(text);

        if (partitions.size() == 1) {
            float[] vec = embedPartition(partitions.get(0));
            return new Embedding(l2Normalize(vec));
        }

        // Weighted average of partition embeddings by token count
        float[] combined = new float[EMBEDDING_DIMS];
        long totalTokens = 0;
        for (String part : partitions) {
            Encoding enc = tokenizer.encode(part);
            long tokenCount = enc.getIds().length;
            float[] partVec = embedPartitionEncoded(enc);
            for (int d = 0; d < EMBEDDING_DIMS; d++) {
                combined[d] += partVec[d] * tokenCount;
            }
            totalTokens += tokenCount;
        }
        if (totalTokens > 0) {
            for (int d = 0; d < EMBEDDING_DIMS; d++) {
                combined[d] /= totalTokens;
            }
        }
        return new Embedding(l2Normalize(combined));
    }

    /**
     * Splits text into word-boundary chunks so each chunk encodes to at most
     * {@value #MAX_TOKENS_PER_PARTITION} tokens.
     */
    private List<String> partition(String text) {
        String[] words = text.split("\\s+");
        List<String> partitions = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int tokenCount = 0;

        for (String word : words) {
            if (word.isBlank()) continue;
            int wordTokens = tokenizer.encode(word).getIds().length;
            if (tokenCount + wordTokens > MAX_TOKENS_PER_PARTITION && !current.isEmpty()) {
                partitions.add(current.toString().trim());
                current = new StringBuilder();
                tokenCount = 0;
            }
            current.append(word).append(' ');
            tokenCount += wordTokens;
        }
        if (!current.isEmpty()) {
            partitions.add(current.toString().trim());
        }
        return partitions.isEmpty() ? List.of(text) : partitions;
    }

    private float[] embedPartition(String text) {
        Encoding enc = tokenizer.encode(text);
        return embedPartitionEncoded(enc);
    }

    private float[] embedPartitionEncoded(Encoding enc) {
        long[] ids = enc.getIds();
        long[] attentionMask = enc.getAttentionMask();
        long[] typeIds = enc.getTypeIds();

        try {
            OnnxTensor inputIdsTensor = OnnxTensor.createTensor(env, new long[][]{ids});
            OnnxTensor attentionMaskTensor = OnnxTensor.createTensor(env, new long[][]{attentionMask});

            Map<String, OnnxTensor> inputs = new LinkedHashMap<>();
            inputs.put("input_ids", inputIdsTensor);
            inputs.put("attention_mask", attentionMaskTensor);

            if (hasTokenTypeIds) {
                OnnxTensor tokenTypeIdsTensor = OnnxTensor.createTensor(env, new long[][]{typeIds});
                inputs.put("token_type_ids", tokenTypeIdsTensor);
            }

            try (OrtSession.Result result = session.run(inputs)) {
                float[][][] lastHiddenState = (float[][][]) result.get("last_hidden_state").get().getValue();
                return meanPool(lastHiddenState, attentionMask);
            } finally {
                for (OnnxTensor t : inputs.values()) {
                    t.close();
                }
            }
        } catch (OrtException e) {
            throw new RuntimeException("ONNX inference failed", e);
        }
    }

    /**
     * Mean-pools the last hidden state over non-padding positions using the attention mask.
     */
    private float[] meanPool(float[][][] hiddenState, long[] attentionMask) {
        int seqLen = hiddenState[0].length;
        int dims = hiddenState[0][0].length;
        float[] pooled = new float[dims];
        long maskSum = 0;
        for (int i = 0; i < seqLen; i++) {
            if (attentionMask[i] == 1) {
                for (int d = 0; d < dims; d++) {
                    pooled[d] += hiddenState[0][i][d];
                }
                maskSum++;
            }
        }
        if (maskSum > 0) {
            for (int d = 0; d < dims; d++) {
                pooled[d] /= maskSum;
            }
        }
        return pooled;
    }

    /**
     * L2-normalizes the vector in place.
     */
    private float[] l2Normalize(float[] v) {
        float norm = 0f;
        for (float x : v) norm += x * x;
        norm = (float) Math.sqrt(norm);
        float[] out = new float[v.length];
        if (norm > 0f) {
            for (int i = 0; i < v.length; i++) {
                out[i] = v[i] / norm;
            }
        }
        return out;
    }

    @Override
    public void close() {
        try {
            session.close();
        } catch (OrtException e) {
            // best-effort close
        }
        env.close();
        tokenizer.close();
    }
}
