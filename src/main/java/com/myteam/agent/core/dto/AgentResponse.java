package com.myteam.agent.core.dto;

import java.util.List;
import java.util.Map;

public class AgentResponse {
    private String status; // "ok" or "error"
    private String message; // human-readable message
    private List<String> steps; // list of steps taken
    private List<ToolInvocation> toolCalls; // list of tool invocations
    private String auditId; // audit log ID
    private Object data; // any additional data
    private List<String> errors; // list of errors if any

    // Constructors
    public AgentResponse() {}

    public AgentResponse(String status, String message, List<String> steps, List<ToolInvocation> toolCalls, String auditId, Object data, List<String> errors) {
        this.status = status;
        this.message = message;
        this.steps = steps;
        this.toolCalls = toolCalls;
        this.auditId = auditId;
        this.data = data;
        this.errors = errors;
    }

    // Getters and Setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<String> getSteps() { return steps; }
    public void setSteps(List<String> steps) { this.steps = steps; }

    public List<ToolInvocation> getToolCalls() { return toolCalls; }
    public void setToolCalls(List<ToolInvocation> toolCalls) { this.toolCalls = toolCalls; }

    public String getAuditId() { return auditId; }
    public void setAuditId(String auditId) { this.auditId = auditId; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
}
