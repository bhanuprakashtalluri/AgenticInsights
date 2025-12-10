package com.myteam.agent.memory;

import java.util.List;

public interface EmbeddingIndex {
    void addEmbedding(String sessionId, float[] embedding, String text);
    List<String> querySimilar(String sessionId, float[] embedding, int topK);
}

