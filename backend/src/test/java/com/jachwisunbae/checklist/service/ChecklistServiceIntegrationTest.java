package com.jachwisunbae.checklist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.checklist.domain.ChecklistItemOrigin;
import com.jachwisunbae.checklist.service.dto.command.ChecklistItemCommand;
import com.jachwisunbae.checklist.service.dto.command.ChecklistRequestMode;
import com.jachwisunbae.checklist.service.dto.command.CreateChecklistCommand;
import com.jachwisunbae.checklist.service.dto.command.ReplaceChecklistCommand;
import com.jachwisunbae.common.IntegrationTest;
import com.jachwisunbae.common.exception.JachwiException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.page.PageQuery;
import java.sql.Connection;
import java.util.List;
import java.util.concurrent.Executors;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class ChecklistServiceIntegrationTest extends IntegrationTest {

    @BeforeAll
    static void allowTriggerCreationInTestContainer() throws Exception {
        final var result = MYSQL_CONTAINER.execInContainer(
                "mysql",
                "-uroot",
                "-ptest",
                "-e",
                "SET GLOBAL log_bin_trust_function_creators = 1"
        );
        assertThat(result.getExitCode()).isZero();
    }

    @Autowired
    private ChecklistCommandService checklistCommandService;

    @Autowired
    private ChecklistQueryService checklistQueryService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS fail_checklist_item_insert");
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS delay_checklist_item_insert");
        jdbcTemplate.update("DELETE FROM checklists");
        jdbcTemplate.update("DELETE FROM members");
        jdbcTemplate.update("UPDATE check_items SET is_active = TRUE");
    }

    @DisplayName("회원은 같은 단계의 체크리스트를 여러 개 만들고 항목 추가·삭제·순서를 전체 교체한다")
    @Test
    void manageMultipleChecklists() {
        final long memberId = saveMember("checklist-service-owner");
        final var first = create(memberId, "현장 기본", CheckStage.ON_SITE, 101, 103);
        final var second = create(memberId, "현장 보조", CheckStage.ON_SITE, 105);
        create(memberId, "전화", CheckStage.ONLINE_PHONE, 201);

        final var changed = checklistCommandService.replaceChecklist(
                memberId,
                first.checklistId(),
                new ReplaceChecklistCommand(" 현장 최종 ", List.of(103L, 102L))
        );

        assertThat(changed.name()).isEqualTo("현장 최종");
        assertThat(changed.stage()).isEqualTo(CheckStage.ON_SITE);
        assertThat(changed.items()).extracting(item -> item.sourceCheckItemId()).containsExactly(103L, 102L);
        assertThat(changed.items()).extracting(item -> item.order()).containsExactly(1, 2);
        assertThat(changed.assignedPropertyCount()).isZero();
        assertThat(checklistQueryService.getChecklists(memberId, CheckStage.ON_SITE, PageQuery.of(0, 20)).content())
                .extracting(item -> item.checklistId())
                .containsExactly(first.checklistId(), second.checklistId());

        checklistCommandService.deleteChecklist(memberId, first.checklistId());

        assertError(
                () -> checklistQueryService.getChecklist(memberId, first.checklistId()),
                ErrorCode.CHECKLIST_NOT_FOUND
        );
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM checklist_items WHERE checklist_id = ?",
                Long.class,
                first.checklistId()
        )).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM check_items", Long.class)).isEqualTo(72L);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM checklist_presets", Long.class)).isEqualTo(6L);
    }

    @DisplayName("PROVIDED와 CUSTOM을 함께 생성하고 기존 로컬 ID를 보존해 수정·삭제·재정렬한다")
    @Test
    void manageProvidedAndCustomItemsWithStableIds() {
        final long memberId = saveMember("custom-checklist-owner");
        final long catalogCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM check_items", Long.class);
        final var created = checklistCommandService.createChecklist(
                memberId,
                new CreateChecklistCommand(
                        "혼합 체크리스트",
                        CheckStage.ON_SITE,
                        List.of(
                                ChecklistItemCommand.provided(101),
                                ChecklistItemCommand.custom(null, "  창틀 곰팡이는 괜찮은가?  "),
                                ChecklistItemCommand.provided(102)
                        ),
                        ChecklistRequestMode.V11
                )
        );
        final long providedId = created.items().getFirst().checklistItemId();
        final long customId = created.items().get(1).checklistItemId();

        assertThat(created.items()).extracting(item -> item.origin())
                .containsExactly(ChecklistItemOrigin.PROVIDED, ChecklistItemOrigin.CUSTOM, ChecklistItemOrigin.PROVIDED);
        assertThat(created.items()).extracting(item -> item.sourceCheckItemId())
                .containsExactly(101L, null, 102L);
        assertThat(created.items().get(1).question()).isEqualTo("창틀 곰팡이는 괜찮은가?");
        assertThat(created.items().get(1).guide()).isNull();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM check_items", Long.class)).isEqualTo(catalogCount);

        final var changed = checklistCommandService.replaceChecklist(
                memberId,
                created.checklistId(),
                new ReplaceChecklistCommand(
                        "혼합 변경",
                        List.of(
                                ChecklistItemCommand.custom(customId, "곰팡이 냄새는 괜찮은가?"),
                                ChecklistItemCommand.provided(101),
                                ChecklistItemCommand.custom(null, "환기 상태는 괜찮은가?")
                        ),
                        ChecklistRequestMode.V11
                )
        );

        assertThat(changed.items()).extracting(item -> item.checklistItemId())
                .containsExactly(customId, providedId, changed.items().get(2).checklistItemId());
        assertThat(changed.items().get(2).checklistItemId()).isNotIn(customId, providedId);
        assertThat(changed.items()).extracting(item -> item.question())
                .containsExactly(
                        "곰팡이 냄새는 괜찮은가?",
                        created.items().getFirst().question(),
                        "환기 상태는 괜찮은가?"
                );
        assertThat(changed.items()).extracting(item -> item.order()).containsExactly(1, 2, 3);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM checklist_items WHERE checklist_id = ? AND check_item_id = 102",
                Long.class,
                created.checklistId()
        )).isZero();
    }

    @DisplayName("다른 체크리스트의 CUSTOM 로컬 ID를 숨기고 legacy 전체 변경은 CUSTOM 유실 전에 409로 롤백한다")
    @Test
    void protectCustomOwnershipAndLegacyReplacement() {
        final long memberId = saveMember("custom-protection-owner");
        final var first = createCustom(memberId, "첫 체크리스트", "첫 질문인가?");
        final var second = createCustom(memberId, "둘째 체크리스트", "둘째 질문인가?");
        final long secondCustomId = second.items().getFirst().checklistItemId();

        assertError(
                () -> checklistCommandService.replaceChecklist(
                        memberId,
                        first.checklistId(),
                        new ReplaceChecklistCommand(
                                "로컬 ID 변조",
                                List.of(ChecklistItemCommand.custom(secondCustomId, "변조 질문인가?")),
                                ChecklistRequestMode.V11
                        )
                ),
                ErrorCode.CHECKLIST_ITEM_NOT_FOUND
        );
        assertError(
                () -> checklistCommandService.replaceChecklist(
                        memberId,
                        first.checklistId(),
                        new ReplaceChecklistCommand("legacy 유실", List.of(101L))
                ),
                ErrorCode.CHECKLIST_REQUIRES_V11_CLIENT
        );

        final var unchanged = checklistQueryService.getChecklist(memberId, first.checklistId());
        assertThat(unchanged.name()).isEqualTo("첫 체크리스트");
        assertThat(unchanged.items()).singleElement().satisfies(item -> {
            assertThat(item.origin()).isEqualTo(ChecklistItemOrigin.CUSTOM);
            assertThat(item.question()).isEqualTo("첫 질문인가?");
        });
    }

    @DisplayName("체크리스트는 없는 항목·중복·단계 불일치·새 비활성 항목을 거부한다")
    @Test
    void validateChecklistItems() {
        final long memberId = saveMember("checklist-validation-owner");

        assertError(
                () -> create(memberId, "빈 목록", CheckStage.ON_SITE),
                ErrorCode.CHECKLIST_EMPTY
        );
        assertError(
                () -> create(memberId, "중복", CheckStage.ON_SITE, 101, 101),
                ErrorCode.CHECKLIST_ITEM_DUPLICATED
        );
        assertError(
                () -> create(memberId, "없음", CheckStage.ON_SITE, 999_999),
                ErrorCode.CHECK_ITEM_NOT_FOUND
        );
        assertError(
                () -> create(memberId, "단계", CheckStage.ON_SITE, 201),
                ErrorCode.CHECKLIST_ITEM_STAGE_MISMATCH
        );

        jdbcTemplate.update("UPDATE check_items SET is_active = FALSE WHERE id = 101");
        assertError(
                () -> create(memberId, "비활성", CheckStage.ON_SITE, 101),
                ErrorCode.CHECK_ITEM_INACTIVE
        );
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM checklists", Long.class)).isZero();
    }

    @DisplayName("이미 선택한 항목은 비활성화된 뒤에도 유지·재정렬할 수 있지만 새 비활성 항목은 추가할 수 없다")
    @Test
    void retainExistingInactiveItemOnly() {
        final long memberId = saveMember("inactive-item-owner");
        final var checklist = create(memberId, "비활성 보존", CheckStage.ON_SITE, 101, 102);
        jdbcTemplate.update("UPDATE check_items SET is_active = FALSE WHERE id IN (101, 103)");

        final var changed = checklistCommandService.replaceChecklist(
                memberId,
                checklist.checklistId(),
                new ReplaceChecklistCommand("비활성 재정렬", List.of(102L, 101L))
        );

        assertThat(changed.items()).extracting(item -> item.sourceCheckItemId()).containsExactly(102L, 101L);
        assertError(
                () -> checklistCommandService.replaceChecklist(
                        memberId,
                        checklist.checklistId(),
                        new ReplaceChecklistCommand("비활성 추가", List.of(101L, 103L))
                ),
                ErrorCode.CHECK_ITEM_INACTIVE
        );
        assertThat(checklistQueryService.getChecklist(memberId, checklist.checklistId()).items())
                .extracting(item -> item.sourceCheckItemId())
                .containsExactly(102L, 101L);
    }

    @DisplayName("다른 회원의 체크리스트는 조회·변경·삭제 경로 모두 존재를 숨긴다")
    @Test
    void protectChecklistOwnership() {
        final long ownerId = saveMember("checklist-real-owner");
        final long otherId = saveMember("checklist-other-member");
        final var checklist = create(ownerId, "소유 체크리스트", CheckStage.ON_SITE, 101);

        assertError(
                () -> checklistQueryService.getChecklist(otherId, checklist.checklistId()),
                ErrorCode.CHECKLIST_NOT_FOUND
        );
        assertError(
                () -> checklistCommandService.replaceChecklist(
                        otherId,
                        checklist.checklistId(),
                        new ReplaceChecklistCommand("변조", List.of(102L))
                ),
                ErrorCode.CHECKLIST_NOT_FOUND
        );
        assertError(
                () -> checklistCommandService.deleteChecklist(otherId, checklist.checklistId()),
                ErrorCode.CHECKLIST_NOT_FOUND
        );
        assertThat(checklistQueryService.getChecklist(ownerId, checklist.checklistId()).name())
                .isEqualTo("소유 체크리스트");
    }

    @DisplayName("항목 교체 도중 DB 오류가 나면 루트 이름과 기존 항목 삭제를 함께 롤백한다")
    @Test
    void rollbackChecklistReplacement() {
        final long memberId = saveMember("replace-rollback-owner");
        final var checklist = create(memberId, "원래 이름", CheckStage.ON_SITE, 101, 102);
        jdbcTemplate.execute(
                """
                CREATE TRIGGER fail_checklist_item_insert
                BEFORE INSERT ON checklist_items
                FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'forced checklist item failure'
                """
        );

        try {
            assertError(
                    () -> checklistCommandService.replaceChecklist(
                            memberId,
                            checklist.checklistId(),
                            new ReplaceChecklistCommand("변경 이름", List.of(103L))
                    ),
                    ErrorCode.INTERNAL_SERVER_ERROR
            );
        } finally {
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS fail_checklist_item_insert");
        }

        final var unchanged = checklistQueryService.getChecklist(memberId, checklist.checklistId());
        assertThat(unchanged.name()).isEqualTo("원래 이름");
        assertThat(unchanged.items()).extracting(item -> item.sourceCheckItemId()).containsExactly(101L, 102L);
    }

    @DisplayName("항목 저장 도중 DB 오류가 나면 미완성 체크리스트 루트도 남기지 않는다")
    @Test
    void rollbackChecklistCreation() {
        final long memberId = saveMember("create-rollback-owner");
        jdbcTemplate.execute(
                """
                CREATE TRIGGER fail_checklist_item_insert
                BEFORE INSERT ON checklist_items
                FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'forced checklist item failure'
                """
        );

        try {
            assertError(
                    () -> create(memberId, "미완성", CheckStage.ON_SITE, 101),
                    ErrorCode.INTERNAL_SERVER_ERROR
            );
        } finally {
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS fail_checklist_item_insert");
        }

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM checklists", Long.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM checklist_items", Long.class)).isZero();
    }

    @DisplayName("동시 전체 변경은 루트 행을 직렬화해 마지막으로 커밋한 완전한 구성을 남긴다")
    @Test
    void serializeConcurrentReplacement() throws Exception {
        final long memberId = saveMember("concurrent-replace-owner");
        final var checklist = create(memberId, "원본", CheckStage.ON_SITE, 101);
        jdbcTemplate.execute(
                """
                CREATE TRIGGER delay_checklist_item_insert
                BEFORE INSERT ON checklist_items
                FOR EACH ROW
                BEGIN
                    DO GET_LOCK('checklist_replace_entered', 0);
                    DO GET_LOCK('checklist_replace_gate', 10);
                    DO RELEASE_LOCK('checklist_replace_gate');
                    DO RELEASE_LOCK('checklist_replace_entered');
                END
                """
        );

        try (Connection gateConnection = dataSource.getConnection();
             var executor = Executors.newFixedThreadPool(2)) {
            assertThat(namedLock(gateConnection, "SELECT GET_LOCK('checklist_replace_gate', 0)")).isEqualTo(1);
            final var first = executor.submit(() -> checklistCommandService.replaceChecklist(
                    memberId,
                    checklist.checklistId(),
                    new ReplaceChecklistCommand("첫 번째", List.of(103L, 101L))
            ));
            awaitNamedLock("checklist_replace_entered");
            final var second = executor.submit(() -> checklistCommandService.replaceChecklist(
                    memberId,
                    checklist.checklistId(),
                    new ReplaceChecklistCommand("두 번째", List.of(102L))
            ));
            assertThat(namedLock(gateConnection, "SELECT RELEASE_LOCK('checklist_replace_gate')")).isEqualTo(1);

            assertThat(first.get().name()).isEqualTo("첫 번째");
            assertThat(second.get().name()).isEqualTo("두 번째");
        } finally {
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS delay_checklist_item_insert");
        }

        final var lastCommitted = checklistQueryService.getChecklist(memberId, checklist.checklistId());
        assertThat(lastCommitted.name()).isEqualTo("두 번째");
        assertThat(lastCommitted.items()).extracting(item -> item.sourceCheckItemId()).containsExactly(102L);
    }

    private com.jachwisunbae.checklist.service.dto.result.ChecklistDetailResult create(
            final long memberId,
            final String name,
            final CheckStage stage,
            final long... checkItemIds
    ) {
        return checklistCommandService.createChecklist(
                memberId,
                new CreateChecklistCommand(name, stage, java.util.Arrays.stream(checkItemIds).boxed().toList())
        );
    }

    private com.jachwisunbae.checklist.service.dto.result.ChecklistDetailResult createCustom(
            final long memberId,
            final String name,
            final String question
    ) {
        return checklistCommandService.createChecklist(
                memberId,
                new CreateChecklistCommand(
                        name,
                        CheckStage.ON_SITE,
                        List.of(ChecklistItemCommand.custom(null, question)),
                        ChecklistRequestMode.V11
                )
        );
    }

    private long saveMember(final String subject) {
        jdbcTemplate.update(
                """
                INSERT INTO members (oauth_provider, oauth_subject, email, display_name, last_login_at)
                VALUES ('GOOGLE', ?, ?, '회원', CURRENT_TIMESTAMP(6))
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

    private int namedLock(final Connection connection, final String sql) throws Exception {
        try (var statement = connection.createStatement();
             var resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private void awaitNamedLock(final String lockName) throws InterruptedException {
        for (int attempt = 0; attempt < 100; attempt++) {
            final Long owner = jdbcTemplate.queryForObject("SELECT IS_USED_LOCK(?)", Long.class, lockName);
            if (owner != null) {
                return;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("동시 변경 테스트가 제한 시간 안에 행 잠금 구간에 진입하지 못했습니다.");
    }

    private void assertError(final Runnable action, final ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(JachwiException.class)
                .extracting(exception -> ((JachwiException) exception).getErrorCode())
                .isEqualTo(errorCode);
    }
}
