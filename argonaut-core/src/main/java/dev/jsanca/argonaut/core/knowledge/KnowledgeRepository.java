package dev.jsanca.argonaut.core.knowledge;

/**
 * Conceptual contract for accessing the controlled knowledge corpus.
 *
 * <p>This interface expresses two stable capabilities:</p>
 * <ul>
 *   <li><b>search</b> — find relevant information by query;</li>
 *   <li><b>read</b> — retrieve the full content of a specific document.</li>
 * </ul>
 *
 * <p>Each framework implementation wires this differently:</p>
 * <ul>
 *   <li>Spring AI → tool or bean</li>
 *   <li>LangChain4j → tool</li>
 *   <li>LangGraph4j → node/tool interaction</li>
 *   <li>Embabel → action or framework-equivalent capability</li>
 * </ul>
 *
 * <p>The interface makes no assumption about the underlying retrieval mechanism. Later
 * implementations may use in-memory search, Lucene, Qdrant, OpenSearch, pgvector, or any
 * framework-native vector store without changing this contract.</p>
 */
public interface KnowledgeRepository {

    KnowledgeSearchResponse search(KnowledgeSearchRequest request);

    DocumentContent read(DocumentReference reference);
}
