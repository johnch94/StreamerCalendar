package com.example.streamercalendar.support;

import com.example.streamercalendar.config.AdminAccountConfig;
import com.example.streamercalendar.config.SecurityConfig;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @WebMvcTest 슬라이스에 실제 보안 설정(권한 규칙, remember-me, 401/403 응답)을 올린다.
 * 관리자 계정은 테스트용 값으로 고정: admin / secret1234
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({SecurityConfig.class, AdminAccountConfig.class})
@TestPropertySource(properties = {
        "app.admin.username=admin",
        "app.admin.password={noop}secret1234",
        "app.admin.remember-me-key=test-remember-me-key"
})
public @interface ImportSecurityConfig {
}
