package com.myteam.agent.mcp;

import com.myteam.agent.core.dto.ToolInvocation;
import org.springframework.ai.model.function.FunctionCallback;

import java.util.List;
import java.util.Map;

public interface McpLeader {

    // Register a tool
    void registerTool(String toolName, ToolFunction toolFunction);

    // Invoke a tool
    ToolInvocation invokeTool(String toolName, Map<String, Object> input);

    // List available tools
    List<String> listTools();

    // Get function callbacks for Spring AI
    List<FunctionCallback> getFunctionCallbacks();

    // Functional interface for tool functions
    @FunctionalInterface
    interface ToolFunction {
        Map<String, Object> execute(Map<String, Object> input);
    }
}
