package com.myteam.agent.ai;

import com.myteam.agent.core.dto.AgentRequest;
import com.myteam.agent.core.dto.ToolInvocation;
import com.myteam.agent.mcp.McpLeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class FunctionCallingService {
    @Autowired
    private McpLeader mcpLeader;

    public ToolInvocation callTool(String toolName, Map<String, Object> input) {
        // Call MCP Leader tool by name
        return mcpLeader.invokeTool(toolName, input);
    }

    public ToolInvocation routeAndCall(AgentRequest request) {
        // Example: route based on intent or payload
        String toolName = (String) request.getPayload().getOrDefault("toolName", "defaultTool");
        return callTool(toolName, request.getPayload());
    }
}

