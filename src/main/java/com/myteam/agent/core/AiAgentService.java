package com.myteam.agent.core;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AiAgentService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory = new InMemoryChatMemory();

    @Autowired
    public AiAgentService(ChatModel chatModel, List<FunctionCallback> functionCallbacks) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
                .defaultFunctions(functionCallbacks.toArray(new FunctionCallback[0]))
                .build();
    }

    public String processRequest(String text, String role, String sessionId) {
        return chatClient.prompt()
                .system("You are an AI assistant for MyTeam. User role: " + role)
                .user(text)
                .advisors(a -> a.param("chat_memory_conversation_id", sessionId))
                .call()
                .content();
    }
}
