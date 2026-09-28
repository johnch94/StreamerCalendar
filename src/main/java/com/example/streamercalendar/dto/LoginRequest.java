package com.example.streamercalendar.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @param rememberMe "로그인 상태 유지" 여부. 생략하면 false
 */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password,
        Boolean rememberMe
) {
    public boolean isRememberMe() {
        return Boolean.TRUE.equals(rememberMe);
    }
}
