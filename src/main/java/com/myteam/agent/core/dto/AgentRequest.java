package com.myteam.agent.core.dto;

import java.util.Map;

public class AgentRequest {
    private String actor; // userId or email
    private String role; // e.g., "ADMIN", "MANAGER", etc.
    private String text; // user input text
    private String intent; // optional intent
    private Map<String, Object> payload; // additional data
    private String sessionId; // session identifier
    private String preferredChannel; // e.g., "api", "chat"
    private String correlationId; // for tracing

    // Constructors
    public AgentRequest() {}

    public AgentRequest(String actor, String role, String text, String intent, Map<String, Object> payload, String sessionId, String preferredChannel, String correlationId) {
        this.actor = actor;
        this.role = role;
        this.text = text;
        this.intent = intent;
        this.payload = payload;
        this.sessionId = sessionId;
        this.preferredChannel = preferredChannel;
        this.correlationId = correlationId;
    }

    // Getters and Setters
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }

    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getPreferredChannel() { return preferredChannel; }
    public void setPreferredChannel(String preferredChannel) { this.preferredChannel = preferredChannel; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
}
