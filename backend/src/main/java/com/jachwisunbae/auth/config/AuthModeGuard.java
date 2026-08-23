package com.jachwisunbae.auth.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class AuthModeGuard {

    private final Environment environment;
    private final String authMode;

    public AuthModeGuard(Environment environment,
                         @Value("${auth.mode:demo}") String authMode) {
        this.environment = environment;
        this.authMode = authMode;
    }

    @PostConstruct
    void validate() {
        if (environment.acceptsProfiles(Profiles.of("prod"))
                && !"google".equalsIgnoreCase(authMode)) {
            throw new IllegalStateException("prod 프로필은 AUTH_MODE=google만 사용할 수 있습니다.");
        }
    }
}
