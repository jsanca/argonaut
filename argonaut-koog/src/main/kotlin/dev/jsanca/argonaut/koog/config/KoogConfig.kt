package dev.jsanca.argonaut.koog.config

import ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import dev.jsanca.argonaut.core.knowledge.KnowledgeRepository
import dev.jsanca.argonaut.core.knowledge.local.LocalKnowledgeRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class KoogConfig {

    @Bean
    fun knowledgeRepository(): KnowledgeRepository = LocalKnowledgeRepository.withDemoCorpus()

    @Bean
    fun promptExecutor(
        @Value("\${openrouter.api-key:not-set}") apiKey: String
    ): PromptExecutor {
        val client = OpenRouterLLMClient(apiKey = apiKey)
        return MultiLLMPromptExecutor(mapOf(LLMProvider.OpenRouter to client))
    }

    @Bean
    fun llmModel(
        @Value("\${openrouter.model:openai/gpt-4o-mini}") modelId: String
    ): LLModel = LLModel(
        provider = LLMProvider.OpenRouter,
        id = modelId,
        capabilities = listOf(LLMCapability.Tools, LLMCapability.Completion, LLMCapability.Temperature)
    )

    @Bean
    fun corsConfigurer(): WebMvcConfigurer = object : WebMvcConfigurer {
        override fun addCorsMappings(registry: CorsRegistry) {
            registry.addMapping("/api/**").allowedOrigins("*").allowedMethods("GET", "POST")
        }
    }
}
