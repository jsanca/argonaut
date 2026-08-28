package dev.jsanca.argonaut.vector.adapter.onnx;

import dev.jsanca.argonaut.vector.domain.Embedding;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests for {@link OnnxEmbeddingAdapter}.
 *
 * <p>These tests require both the ONNX model on the classpath (downloaded by the
 * download-maven-plugin during {@code generate-resources}) and a compatible native tokenizer
 * library from DJL. The DJL 0.36.0 tokenizers artifact ships native libraries for
 * {@code osx-aarch64} and {@code linux-x86_64} — if the JVM reports {@code osx-x86_64} (e.g. an
 * Intel JVM on Apple Silicon running under Rosetta) the native load will fail and all tests in
 * this class are skipped rather than errored.
 */
class OnnxEmbeddingAdapterTest {

    static OnnxEmbeddingAdapter adapter;
    static boolean available = false;

    @BeforeAll
    static void setup() {
        try {
            adapter = new OnnxEmbeddingAdapter();
            available = true;
        } catch (Exception e) {
            // DJL tokenizer native library not available for this OS/arch — skip all tests
            System.out.println("[OnnxEmbeddingAdapterTest] Skipping: " + e.getMessage());
        }
    }

    @AfterAll
    static void teardown() throws Exception {
        if (adapter != null) adapter.close();
    }

    @Test
    void embeds_text_to_384_dimensions() {
        Assumptions.assumeTrue(available, "ONNX adapter not available on this platform");
        Embedding e = adapter.embed("Hello world");
        assertThat(e.values()).hasSize(384);
    }

    @Test
    void embedding_is_unit_vector() {
        Assumptions.assumeTrue(available, "ONNX adapter not available on this platform");
        Embedding e = adapter.embed("test sentence");
        float norm = 0f;
        for (float v : e.values()) norm += v * v;
        assertThat((double) Math.sqrt(norm)).isCloseTo(1.0, within(0.001));
    }

    @Test
    void similar_texts_score_higher_than_dissimilar() {
        Assumptions.assumeTrue(available, "ONNX adapter not available on this platform");
        Embedding cat1 = adapter.embed("cats are cute animals");
        Embedding cat2 = adapter.embed("kittens are adorable pets");
        Embedding weather = adapter.embed("the weather is rainy today");

        float catSimilarity = dotProduct(cat1.values(), cat2.values());
        float unrelatedSimilarity = dotProduct(cat1.values(), weather.values());

        assertThat(catSimilarity).isGreaterThan(unrelatedSimilarity);
    }

    @Test
    void embedding_is_stable_for_same_input() {
        Assumptions.assumeTrue(available, "ONNX adapter not available on this platform");
        Embedding e1 = adapter.embed("hello world");
        Embedding e2 = adapter.embed("hello world");

        float dot = dotProduct(e1.values(), e2.values());
        assertThat((double) dot).isCloseTo(1.0, within(0.001));
    }

    private float dotProduct(float[] a, float[] b) {
        float sum = 0f;
        for (int i = 0; i < a.length; i++) sum += a[i] * b[i];
        return sum;
    }
}
