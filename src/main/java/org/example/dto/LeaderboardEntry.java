package org.example.dto;

public class LeaderboardEntry {
    public Long id; // senderId or recipientId
    public String name; // populated by mapper
    public Long count;
    public Integer points;

    public LeaderboardEntry() {}

    public LeaderboardEntry(Long id, String name, Long count, Integer points) {
        this.id = id;
        this.name = name;
        this.count = count;
        this.points = points;
    }
}

