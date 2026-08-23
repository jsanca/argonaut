package dev.jsanca.argonaut.langgraph4j.agent;

import dev.jsanca.argonaut.core.evidence.Evidence;
import dev.jsanca.argonaut.core.evidence.EvidenceKind;
import dev.jsanca.argonaut.core.experiment.ControlledLocalEvidencePrompt;
import dev.jsanca.argonaut.core.experiment.ExperimentRequest;
import dev.jsanca.argonaut.core.experiment.ExperimentResult;
import dev.jsanca.argonaut.core.knowledge.DocumentContent;
import dev.jsanca.argonaut.core.knowledge.KnowledgeRepository;
import dev.jsanca.argonaut.core.metrics.ExecutionMetrics;
import dev.jsanca.argonaut.core.observability.InMemoryExecutionObserver;
import dev.jsanca.argonaut.core.testing.TraceEventMetadata;
import dev.jsanca.argonaut.core.trace.ExecutionEvent;
import dev.jsanca.argonaut.core.trace.ExecutionEventType;
import dev.jsanca.argonaut.core.trace.ExecutionTrace;
import dev.jsanca.argonaut.langgraph4j.knowledge.KnowledgeTools;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.langchain4j.serializer.std.LC4jStateSerializer;
import org.bsc.langgraph4j.langchain4j.tool.LC4jToolService;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * LangGraph4j graph-based implementation of the Controlled Local Evidence RAG use case (UC-001).
 *
 * <p>Graph topology (ReAct pattern):
 * <pre>
 *   START → agent → (hasToolCalls?) → tools → agent → ... → END
 * </pre>
 *
 * <p>Unlike Spring AI ({@code ChatClient}, hidden tool loop) and LangChain4j ({@code AiServices},
 * hidden tool loop), LangGraph4j makes the agentic loop explicit as graph edges. The model is
 * called directly via {@code chatModel.chat(ChatRequest)} inside the agent node, which means
 * the number of actual model calls is observable and counted accurately.
 *
 * <p>Per-run state:
 * <ul>
 *   <li>The graph is compiled per run, with node closures capturing the per-run
 *       {@link InMemoryExecutionObserver}, reads accumulator, and model-call counter.</li>
 *   <li>This is different from Spring AI ({@code ToolContext} side-channel) and
 *       LangChain4j 0.x ({@code KnowledgeTools} constructor injection + per-run
 *       {@code AiServices.build()}).</li>
 * </ul>
 *
 * <p>Trace events: agent node emits {@code MODEL_CALL_STARTED/COMPLETED} (newly observable
 * in this framework); tool node delegates to {@link KnowledgeTools} for
 * {@code KNOWLEDGE_SEARCH_*}, {@code DOCUMENT_READ_*}, {@code EVIDENCE_RETRIEVED};
 * this class emits {@code RUN_*}, {@code EVIDENCE_SELECTED}, {@code ANSWER_SYNTHESIZED}.
 */
@Service
public class ControlledLocalEvidenceAgent {

    static final String FRAMEWORK_ID = "langgraph4j";

    private final ChatModel chatModel;
    private final KnowledgeRepository repository;

    public ControlledLocalEvidenceAgent(ChatModel chatModel, KnowledgeRepository repository) {
        this.chatModel = chatModel;
        this.repository = repository;
    }

