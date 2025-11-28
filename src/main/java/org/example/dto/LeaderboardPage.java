package org.example.dto;

import java.util.List;

public class LeaderboardPage {
    public List<LeaderboardEntry> content;
    public int page;
    public int size;
    public long totalElements;
    public int totalPages;

    public LeaderboardPage() {}

    public LeaderboardPage(List<LeaderboardEntry> content, int page, int size, long totalElements) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = (int) ((totalElements + size - 1) / size);
    }
}

