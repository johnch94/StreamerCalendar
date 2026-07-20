package com.example.streamercalendar.dto;

import com.example.streamercalendar.domain.Streamer;

import java.time.OffsetDateTime;

public record StreamerResponse(
        Long id,
        String name,
        String profileImageUrl,
        OffsetDateTime createdAt
) {
    public static StreamerResponse from(Streamer streamer) {
        return new StreamerResponse(
                streamer.getId(),
                streamer.getName(),
                streamer.getProfileImageUrl(),
                streamer.getCreatedAt()
        );
    }
}
