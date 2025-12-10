package com.myteam.agent.core.dto;

import java.util.Map;

public class ToolInvocation {
    private String toolName;
    private Map<String, Object> input;
    private Map<String, Object> output;
    private long durationMs;
    private boolean success;

    // Constructors
    public ToolInvocation() {}

    public ToolInvocation(String toolName, Map<String, Object> input, Map<String, Object> output, long durationMs, boolean success) {
        this.toolName = toolName;
        this.input = input;
        this.output = output;
        this.durationMs = durationMs;
        this.success = success;
    }

    // Getters and Setters
    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }

    public Map<String, Object> getInput() { return input; }
    public void setInput(Map<String, Object> input) { this.input = input; }

    public Map<String, Object> getOutput() { return output; }
    public void setOutput(Map<String, Object> output) { this.output = output; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
}
