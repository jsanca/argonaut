package dev.jsanca.argonaut.langchain4j.config;

import dev.jsanca.argonaut.core.knowledge.KnowledgeRepository;
import dev.jsanca.argonaut.core.knowledge.local.LocalKnowledgeRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class LangChain4jConfig {

    @Bean
    KnowledgeRepository knowledgeRepository() {
        return LocalKnowledgeRepository.withDemoCorpus();
    }

    @Bean
    ChatLanguageModel chatLanguageModel(
            @Value("${openrouter.base-url:https://openrouter.ai/api/v1}") String baseUrl,
            @Value("${openrouter.api-key:}") String apiKey,
            @Value("${openrouter.model:}") String modelName) {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey.isBlank() ? "no-key" : apiKey)
                .modelName(modelName.isBlank() ? "openai/gpt-4o-mini" : modelName)
                .build();
    }

    @Bean
    WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**").allowedOrigins("*").allowedMethods("GET", "POST");
            }
        };
    }
}
