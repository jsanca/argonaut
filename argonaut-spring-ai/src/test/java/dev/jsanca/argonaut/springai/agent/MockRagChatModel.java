package dev.jsanca.argonaut.springai.agent;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test-only mock {@link ChatModel} that simulates a tool-calling LLM for TC-UC-001.
 *
 * <p>Simulates three call stages:
 * <ol>
 *   <li>Search — issues two {@code searchKnowledge} tool calls</li>
 *   <li>Read — issues three {@code readDocument} tool calls for exp-001, rag-001, obs-001</li>
 *   <li>Answer — returns a deterministic final answer grounded in the retrieved evidence</li>
 * </ol>
 *
 * <p>Returns {@link ToolCallingChatOptions} from {@link #getOptions()} so that Spring AI's
 * {@code ToolCallingAdvisor} activates the tool execution loop.
 */
class MockRagChatModel implements ChatModel {

    private static final String FINAL_ANSWER =
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
    public ChatResponse call(Prompt prompt) {
        return switch (callCount.incrementAndGet()) {
            case 1 -> searchResponse();
            case 2 -> readResponse();
            default -> finalAnswerResponse();
        };
    }

    @Override
    public ChatOptions getOptions() {
        return ToolCallingChatOptions.builder().build();
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return Flux.error(new UnsupportedOperationException("streaming not supported in mock"));
    }

    private ChatResponse searchResponse() {
        var tc1 = new AssistantMessage.ToolCall(
                "call_search_1", "function", "searchKnowledge",
                """
                {"query": "controlled evidence framework comparison"}
                """);
        var tc2 = new AssistantMessage.ToolCall(
                "call_search_2", "function", "searchKnowledge",
                """
                {"query": "controlled local evidence reproducible deterministic"}
                """);
        return chatResponse(AssistantMessage.builder().toolCalls(List.of(tc1, tc2)).build());
    }

    private ChatResponse readResponse() {
        var tc1 = new AssistantMessage.ToolCall(
                "call_read_1", "function", "readDocument",
                """
                {"sourceId": "exp-001"}
                """);
        var tc2 = new AssistantMessage.ToolCall(
                "call_read_2", "function", "readDocument",
                """
                {"sourceId": "rag-001"}
                """);
        var tc3 = new AssistantMessage.ToolCall(
                "call_read_3", "function", "readDocument",
                """
                {"sourceId": "obs-001"}
                """);
        return chatResponse(AssistantMessage.builder().toolCalls(List.of(tc1, tc2, tc3)).build());
    }

    private ChatResponse finalAnswerResponse() {
        return chatResponse(new AssistantMessage(FINAL_ANSWER));
    }

    private ChatResponse chatResponse(AssistantMessage message) {
        return new ChatResponse(List.of(new Generation(message)));
    }
}
