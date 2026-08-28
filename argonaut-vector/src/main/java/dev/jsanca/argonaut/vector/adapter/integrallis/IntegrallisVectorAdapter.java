package dev.jsanca.argonaut.vector.adapter.integrallis;

import com.integrallis.vectors.core.SimilarityFunction;
import com.integrallis.vectors.db.IndexType;
import com.integrallis.vectors.db.SearchRequest;
import com.integrallis.vectors.db.VectorCollection;
import dev.jsanca.argonaut.vector.config.VectorProperties;
import dev.jsanca.argonaut.vector.domain.Document;
import dev.jsanca.argonaut.vector.domain.Embedding;
import dev.jsanca.argonaut.vector.domain.SearchOptions;
import dev.jsanca.argonaut.vector.domain.SearchResult;
import dev.jsanca.argonaut.vector.domain.VectorDocument;
import dev.jsanca.argonaut.vector.port.SearchPort;
import dev.jsanca.argonaut.vector.port.StorePort;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Vector store adapter backed by an Integrallis {@link VectorCollection}.
 *
 * <p>Supports both in-memory mode (no {@code storagePath}) and persistent mmap-backed mode when
 * {@code argonaut.vector.integrallis.storagePath} is set to an absolute directory path.
 *
 * <p>Uses HNSW index with COSINE similarity for MiniLM-L6-v2 (384 dimensions).
 */
@Component
public class IntegrallisVectorAdapter implements StorePort, SearchPort, AutoCloseable {

    private static final int EMBEDDING_DIMS = 384;

    private final VectorCollection collection;

    public IntegrallisVectorAdapter(VectorProperties props) {
        var builder = VectorCollection.builder()
                .dimension(EMBEDDING_DIMS)
                .metric(SimilarityFunction.COSINE)
                .indexType(IndexType.HNSW);

        String path = props.vector().integrallis().storagePath();
        if (path != null && !path.isBlank()) {
            builder = builder.storagePath(Path.of(path));
        }

        this.collection = builder.build();
    }

    @Override
    public void store(VectorDocument document) {
        collection.add(
                document.document().id(),
                document.embedding().values(),
                document.document().content());
        collection.commit();
    }

    @Override
    public void store(Iterable<VectorDocument> documents) {
        for (VectorDocument vd : documents) {
            collection.add(
                    vd.document().id(),
                    vd.embedding().values(),
                    vd.document().content());
        }
        collection.commit();
    }

    @Override
    public List<SearchResult> search(Embedding query, SearchOptions options) {
        com.integrallis.vectors.db.SearchResult result = collection.search(
                SearchRequest.builder(query.values(), options.limit()).build());

        return result.hits().stream()
                .map(hit -> {
                    String id = hit.id();
                    String text = hit.document() != null && hit.document().text() != null
                            ? hit.document().text()
                            : "";
                    return new SearchResult(Document.of(id, text), hit.score());
                })
                .collect(Collectors.toList());
    }

    @Override
    @PreDestroy
    public void close() {
        collection.close();
    }
}
