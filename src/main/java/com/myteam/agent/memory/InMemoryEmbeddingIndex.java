package com.myteam.agent.memory;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class InMemoryEmbeddingIndex implements EmbeddingIndex {
    private final Map<String, List<Entry>> index = new HashMap<>();

    private static class Entry {
        float[] embedding;
        String text;
        Entry(float[] embedding, String text) {
            this.embedding = embedding;
            this.text = text;
        }
    }

    @Override
    public void addEmbedding(String sessionId, float[] embedding, String text) {
        index.putIfAbsent(sessionId, new ArrayList<>());
        index.get(sessionId).add(new Entry(embedding, text));
    }

    @Override
    public List<String> querySimilar(String sessionId, float[] embedding, int topK) {
        List<Entry> entries = index.getOrDefault(sessionId, new ArrayList<>());
        entries.sort(Comparator.comparingDouble(e -> -cosineSimilarity(e.embedding, embedding)));
        List<String> results = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, entries.size()); i++) {
            results.add(entries.get(i).text);
        }
        return results;
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB) + 1e-8);
    }
}