    public ExperimentResult run(ExperimentRequest request) {

        final long startMs = System.currentTimeMillis();
        final var observer = new InMemoryExecutionObserver();
        final List<DocumentContent> reads = new CopyOnWriteArrayList<>();
        final AtomicInteger modelCallCounter = new AtomicInteger(0);

        observer.record(agentEvent(ExecutionEventType.RUN_STARTED, Map.of()));

        try {
            final var knowledgeTools = new KnowledgeTools(repository, observer, reads);
            final var toolService = LC4jToolService.builder()
                    .toolsFromObject(knowledgeTools)
                    .build();
            final var toolSpecs = toolService.toolSpecifications();

            final var graph = new MessagesStateGraph<ChatMessage>(
                    new LC4jStateSerializer<>(MessagesState::new))
                    .addNode("agent", node_async(state -> {
                        var history = state.messages();
                        var messages = new ArrayList<ChatMessage>();
                        messages.add(SystemMessage.from(ControlledLocalEvidencePrompt.SYSTEM_PROMPT));
                        messages.addAll(history);

                        observer.record(agentEvent(ExecutionEventType.MODEL_CALL_STARTED, Map.of()));
                        modelCallCounter.incrementAndGet();

                        var chatRequest = ChatRequest.builder()
                                .messages(messages)
                                .parameters(ChatRequestParameters.builder()
                                        .toolSpecifications(toolSpecs)
                                        .build())
                                .build();

                        var response = chatModel.chat(chatRequest);

                        observer.record(agentEvent(ExecutionEventType.MODEL_CALL_COMPLETED, Map.of()));

                        return Map.of("messages", response.aiMessage());
                    }))
                    .addNode("tools", node_async(state -> {
                        var lastMsg = (AiMessage) state.lastMessage().orElseThrow(
                                () -> new IllegalStateException("tools node: no last message in state"));

                        var command = toolService.execute(
                                lastMsg.toolExecutionRequests(),
                                InvocationContext.builder().build(),
                                "messages"
                        ).get();

                        return command.update();
                    }))
                    .addEdge(START, "agent")
                    .addConditionalEdges("agent",
                            edge_async(state -> state.lastMessage()
                                    .filter(m -> m instanceof AiMessage ai && ai.hasToolExecutionRequests())
                                    .map(_ -> "tools")
                                    .orElse(END)),
                            Map.of("tools", "tools", END, END))
                    .addEdge("tools", "agent")
                    .compile();

            final var finalState = graph.invoke(
                    Map.of("messages", UserMessage.from(request.question()))
            ).orElseThrow(() -> new IllegalStateException("graph produced no final state"));

            final var lastMsg = (AiMessage) finalState.lastMessage().orElseThrow(
                    () -> new IllegalStateException("final state has no messages"));

            final String answer = lastMsg.text();
            if (answer == null || answer.isBlank()) {
                throw new IllegalStateException("model produced no answer");
            }

            final List<Evidence> evidence = buildEvidence(reads);

            for (final Evidence ev : evidence) {
                observer.record(agentEvent(ExecutionEventType.EVIDENCE_SELECTED,
                        Map.of(TraceEventMetadata.SOURCE_ID, ev.sourceId(),
                               TraceEventMetadata.EVIDENCE_ID, ev.id())));
            }
            observer.record(agentEvent(ExecutionEventType.ANSWER_SYNTHESIZED, Map.of()));
            observer.record(agentEvent(ExecutionEventType.RUN_COMPLETED, Map.of()));

            final ExecutionTrace trace = observer.toTrace();
            final ExecutionMetrics metrics = buildMetrics(trace, evidence,
                    System.currentTimeMillis() - startMs, modelCallCounter.get());

            return ExperimentResult.completed(request.runId(), FRAMEWORK_ID, answer, evidence, trace, metrics);

        } catch (Exception e) {
            observer.record(agentEvent(ExecutionEventType.RUN_FAILED, Map.of()));
            ExecutionTrace trace = observer.toTrace();
            long durationMs = System.currentTimeMillis() - startMs;
            ExecutionMetrics metrics = new ExecutionMetrics(durationMs, 0, 0, 0, 0, 0, 1);
            return ExperimentResult.failed(request.runId(), FRAMEWORK_ID, List.of(), trace, metrics);
        }
    }

    private List<Evidence> buildEvidence(List<DocumentContent> reads) {
        return reads.stream()
                .map(doc -> {
                    String sourceId = doc.reference().sourceId();
                    String body = doc.content();
                    String excerpt = body.substring(0, Math.min(200, body.length())).strip();
                    return new Evidence(
                            "ev-" + sourceId,
                            EvidenceKind.SUPPORTING,
                            sourceId,
                            sourceId,
                            excerpt,
                            1.0,
                            "retrieved and read by LangGraph4j agent");
                })
                .collect(Collectors.toList());
    }

    private ExecutionMetrics buildMetrics(ExecutionTrace trace, List<Evidence> evidence,
                                          long durationMs, int modelCalls) {
        int searches = countEvents(trace, ExecutionEventType.KNOWLEDGE_SEARCH_COMPLETED);
        int docReads = countEvents(trace, ExecutionEventType.DOCUMENT_READ_COMPLETED);
        return new ExecutionMetrics(durationMs, modelCalls, 0, searches, docReads, evidence.size(), 0);
    }

    private int countEvents(ExecutionTrace trace, ExecutionEventType type) {
        return (int) trace.events().stream().filter(e -> e.type() == type).count();
    }

    private ExecutionEvent agentEvent(ExecutionEventType type, Map<String, String> metadata) {
        return new ExecutionEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                type,
                FRAMEWORK_ID,
                type.name().toLowerCase(),
                type.name(),
                metadata,
                null);
    }
}
