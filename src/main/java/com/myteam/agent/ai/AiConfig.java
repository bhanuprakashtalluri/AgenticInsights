package com.myteam.agent.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.chat.model.ChatModel;

@Configuration
public class AiConfig {
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    @Bean
    public ChatModel chatModel() {
        OpenAiApi openAiApi = new OpenAiApi(apiKey, baseUrl);
        OpenAiChatOptions options = OpenAiChatOptions.builder().withModel(model).build();
        return new OpenAiChatModel(openAiApi, options);
    }
}
