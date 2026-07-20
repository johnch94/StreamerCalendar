package com.example.streamercalendar.dto;

import com.example.streamercalendar.domain.Platform;
import com.example.streamercalendar.domain.Source;
import com.example.streamercalendar.domain.StreamRecord;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record StreamRecordResponse(
        Long id,
        Long streamerId,
        String streamerName,
        LocalDate broadcastDate,
        Platform platform,
        String title,
        String vodUrl,
        String youtubeUrl,
        Source source,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static StreamRecordResponse from(StreamRecord record) {
        return new StreamRecordResponse(
                record.getId(),
                record.getStreamer().getId(),
                record.getStreamer().getName(),
                record.getBroadcastDate(),
                record.getPlatform(),
                record.getTitle(),
                record.getVodUrl(),
                record.getYoutubeUrl(),
                record.getSource(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}
