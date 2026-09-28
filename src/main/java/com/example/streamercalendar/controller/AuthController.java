package com.example.streamercalendar.controller;

import com.example.streamercalendar.dto.AuthStatusResponse;
import com.example.streamercalendar.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 로그인 API. 로그아웃(POST /api/auth/logout)은 SecurityConfig의 LogoutFilter가 처리한다.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final RememberMeServices rememberMeServices;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    @PostMapping("/login")
    public ResponseEntity<AuthStatusResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        // 실패하면 AuthenticationException → GlobalExceptionHandler에서 401
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password())
        );

        // 세션 고정 공격 방지: 로그인 전에 쓰던 세션이 있으면 ID를 새로 발급
        if (httpRequest.getSession(false) != null) {
            httpRequest.changeSessionId();
        }

        // Spring Security 6+는 인증 정보를 세션에 자동 저장하지 않으므로 직접 저장한다
        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        if (request.isRememberMe()) {
            rememberMeServices.loginSuccess(httpRequest, httpResponse, authentication);
        }

        return ResponseEntity.ok(AuthStatusResponse.from(authentication));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthStatusResponse> me(Authentication authentication) {
        return ResponseEntity.ok(AuthStatusResponse.from(authentication));
    }
}
