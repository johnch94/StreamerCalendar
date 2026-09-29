package com.example.streamercalendar.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
        // 접두사가 없으면 기동은 되지만 로그인 시점에야 DelegatingPasswordEncoder가 예외를 던지므로 기동 단계에서 막는다.
        // {bcrypt}는 뒤에 실제 BCrypt 해시($2a$10$ + 53자)가 와야 한다 ({bcrypt}평문 을 넣으면 어떤 비밀번호로도 로그인이 안 됨)
        @NotBlank
        @Pattern(regexp = "^(\\{bcrypt}\\$2[aby]?\\$\\d{2}\\$[./A-Za-z0-9]{53}|\\{(?!bcrypt})[a-z0-9]+}.+)$",
                message = "\\{bcrypt\\} 같은 인코딩 접두사가 붙은 해시여야 합니다. ./gradlew hashPassword -Ppassword=... 출력 전체를 넣어주세요")
        String password,
        @NotBlank String rememberMeKey
) {
}
