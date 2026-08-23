package dev.jsanca.argonaut.langgraph4j.agent;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.FinishReason;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Deterministic mock for {@link ChatModel} (LangChain4j 1.x).
 *
 * <p>Simulates a 4-call tool-using agent:
 * <ol>
 *   <li>Call 1 → searchKnowledge("controlled local evidence")</li>
 *   <li>Call 2 → searchKnowledge("agentic framework comparison") + readDocument("exp-001")</li>
 *   <li>Call 3 → readDocument("rag-001") + readDocument("obs-001")</li>
 *   <li>Call 4 → final text answer</li>
 * </ol>
 *
 * <p>Implements {@code doChat()} which is the abstract method of {@link ChatModel} in
 * LangChain4j 1.x (the {@code chat()} default wraps it with validation and retry logic).
 */
class MockChatModel implements ChatModel {

    private final AtomicInteger callCount = new AtomicInteger(0);

    @Override
    public ChatResponse doChat(ChatRequest request) {
        int call = callCount.incrementAndGet();

        return switch (call) {
            case 1 -> toolCallResponse(
                    ToolExecutionRequest.builder()
                            .id("t1")
                            .name("searchKnowledge")
                            .arguments("{\"query\":\"controlled local evidence\"}")
                            .build());
            case 2 -> multiToolCallResponse(
                    ToolExecutionRequest.builder()
                            .id("t2")
                            .name("searchKnowledge")
                            .arguments("{\"query\":\"agentic framework comparison\"}")
                            .build(),
                    ToolExecutionRequest.builder()
                            .id("t3")
                            .name("readDocument")
                            .arguments("{\"sourceId\":\"exp-001\"}")
                            .build());
            case 3 -> multiToolCallResponse(
                    ToolExecutionRequest.builder()
                            .id("t4")
                            .name("readDocument")
                            .arguments("{\"sourceId\":\"rag-001\"}")
                            .build(),
                    ToolExecutionRequest.builder()
                            .id("t5")
                            .name("readDocument")
                            .arguments("{\"sourceId\":\"obs-001\"}")
                            .build());
            default -> textResponse(
                    "Based on the controlled evidence corpus, the key principles of " +
                    "controlled local evidence RAG are: (1) evidence must be grounded " +
                    "in the knowledge repository, (2) the agent drives retrieval decisions, " +
                    "and (3) the same corpus is used across all framework comparisons. " +
                    "Source: exp-001, rag-001, obs-001.");
        };
    }

    private ChatResponse toolCallResponse(ToolExecutionRequest request) {
        return ChatResponse.builder()
                .aiMessage(AiMessage.from(request))
                .finishReason(FinishReason.TOOL_EXECUTION)
                .build();
    }

    private ChatResponse multiToolCallResponse(ToolExecutionRequest... requests) {
        return ChatResponse.builder()
                .aiMessage(AiMessage.builder()
                        .toolExecutionRequests(java.util.List.of(requests))
                        .build())
                .finishReason(FinishReason.TOOL_EXECUTION)
                .build();
    }

    private ChatResponse textResponse(String text) {
        return ChatResponse.builder()
                .aiMessage(AiMessage.from(text))
                .finishReason(FinishReason.STOP)
                .build();
    }
}
