package com.example.streamercalendar.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices.RememberMeTokenAlgorithm;

import java.time.Duration;

/**
 * 설정 파일(app.admin.*)에 있는 관리자 1명을 인메모리 계정으로 등록하고,
 * AuthController의 로그인 요청을 처리할 AuthenticationManager와 "로그인 상태 유지"용 RememberMeServices를 만든다.
 */
@Configuration
@EnableConfigurationProperties(AdminProperties.class)
public class AdminAccountConfig {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final Duration REMEMBER_ME_VALIDITY = Duration.ofDays(14);

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(AdminProperties adminProperties) {
        return new InMemoryUserDetailsManager(
                User.withUsername(adminProperties.username())
                        .password(adminProperties.password())
                        .roles(ROLE_ADMIN)
                        .build()
        );
    }

    /**
     * "로그인 상태 유지" 쿠키(remember-me). 세션(2시간)이 끝나거나 브라우저를 닫아도 이 기간 동안 로그인이 유지된다.
     * 쿠키 값은 아이디 + 만료 시각 + 비밀번호 해시 + 키로 서명하므로, 관리자 비밀번호나 키를 바꾸면 기존 쿠키는 무효가 된다.
     */
    @Bean
    public RememberMeServices rememberMeServices(UserDetailsService userDetailsService, AdminProperties adminProperties) {
        TokenBasedRememberMeServices services = new TokenBasedRememberMeServices(
                adminProperties.rememberMeKey(), userDetailsService, RememberMeTokenAlgorithm.SHA256);
        // 기본 동작은 폼 파라미터(remember-me)를 보고 발급 여부를 정하지만, 로그인 API는 JSON Body를 받으므로
        // AuthController가 rememberMe 값을 보고 loginSuccess 호출 여부를 직접 결정한다
        services.setAlwaysRemember(true);
        services.setTokenValiditySeconds((int) REMEMBER_ME_VALIDITY.toSeconds());
        services.setCookieCustomizer(cookie -> cookie.setAttribute("SameSite", "Lax"));
        return services;
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }
}
