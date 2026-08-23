package dev.jsanca.argonaut.langchain4j.agent;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.agent.tool.ToolExecutionRequest;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test-only mock {@link ChatLanguageModel} that simulates a tool-calling LLM for TC-UC-001.
 *
 * <p>Simulates three call stages to exercise the full agent loop:
 * <ol>
 *   <li>Search — issues two {@code searchKnowledge} tool calls</li>
 *   <li>Read — issues three {@code readDocument} tool calls for exp-001, rag-001, obs-001</li>
 *   <li>Answer — returns a deterministic final answer grounded in the retrieved evidence</li>
 * </ol>
 *
 * <p>LangChain4j's {@link dev.langchain4j.service.AiServices} drives the tool-calling loop
 * by calling {@link #generate(List, List)} when tool specifications are registered.
 * This mock intercepts those calls via {@code callCount} to produce stage-appropriate responses.
 */
class MockChatLanguageModel implements ChatLanguageModel {

    static final String FINAL_ANSWER =
            "Argonaut uses controlled local evidence before introducing web search, vector databases, " +
            "or external observability tools because controlled evidence reduces experimental noise. " +
            "When all framework implementations receive identical documents from the same controlled " +
            "corpus, any observed differences in their answers reflect framework orchestration quality " +
            "rather than retrieval quality. The controlled corpus also makes experiments reproducible: " +
            "the same query produces the same retrieval results on every run, enabling reliable " +
            "comparison across Spring AI, LangChain4j, LangGraph4j, and Embabel. Without controlled " +
            "evidence, differences in results may reflect retrieval noise rather than meaningful " +
            "differences in how each framework orchestrates its agentic behavior.";

    private final AtomicInteger callCount = new AtomicInteger(0);

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        return finalAnswerResponse();
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
        return switch (callCount.incrementAndGet()) {
            case 1 -> searchResponse();
            case 2 -> readResponse();
            default -> finalAnswerResponse();
        };
    }

    private Response<AiMessage> searchResponse() {
        List<ToolExecutionRequest> requests = List.of(
                ToolExecutionRequest.builder()
                        .id(UUID.randomUUID().toString())
                        .name("searchKnowledge")
                        .arguments("{\"query\": \"controlled evidence framework comparison\"}")
                        .build(),
                ToolExecutionRequest.builder()
                        .id(UUID.randomUUID().toString())
                        .name("searchKnowledge")
                        .arguments("{\"query\": \"controlled local evidence reproducible deterministic\"}")
                        .build()
        );
        return Response.from(AiMessage.from(requests));
    }

    private Response<AiMessage> readResponse() {
        List<ToolExecutionRequest> requests = List.of(
                ToolExecutionRequest.builder()
                        .id(UUID.randomUUID().toString())
                        .name("readDocument")
                        .arguments("{\"sourceId\": \"exp-001\"}")
                        .build(),
                ToolExecutionRequest.builder()
                        .id(UUID.randomUUID().toString())
                        .name("readDocument")
                        .arguments("{\"sourceId\": \"rag-001\"}")
                        .build(),
                ToolExecutionRequest.builder()
                        .id(UUID.randomUUID().toString())
                        .name("readDocument")
                        .arguments("{\"sourceId\": \"obs-001\"}")
                        .build()
        );
        return Response.from(AiMessage.from(requests));
    }

    private Response<AiMessage> finalAnswerResponse() {
        return Response.from(AiMessage.from(FINAL_ANSWER));
    }
}
