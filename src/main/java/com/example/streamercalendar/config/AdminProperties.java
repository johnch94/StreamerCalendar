package com.example.streamercalendar.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 관리자 계정 정보 (app.admin.*). 관리자는 1명이라 DB 테이블 대신 설정으로 관리한다.
 * password는 DelegatingPasswordEncoder 형식의 해시값이어야 한다. 예) {bcrypt}$2a$10$...
 * rememberMeKey는 "로그인 상태 유지" 쿠키 서명용 비밀 키다. 바꾸면 발급된 쿠키가 모두 무효가 된다.
 * 값이 비어 있으면 애플리케이션 기동 시점에 실패한다 (관리자 없이 조용히 뜨는 것 방지).
 */
@Validated
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String rememberMeKey
) {
}
