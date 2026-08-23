package dev.jsanca.argonaut.langgraph4j.config;

import dev.jsanca.argonaut.core.knowledge.KnowledgeRepository;
import dev.jsanca.argonaut.core.knowledge.local.LocalKnowledgeRepository;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class LangGraph4jConfig {

    @Bean
    public KnowledgeRepository knowledgeRepository() {
        return LocalKnowledgeRepository.withDemoCorpus();
    }

    @Bean
    public ChatModel chatModel(
            @Value("${openrouter.base-url}") String baseUrl,
            @Value("${openrouter.api-key}") String apiKey,
            @Value("${openrouter.model}") String model) {

        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .build();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**").allowedOrigins("*").allowedMethods("GET", "POST");
            }
        };
    }
}
