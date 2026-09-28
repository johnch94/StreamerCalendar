package com.example.streamercalendar.dto;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * 현재 로그인 상태. 비로그인 사용자도 200으로 받는다 (프론트가 관리자 메뉴 노출 여부만 판단하면 되므로).
 */
public record AuthStatusResponse(
        boolean authenticated,
        String username
) {
    public static AuthStatusResponse from(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return new AuthStatusResponse(false, null);
        }
        return new AuthStatusResponse(true, authentication.getName());
    }
}
