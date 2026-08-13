package com.jachwisunbae.member.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jachwisunbae.common.IntegrationTest;
import com.jachwisunbae.member.client.VerifiedGoogleProfile;
import com.jachwisunbae.member.domain.Member;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class MemberAuthenticationServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private MemberAuthenticationService memberAuthenticationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void deleteMembers() {
        jdbcTemplate.update("DELETE FROM members");
    }

    @DisplayName("동일한 Google 사용자의 동시 최초 로그인은 한 회원으로 수렴한다")
    @Test
    void concurrentFirstLoginCreatesSingleMember() throws Exception {
        final int requestCount = 8;
        final ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        final CountDownLatch ready = new CountDownLatch(requestCount);
        final CountDownLatch start = new CountDownLatch(1);
        final VerifiedGoogleProfile profile = new VerifiedGoogleProfile(
                "concurrent-google-subject",
                "member@example.com",
                "동시 로그인 회원"
        );

        try {
            final List<CompletableFuture<Member>> futures = java.util.stream.IntStream.range(0, requestCount)
                    .mapToObj(index -> CompletableFuture.supplyAsync(() -> {
                        ready.countDown();
                        await(start);
                        return memberAuthenticationService.authenticate(profile);
                    }, executor))
                    .toList();

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            final List<Member> members = futures.stream().map(CompletableFuture::join).toList();

            assertThat(members).extracting(Member::id).containsOnly(members.getFirst().id());
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM members WHERE oauth_subject = 'concurrent-google-subject'",
                    Long.class
            )).isEqualTo(1L);
        } finally {
            executor.shutdownNow();
        }
    }

    private void await(final CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("동시 로그인 테스트 대기가 중단되었습니다.", exception);
        }
    }
}
