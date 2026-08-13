package com.jachwisunbae.property.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jachwisunbae.common.IntegrationTest;
import com.jachwisunbae.property.service.dto.command.CreatePropertyCommand;
import com.jachwisunbae.property.service.dto.command.SavePropertyMemoCommand;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class PropertyMemoConcurrencyIntegrationTest extends IntegrationTest {

    @Autowired
    private PropertyCommandService propertyCommandService;

    @Autowired
    private PropertyQueryService propertyQueryService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long memberId;
    private long propertyId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM properties");
        jdbcTemplate.update("DELETE FROM members");
        memberId = saveMember("memo-concurrency-owner");
        propertyId = propertyCommandService.createProperty(
                memberId,
                new CreatePropertyCommand("동시 메모 매물", 1, 2, "발견 경로")
        ).propertyId();
    }

    @DisplayName("같은 매물의 동시 메모 저장은 행 잠금으로 직렬화되고 완전한 마지막 요청 하나가 최종값이 된다")
    @Test
    void keepLastCommittedMemo() throws Exception {
        final List<Throwable> failures = runConcurrently(
                () -> saveMemo("첫 동시 메모"),
                () -> saveMemo("둘째 동시 메모")
        );

        assertThat(failures).containsOnlyNulls();
        final var finalMemo = propertyQueryService.getProperty(memberId, propertyId).memo();
        assertThat(finalMemo.additionalMemo()).isIn("첫 동시 메모", "둘째 동시 메모");
        assertThat(finalMemo.viewingSchedule()).isEqualTo(finalMemo.additionalMemo() + " 일정");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT memo FROM properties WHERE id = ?",
                String.class,
                propertyId
        )).isEqualTo(finalMemo.additionalMemo());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM property_pre_visit_memos WHERE property_id = ?",
                Long.class,
                propertyId
        )).isOne();
    }

    private void saveMemo(final String additionalMemo) {
        propertyCommandService.saveMemo(
                memberId,
                propertyId,
                SavePropertyMemoCommand.replace(
                        additionalMemo + " 일정",
                        "입주 가능일",
                        "가계약금",
                        "방 옵션",
                        "관리비와 공과금",
                        "통학 시간",
                        "정부 지원",
                        additionalMemo
                )
        );
    }

    private List<Throwable> runConcurrently(final Runnable first, final Runnable second) throws Exception {
        final CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            final Future<Throwable> firstFuture = executor.submit(() -> runAfter(start, first));
            final Future<Throwable> secondFuture = executor.submit(() -> runAfter(start, second));
            start.countDown();
            return Arrays.asList(firstFuture.get(), secondFuture.get());
        }
    }

    private Throwable runAfter(final CountDownLatch start, final Runnable action) {
        try {
            start.await();
            action.run();
            return null;
        } catch (Throwable throwable) {
            return throwable;
        }
    }

    private long saveMember(final String subject) {
        jdbcTemplate.update(
                """
                INSERT INTO members (
                    oauth_provider, oauth_subject, email, display_name, last_login_at
                ) VALUES ('GOOGLE', ?, ?, '회원', CURRENT_TIMESTAMP(6))
                """,
                subject,
                subject + "@example.com"
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM members WHERE oauth_subject = ?",
                Long.class,
                subject
        );
    }
}
