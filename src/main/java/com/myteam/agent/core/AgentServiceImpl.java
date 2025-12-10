package com.myteam.agent.core;

import com.myteam.agent.ai.FunctionCallingService;
import com.myteam.agent.ai.PromptService;
import com.myteam.agent.core.dto.AgentRequest;
import com.myteam.agent.core.dto.AgentResponse;
import com.myteam.agent.core.dto.ToolInvocation;
import com.myteam.agent.core.policy.RbacPolicyMapper;
import com.myteam.agent.mcp.McpLeader;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AgentServiceImpl implements AgentService {
    @Autowired
    private McpLeader mcpLeader;

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private PromptService promptService;

    @Autowired
    private FunctionCallingService functionCallingService;

    @Autowired
    private com.myteam.agent.memory.ConversationMemoryStore memoryStore;

    @Autowired
    private com.myteam.agent.memory.InMemoryEmbeddingIndex embeddingIndex;

    private final ChatMemory chatMemory = new InMemoryChatMemory();

    @Override
    public AgentResponse execute(AgentRequest request) {
        // Step 1: RBAC Check
        if (!RbacPolicyMapper.canExecuteAgent(request.getRole())) {
            return new AgentResponse("error", "Access denied", List.of("RBAC check"), new ArrayList<>(), "denied", null, List.of("Insufficient permissions"));
        }

        // Step 2: Use PromptService for system prompt
        var params = Map.<String, Object>of(
            "role", request.getRole(),
            "safety", "standard",
            "redaction", "enabled"
        );
        String systemPrompt = promptService.render("system", params);

        // Step 3: Add user turn to memory
        memoryStore.addTurn(request.getSessionId(), request.getText());

        // Step 4: Retrieve context from embedding index (stub: not used in prompt yet)
        // Step 5: AI call with prompt
        List<String> steps = new ArrayList<>();
        steps.add("AI processing request");
        List<ToolInvocation> toolCalls = new ArrayList<>();

        try {
            ChatClient chatClient = ChatClient.builder(chatModel)
                    .defaultAdvisors(new org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor(chatMemory))
                    .build();

            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(request.getText())
                    .advisors(a -> a.param("chat_memory_conversation_id", request.getSessionId()))
                    .call()
                    .content();

            // Step 6: Function calling (if intent/toolName present)
            ToolInvocation toolInvocation = null;
            if (request.getPayload() != null && request.getPayload().containsKey("toolName")) {
                toolInvocation = functionCallingService.routeAndCall(request);
                if (toolInvocation != null) {
                    toolCalls.add(toolInvocation);
                }
            }

            // Step 7: Audit (dropped)
            String auditId = "dropped";

            // Step 8: Build response
            return new AgentResponse("ok", response, steps, toolCalls, auditId, null, new ArrayList<>());
        } catch (Exception e) {
            String auditId = "dropped";
            return new AgentResponse("error", e.getMessage(), steps, toolCalls, auditId, null, List.of(e.getMessage()));
        }
    }
}
