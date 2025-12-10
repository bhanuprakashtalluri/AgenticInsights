package com.myteam.agent.mcp;

import com.myteam.agent.core.dto.ToolInvocation;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class McpLeaderImpl implements McpLeader {

    private final Map<String, ToolFunction> tools = new ConcurrentHashMap<>();

    // Constructor to register mock tools
    public McpLeaderImpl() {
        registerTool("getEmployeeCount", input -> {
            // Mock: return a fixed count
            Map<String, Object> output = new HashMap<>();
            output.put("count", 42);
            return output;
        });

        registerTool("sendRecognition", input -> {
            // Mock: simulate sending recognition
            String sender = (String) input.get("sender");
            String recipient = (String) input.get("recipient");
            String message = (String) input.get("message");
            Map<String, Object> output = new HashMap<>();
            output.put("status", "sent");
            output.put("id", "mock-recog-" + System.currentTimeMillis());
            return output;
        });
    }

    @Override
    public void registerTool(String toolName, ToolFunction toolFunction) {
        tools.put(toolName, toolFunction);
    }

    @Override
    public ToolInvocation invokeTool(String toolName, Map<String, Object> input) {
        ToolFunction tool = tools.get(toolName);
        if (tool == null) {
            return new ToolInvocation(toolName, input, Map.of("error", "Tool not found"), 0, false);
        }
        long start = System.currentTimeMillis();
        try {
            Map<String, Object> output = tool.execute(input);
            long duration = System.currentTimeMillis() - start;
            return new ToolInvocation(toolName, input, output, duration, true);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            return new ToolInvocation(toolName, input, Map.of("error", e.getMessage()), duration, false);
        }
    }

    @Override
    public List<String> listTools() {
        return List.copyOf(tools.keySet());
    }
}
