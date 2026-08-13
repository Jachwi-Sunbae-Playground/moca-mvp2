package com.jachwisunbae.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SensitiveRejectedValuePolicyTest {

    private final SensitiveRejectedValuePolicy policy = new SensitiveRejectedValuePolicy();

    @DisplayName("토큰과 인증정보의 거절 값은 검증 오류에서 제거한다")
    @Test
    void removeSensitiveRejectedValue() {
        assertThat(policy.sanitize("authorizationCode", "secret-code")).isNull();
        assertThat(policy.sanitize("codeVerifier", "secret-verifier")).isNull();
        assertThat(policy.sanitize("nonce", "secret-nonce")).isNull();
        assertThat(policy.sanitize("accessToken", "secret-token")).isNull();
        assertThat(policy.sanitize("content", "private-memo")).isNull();
    }

    @DisplayName("민감하지 않은 입력값은 검증 오류에 유지한다")
    @Test
    void keepNonSensitiveRejectedValue() {
        assertThat(policy.sanitize("displayName", "")).isEqualTo("");
    }
}
