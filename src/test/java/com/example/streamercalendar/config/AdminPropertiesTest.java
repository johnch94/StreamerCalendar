package com.example.streamercalendar.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AdminPropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<ConstraintViolation<AdminProperties>> validate(String password) {
        return validator.validate(new AdminProperties("admin", password, "key"));
    }

    private static final String BCRYPT_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    @Test
    void 인코딩_접두사가_붙은_해시는_통과한다() {
        assertThat(validate("{bcrypt}" + BCRYPT_HASH)).isEmpty();
        assertThat(validate("{noop}local-password")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            BCRYPT_HASH,              // 접두사 없음
            "plain-password",         // 평문
            "{bcrypt}",               // 해시 없음
            "{bcrypt}pw123",          // 접두사 뒤에 해시 대신 평문
    })
    void 접두사가_없거나_해시가_비어있으면_기동_단계에서_막는다(String password) {
        assertThat(validate(password))
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("password");
    }
}
