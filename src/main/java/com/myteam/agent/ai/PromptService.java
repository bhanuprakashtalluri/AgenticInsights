package com.myteam.agent.ai;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class PromptService {
    private final Map<String, String> templates = new HashMap<>();

    public PromptService() {
        templates.put("system", "You are an AI assistant for MyTeam. Help with employee recognitions and insights. Role: {role}. Safety: {safety}. Redaction: {redaction}.");
        templates.put("routing_intent", "Extract intent and entities from: {text}. Return JSON schema.");
        templates.put("recognition_classify", "Classify recognition type, category, level, and points for: {text}.");
    }

    public String render(String templateName, Map<String, Object> params) {
        String template = templates.get(templateName);
        if (template == null) return "";
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            template = template.replace("{" + entry.getKey() + "}", entry.getValue().toString());
        }
        return template;
    }
}

