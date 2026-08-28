package dev.jsanca.argonaut.vector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the argonaut-vector service (port 8086).
 *
 * <p>Provides common vector ports: ONNX embedding (MiniLM-L6-v2), InMemory, Integrallis, and
 * Qdrant backends wired together as a {@code SemanticRepository}.
 */
@SpringBootApplication
public class ArgonautVectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArgonautVectorApplication.class, args);
    }
}
