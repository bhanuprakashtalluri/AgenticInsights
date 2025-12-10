package com.myteam.agent.memory;

import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Component
public class ConversationMemoryStore {
    private final Map<String, LinkedList<String>> sessionMemory = new HashMap<>();
    private final int maxTurns = 10;

    public void addTurn(String sessionId, String message) {
        sessionMemory.putIfAbsent(sessionId, new LinkedList<>());
        LinkedList<String> turns = sessionMemory.get(sessionId);
        if (turns.size() >= maxTurns) {
            turns.removeFirst();
        }
        turns.addLast(message);
    }

    public List<String> getTurns(String sessionId) {
        return sessionMemory.getOrDefault(sessionId, new LinkedList<>());
    }

    public void clearSession(String sessionId) {
        sessionMemory.remove(sessionId);
    }
}
