package com.jachwisunbae.auth.nickname;

import com.jachwisunbae.common.exception.BusinessException;
import com.jachwisunbae.common.exception.DomainErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NicknameLoginAttemptLimiter {

    private final ConcurrentHashMap<String, AttemptWindow> attempts = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int maxFailures;
    private final long windowSeconds;

    public NicknameLoginAttemptLimiter(
            Clock clock,
            @Value("${auth.nickname.max-failures:5}") int maxFailures,
            @Value("${auth.nickname.failure-window-seconds:600}") long windowSeconds) {
        this.clock = clock;
        this.maxFailures = maxFailures;
        this.windowSeconds = windowSeconds;
    }

    public void checkAllowed(String nicknameKey) {
        Instant now = clock.instant();
        AttemptWindow window = attempts.get(nicknameKey);
        if (window == null) {
            return;
        }
        if (window.startedAt().plusSeconds(windowSeconds).isBefore(now)) {
            attempts.remove(nicknameKey, window);
            return;
        }
        if (window.failures() >= maxFailures) {
            throw new BusinessException(DomainErrorCode.NICKNAME_AUTH_RATE_LIMITED,
                    "닉네임 로그인 실패 횟수를 초과했습니다.");
        }
    }

    public void recordFailure(String nicknameKey) {
        Instant now = clock.instant();
        attempts.compute(nicknameKey, (key, previous) -> {
            if (previous == null || previous.startedAt().plusSeconds(windowSeconds).isBefore(now)) {
                return new AttemptWindow(1, now);
            }
            return new AttemptWindow(previous.failures() + 1, previous.startedAt());
        });
    }

    public void reset(String nicknameKey) {
        attempts.remove(nicknameKey);
    }

    private record AttemptWindow(int failures, Instant startedAt) {
    }
}
