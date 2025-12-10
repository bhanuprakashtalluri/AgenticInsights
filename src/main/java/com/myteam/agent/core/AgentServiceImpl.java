package com.myteam.agent.core;

import com.myteam.agent.core.dto.AgentRequest;
import com.myteam.agent.core.dto.AgentResponse;
import com.myteam.agent.core.dto.ToolInvocation;
import com.myteam.agent.core.policy.RbacPolicyMapper;
import com.myteam.agent.mcp.McpLeader;
import org.example.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AgentServiceImpl implements AgentService {

    @Autowired
    private McpLeader mcpLeader;

    @Autowired
    private AuditLogService auditLogService; // Assuming from existing

    @Override
    public AgentResponse execute(AgentRequest request) {
        // Step 1: RBAC Check
        if (!RbacPolicyMapper.canExecuteAgent(request.getRole())) {
            String auditId = auditLogService.log(request.getActor(), "AGENT_EXECUTE_DENIED", "RBAC denied for role: " + request.getRole());
            return new AgentResponse("error", "Access denied", List.of("RBAC check"), new ArrayList<>(), auditId, null, List.of("Insufficient permissions"));
        }

        // Step 2: Mock intent parsing and tool invocation
        List<String> steps = new ArrayList<>();
        steps.add("Parsed request");
        List<ToolInvocation> toolCalls = new ArrayList<>();

        if (request.getText().toLowerCase().contains("count")) {
            ToolInvocation invocation = mcpLeader.invokeTool("getEmployeeCount", Map.of());
            toolCalls.add(invocation);
            steps.add("Invoked getEmployeeCount tool");
        } else if (request.getText().toLowerCase().contains("recognize")) {
            Map<String, Object> input = Map.of(
                "sender", request.getActor(),
                "recipient", "someone@example.com", // mock
                "message", request.getText()
            );
            ToolInvocation invocation = mcpLeader.invokeTool("sendRecognition", input);
            toolCalls.add(invocation);
            steps.add("Invoked sendRecognition tool");
        } else {
            steps.add("No matching tool found");
        }

        // Step 3: Audit
        String auditId = auditLogService.log(request.getActor(), "AGENT_EXECUTE_SUCCESS", "Executed with " + toolCalls.size() + " tool calls");

        // Step 4: Build response
        return new AgentResponse("ok", "Execution completed", steps, toolCalls, auditId, null, new ArrayList<>());
    }
}
