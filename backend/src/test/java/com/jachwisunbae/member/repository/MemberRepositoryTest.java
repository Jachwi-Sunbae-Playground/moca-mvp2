package com.jachwisunbae.member.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jachwisunbae.common.RepositoryTest;
import com.jachwisunbae.member.domain.Member;
import com.jachwisunbae.member.domain.OAuthProvider;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

class MemberRepositoryTest extends RepositoryTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        memberRepository = new MemberRepository(jdbcTemplate);
    }

    @DisplayName("Google 프로필을 저장하고 생성된 회원을 조회한다")
    @Test
    void upsertNewMember() {
        final Instant loginAt = Instant.parse("2026-08-10T01:00:00Z");

        final Member member = memberRepository.upsert(
                OAuthProvider.GOOGLE,
                "google-subject",
                "member@example.com",
                "회원",
                loginAt
        );

        assertThat(member.id()).isPositive();
        assertThat(member.oauthProvider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(member.oauthSubject()).isEqualTo("google-subject");
        assertThat(member.lastLoginAt()).isEqualTo(loginAt);
        assertThat(memberRepository.findById(member.id())).contains(member);
    }

    @DisplayName("같은 Google 사용자가 다시 로그인하면 기존 회원 프로필과 로그인 시각을 갱신한다")
    @Test
    void upsertExistingMember() {
        final Member first = memberRepository.upsert(
                OAuthProvider.GOOGLE,
                "same-google-subject",
                "before@example.com",
                "변경 전",
                Instant.parse("2026-08-10T01:00:00Z")
        );

        final Member second = memberRepository.upsert(
                OAuthProvider.GOOGLE,
                "same-google-subject",
                "after@example.com",
                "변경 후",
                Instant.parse("2026-08-10T02:00:00Z")
        );

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.email()).isEqualTo("after@example.com");
        assertThat(second.displayName()).isEqualTo("변경 후");
        assertThat(second.lastLoginAt()).isEqualTo(Instant.parse("2026-08-10T02:00:00Z"));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM members WHERE oauth_subject = 'same-google-subject'",
                Long.class
        )).isEqualTo(1L);
    }

    @DisplayName("같은 OAuth 제공자와 Google subject를 가진 회원을 DB 유일 제약으로 거부한다")
    @Test
    void rejectDuplicatedProviderAndSubject() {
        final String insertSql = """
                INSERT INTO members (
                    oauth_provider,
                    oauth_subject,
                    email,
                    display_name,
                    last_login_at
                ) VALUES ('GOOGLE', 'duplicated-subject', ?, '회원', CURRENT_TIMESTAMP(6))
                """;
        jdbcTemplate.update(insertSql, "first@example.com");

        assertThatThrownBy(() -> jdbcTemplate.update(insertSql, "second@example.com"))
                .isInstanceOf(DuplicateKeyException.class);
    }
}
