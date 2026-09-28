package com.example.streamercalendar.config;

import com.example.streamercalendar.dto.ErrorResponse;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 접근 권한: 사용자(비로그인)는 스트리머/방송 기록 조회만, 등록·수정·삭제는 관리자(ADMIN)만 가능하다.
 * 인증은 세션 방식이다. POST /api/auth/login에 성공하면 JSESSIONID 쿠키(HttpOnly)가 발급되고,
 * "로그인 상태 유지"를 선택하면 remember-me 쿠키(14일)도 함께 발급된다.
 *
 * 조회 허용 경로를 "GET /api/**" 대신 하나씩 나열한 이유:
 * Phase 2의 GET /api/youtube-candidates처럼 관리자 전용 조회 API가 추가될 예정이라,
 * 새 경로는 기본적으로 관리자 전용이 되도록 막아두고 공개할 경로만 명시적으로 연다.
 *
 * CSRF를 끈 이유: 세션 쿠키를 SameSite=Lax로 발급(application.properties)하므로
 * 다른 사이트에서 보낸 POST/PUT/DELETE에는 쿠키가 실리지 않는다. 또 API는 JSON Body만 받고,
 * CORS로 허용 origin을 제한한다. 운영에서는 Vercel이 /api를 같은 도메인으로 프록시하므로 이 전제가 유지된다.
 * 프론트와 API를 서로 다른 사이트(도메인)에서 직접 호출하게 바꿔 SameSite=None이 필요해지면 CSRF 토큰을 다시 켜야 한다.
 *
 * CORS 설정을 WebMvcConfigurer가 아니라 여기(SecurityFilterChain)에 두는 이유:
 * Spring Security가 필터 체인에서 요청을 먼저 가로채기 때문에,
 * WebMvcConfigurer#addCorsMappings로 등록한 CORS 설정은 무시될 수 있다.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    // app.cors.allowed-origins (쉼표 구분). 로컬 개발 기본값은 http://localhost:5173
    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, RememberMeServices rememberMeServices) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/streamers", "/api/streamers/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/streams", "/api/streams/**").permitAll()
                .anyRequest().hasRole(AdminAccountConfig.ROLE_ADMIN)
            )
            // 세션이 없어도 remember-me 쿠키가 유효하면 관리자로 자동 로그인. 로그아웃 시 쿠키도 함께 삭제된다
            .rememberMe(rememberMe -> rememberMe.rememberMeServices(rememberMeServices))
            .logout(logout -> logout
                .logoutRequestMatcher(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/auth/logout"))
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, e) ->
                    writeError(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "관리자 로그인이 필요합니다."))
                .accessDeniedHandler((request, response, e) ->
                    writeError(response, HttpStatus.FORBIDDEN, "FORBIDDEN", "관리자만 사용할 수 있는 기능입니다."))
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        // 세션 쿠키를 주고받기 위해 필요 (프론트는 fetch에 credentials: 'include' 사용)
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    // 필터 단계의 401/403은 GlobalExceptionHandler까지 오지 않으므로 공통 에러 포맷을 여기서 직접 쓴다
    private void writeError(HttpServletResponse response, HttpStatus status, String code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(code, message));
    }
}
