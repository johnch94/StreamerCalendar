package com.example.streamercalendar.dto;

import com.example.streamercalendar.domain.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record StreamRecordRequest(
        @NotNull(message = "streamerId는 필수입니다.") Long streamerId,
        @NotNull(message = "broadcastDate는 필수입니다.") LocalDate broadcastDate,
        @NotNull(message = "platform은 필수입니다.") Platform platform,
        @NotBlank(message = "title은 필수입니다.") String title,
        String vodUrl,
        String youtubeUrl
) {
}
