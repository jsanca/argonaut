package dev.jsanca.argonaut.vector.adapter.qdrant;

import dev.jsanca.argonaut.vector.config.VectorProperties;
import dev.jsanca.argonaut.vector.domain.Document;
import dev.jsanca.argonaut.vector.domain.Embedding;
import dev.jsanca.argonaut.vector.domain.SearchOptions;
import dev.jsanca.argonaut.vector.domain.SearchResult;
import dev.jsanca.argonaut.vector.domain.VectorDocument;
import dev.jsanca.argonaut.vector.port.SearchPort;
import dev.jsanca.argonaut.vector.port.StorePort;
import io.qdrant.client.PointIdFactory;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.ValueFactory;
import io.qdrant.client.VectorsFactory;
import io.qdrant.client.WithPayloadSelectorFactory;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.SearchPoints;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Vector store adapter backed by a Qdrant server.
 *
 * <p>Always created as a Spring bean. If the Qdrant server is unreachable at startup,
 * the adapter marks itself unavailable ({@link #isAvailable()} returns {@code false}) rather
 * than failing the entire application context. The {@link VectorProviderRegistry} will skip
 * registering this adapter when it is unavailable.
 *
 * <p>Connects via gRPC (default port 6334). Uses COSINE distance for MiniLM-L6-v2 (384 dims).
 * Document string IDs are mapped to stable UUIDs via {@link UUID#nameUUIDFromBytes(byte[])}.
 */
@Component
public class QdrantVectorAdapter implements StorePort, SearchPort {

    private static final Logger log = LoggerFactory.getLogger(QdrantVectorAdapter.class);

    private static final String COLLECTION = "argonaut-documents";
    private static final int DIMENSION = 384;

    private final QdrantClient client;
    private final boolean available;

    public QdrantVectorAdapter(VectorProperties props) {
        VectorProperties.QdrantConfig cfg = props.vector().qdrant();
        QdrantClient tempClient = null;
        boolean tempAvailable = false;
        try {
            tempClient = new QdrantClient(
                    QdrantGrpcClient.newBuilder(cfg.host(), cfg.grpcPort(), false).build());
            ensureCollection(tempClient);
            tempAvailable = true;
        } catch (Exception e) {
            log.warn("Qdrant unavailable at {}:{} — QDRANT provider will not be registered: {}",
                    cfg.host(), cfg.grpcPort(), e.getMessage());
            if (tempClient != null) {
                try { tempClient.close(); } catch (Exception ignored) {}
            }
            tempClient = null;
        }
        this.client = tempClient;
        this.available = tempAvailable;
    }

    /** Returns {@code true} if the Qdrant server was reachable at startup. */
    public boolean isAvailable() {
        return available;
    }

    @Override
    public void store(VectorDocument document) {
        if (!available) throw new IllegalStateException("Qdrant provider is not available");
        UUID uuid = UUID.nameUUIDFromBytes(document.document().id().getBytes(StandardCharsets.UTF_8));
        PointStruct point = PointStruct.newBuilder()
                .setId(PointIdFactory.id(uuid))
                .setVectors(VectorsFactory.vectors(toFloatList(document.embedding().values())))
                .putAllPayload(Map.of(
                        "id", ValueFactory.value(document.document().id()),
                        "content", ValueFactory.value(document.document().content())))
                .build();
        try {
            client.upsertAsync(COLLECTION, List.of(point)).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Qdrant upsert failed", e);
        }
    }

    @Override
    public List<SearchResult> search(Embedding query, SearchOptions options) {
        if (!available) throw new IllegalStateException("Qdrant provider is not available");
        SearchPoints request = SearchPoints.newBuilder()
                .setCollectionName(COLLECTION)
                .addAllVector(toFloatList(query.values()))
                .setLimit(options.limit())
                .setWithPayload(WithPayloadSelectorFactory.enable(true))
                .build();
        try {
            List<ScoredPoint> points = client.searchAsync(request).get(10, TimeUnit.SECONDS);
            return points.stream()
                    .map(sp -> {
                        String id = sp.getPayloadMap().get("id").getStringValue();
                        String content = sp.getPayloadMap().get("content").getStringValue();
                        return new SearchResult(Document.of(id, content), sp.getScore());
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Qdrant search failed", e);
        }
    }

    private void ensureCollection(QdrantClient c) throws Exception {
        boolean exists = c.collectionExistsAsync(COLLECTION).get(10, TimeUnit.SECONDS);
        if (!exists) {
            c.createCollectionAsync(COLLECTION,
                    VectorParams.newBuilder()
                            .setSize(DIMENSION)
                            .setDistance(Distance.Cosine)
                            .build())
                    .get(10, TimeUnit.SECONDS);
        }
    }

    private List<Float> toFloatList(float[] values) {
        List<Float> list = new ArrayList<>(values.length);
        for (float v : values) {
            list.add(v);
        }
        return list;
    }

    @PreDestroy
    public void close() {
        if (client != null) {
            client.close();
        }
    }
}
