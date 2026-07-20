package com.example.streamercalendar.dto;

import jakarta.validation.constraints.NotBlank;

public record StreamerCreateRequest(
        @NotBlank(message = "name은 필수입니다.") String name,
        String profileImageUrl
) {
}
