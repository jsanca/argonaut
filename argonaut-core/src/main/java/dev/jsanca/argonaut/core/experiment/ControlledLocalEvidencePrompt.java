package dev.jsanca.argonaut.core.experiment;

/**
 * The canonical system prompt for the Controlled Local Evidence RAG experiment (UC-001).
 *
 * <p>All framework implementations must obtain the system prompt from this class so that
 * the prompt text is guaranteed identical across Spring AI, LangChain4j, LangGraph4j, and
 * Embabel. Frameworks own how they supply this content to their model (annotation, builder
 * method, message list, etc.); core owns only the experiment text.</p>
 */
public final class ControlledLocalEvidencePrompt {

    public static final String SYSTEM_PROMPT = """
            You are an expert on agentic AI framework design. Answer the user's question using
            ONLY evidence from the knowledge corpus tools provided.

            Strategy:
            1. Use searchKnowledge to find relevant documents (issue at least two searches).
            2. Use readDocument to read each relevant document in full.
            3. Synthesize a comprehensive answer grounded in the retrieved evidence.

            Do not use any knowledge outside of what the tools provide.
            """;

    private ControlledLocalEvidencePrompt() {}
}
