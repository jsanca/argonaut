package dev.jsanca.argonaut.springai.config;

import dev.jsanca.argonaut.core.knowledge.KnowledgeRepository;
import dev.jsanca.argonaut.core.knowledge.local.LocalKnowledgeRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SpringAiConfig {

    @Bean
    KnowledgeRepository knowledgeRepository() {
        return LocalKnowledgeRepository.withDemoCorpus();
    }

    @Bean
    ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
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
