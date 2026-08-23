package com.jachwisunbae.auth.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class AuthModeGuardTest {

    @Test
    void 운영에서_데모_인증으로_시작할_수_없다() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        AuthModeGuard guard = new AuthModeGuard(environment, "demo");

        assertThatThrownBy(guard::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AUTH_MODE=google");
    }

    @Test
    void 운영_Google과_로컬_데모_인증을_허용한다() {
        MockEnvironment prod = new MockEnvironment();
        prod.setActiveProfiles("prod");
        MockEnvironment local = new MockEnvironment();
        local.setActiveProfiles("local");

        assertThatCode(() -> new AuthModeGuard(prod, "google").validate())
                .doesNotThrowAnyException();
        assertThatCode(() -> new AuthModeGuard(local, "demo").validate())
                .doesNotThrowAnyException();
    }
}
