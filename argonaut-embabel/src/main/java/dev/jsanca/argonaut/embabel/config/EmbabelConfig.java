package dev.jsanca.argonaut.embabel.config;

import dev.jsanca.argonaut.core.knowledge.KnowledgeRepository;
import dev.jsanca.argonaut.core.knowledge.local.LocalKnowledgeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class EmbabelConfig {

    @Bean
    KnowledgeRepository knowledgeRepository() {
        return LocalKnowledgeRepository.withDemoCorpus();
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
