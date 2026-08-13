package com.jachwisunbae.checklist.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.checklist.domain.Checklist;
import com.jachwisunbae.checklist.domain.ChecklistItem;
import com.jachwisunbae.checklist.domain.ChecklistItemOrigin;
import com.jachwisunbae.checklist.domain.ChecklistName;
import com.jachwisunbae.checklist.domain.ChecklistPresetType;
import com.jachwisunbae.common.RepositoryTest;
import com.jachwisunbae.common.page.PageQuery;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

class ChecklistRepositoryTest extends RepositoryTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private CheckItemRepository checkItemRepository;
    private ChecklistPresetRepository checklistPresetRepository;
    private ChecklistRepository checklistRepository;
    private ChecklistQueryRepository checklistQueryRepository;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM checklists");
        jdbcTemplate.update("DELETE FROM members");
        jdbcTemplate.update("DELETE FROM check_items WHERE id >= 9000");
        jdbcTemplate.update("UPDATE check_items SET is_active = TRUE");
        checkItemRepository = new CheckItemRepository(jdbcTemplate);
        checklistPresetRepository = new ChecklistPresetRepository(jdbcTemplate);
        checklistRepository = new ChecklistRepository(jdbcTemplate);
        checklistQueryRepository = new ChecklistQueryRepository(jdbcTemplate);
    }

    @DisplayName("기준 데이터는 72개 항목과 6개 프리셋을 보존하고 원룸 프리셋만 활성화한다")
    @Test
    void initializeCatalogAndPresets() {
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM check_items", Long.class)).isEqualTo(72L);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM checklist_presets", Long.class)).isEqualTo(6L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM checklist_presets WHERE is_active = TRUE",
                Long.class
        )).isEqualTo(3L);

        for (final CheckStage stage : CheckStage.values()) {
            final ChecklistPresetProjection preset = checklistPresetRepository.findActive(
                    ChecklistPresetType.ONE_ROOM,
                    stage
            ).orElseThrow();
            assertThat(preset.items()).isNotEmpty();
            assertThat(preset.items()).extracting(item -> item.order())
                    .containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(1, preset.items().size())
                            .boxed().toList());
            assertThat(preset.items()).extracting(item -> item.checkItemId()).doesNotHaveDuplicates();
            assertThat(checklistPresetRepository.findActive(ChecklistPresetType.GOSHIWON, stage)).isEmpty();
        }

        final Long mismatchCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM checklist_preset_items preset_item
                JOIN checklist_presets preset ON preset.id = preset_item.preset_id
                JOIN check_items item ON item.id = preset_item.check_item_id
                WHERE preset_item.stage <> preset.stage OR preset_item.stage <> item.stage
                """,
                Long.class
        );
        assertThat(mismatchCount).isZero();

        jdbcTemplate.update("UPDATE check_items SET is_active = FALSE WHERE id = 101");
        final ChecklistPresetProjection activeOnly = checklistPresetRepository.findActive(
                ChecklistPresetType.ONE_ROOM,
                CheckStage.ON_SITE
        ).orElseThrow();
        assertThat(activeOnly.items()).extracting(item -> item.checkItemId()).doesNotContain(101L);
        assertThat(activeOnly.items()).extracting(item -> item.order())
                .containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(1, activeOnly.items().size())
                        .boxed().toList());
    }

    @DisplayName("체크 항목 검색은 단계·활성 상태·페이지를 적용하고 SQL 와일드카드를 일반 문자로 취급한다")
    @Test
    void searchActiveItemsSafely() {
        jdbcTemplate.update(
                "INSERT INTO check_items (id, stage, question, is_active) VALUES (9001, 'ON_SITE', '경고! 할인 10%_확인', TRUE)"
        );
        jdbcTemplate.update(
                "INSERT INTO check_items (id, stage, question, is_active) VALUES (9002, 'ON_SITE', '할인 100 확인', TRUE)"
        );
        jdbcTemplate.update("UPDATE check_items SET is_active = FALSE WHERE id = 101");

        final var result = checkItemRepository.findAllActive(
                CheckStage.ON_SITE,
                "! 할인 10%_",
                PageQuery.of(0, 20)
        );

        assertThat(result).extracting(item -> item.id()).containsExactly(9001L);
        assertThat(checkItemRepository.findAllActive(CheckStage.ON_SITE, "보일러", PageQuery.of(0, 20)))
                .isEmpty();
        assertThat(checkItemRepository.countAllActive(CheckStage.ON_SITE, "! 할인 10%_")).isEqualTo(1L);
    }

    @DisplayName("내 체크리스트 목록과 상세는 회원·단계·수정 시각·항목 순서를 한정한다")
    @Test
    void queryOwnedChecklistsWithoutNPlusOne() {
        final long ownerId = saveMember("checklist-owner");
        final long otherId = saveMember("checklist-other");
        final Checklist old = saveChecklist(ownerId, "이전", "2026-08-10T01:00:00Z", 101, 103);
        final Checklist recent = saveChecklist(ownerId, "최근", "2026-08-10T02:00:00Z", 103, 101);
        saveChecklist(otherId, "타인", "2026-08-10T03:00:00Z", 101);

        final var summaries = checklistQueryRepository.findAllOwned(
                ownerId,
                CheckStage.ON_SITE,
                PageQuery.of(0, 20)
        );
        final var detail = checklistQueryRepository.findOwnedDetail(ownerId, recent.id()).orElseThrow();

        assertThat(summaries).extracting(summary -> summary.checklistId())
                .containsExactly(recent.id(), old.id());
        assertThat(summaries).extracting(summary -> summary.itemCount()).containsExactly(2, 2);
        assertThat(detail.items()).extracting(item -> item.sourceCheckItemId()).containsExactly(103L, 101L);
        assertThat(checklistQueryRepository.findOwnedDetail(otherId, recent.id())).isEmpty();
    }

    @DisplayName("DB는 항목 중복·순서 중복·단계 불일치와 회원 FK를 강제한다")
    @Test
    void enforceChecklistConstraints() {
        final long memberId = saveMember("constraint-owner");
        final Checklist checklist = saveChecklist(memberId, "제약", "2026-08-10T01:00:00Z", 101);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO checklist_items (checklist_id, check_item_id, stage, item_order) VALUES (?, 103, 'ON_SITE', 1)",
                checklist.id()
        )).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO checklist_items (checklist_id, check_item_id, stage, item_order) VALUES (?, 201, 'ONLINE_PHONE', 2)",
                checklist.id()
        )).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO checklists (member_id, name, stage, created_at, updated_at) VALUES (999999, '위조', 'ON_SITE', NOW(6), NOW(6))"
        )).isInstanceOf(DataAccessException.class);
    }

    @DisplayName("DB와 상세 조회는 PROVIDED·CUSTOM 배타성·로컬 ID·nullable 출처를 강제한다")
    @Test
    void storeProvidedAndCustomItemsExclusively() {
        final long memberId = saveMember("custom-repository-owner");
        final Instant now = Instant.parse("2026-08-10T01:00:00Z");
        final Checklist saved = checklistRepository.save(new Checklist(
                0,
                memberId,
                new ChecklistName("혼합 저장"),
                CheckStage.ON_SITE,
                List.of(
                        ChecklistItem.provided(0, 101, CheckStage.ON_SITE, 1),
                        ChecklistItem.custom(0, "창틀 곰팡이는 괜찮은가?", CheckStage.ON_SITE, 2)
                ),
                now,
                now
        ));

        final var detail = checklistQueryRepository.findOwnedDetail(memberId, saved.id()).orElseThrow();
        assertThat(detail.items()).extracting(item -> item.origin())
                .containsExactly(ChecklistItemOrigin.PROVIDED, ChecklistItemOrigin.CUSTOM);
        assertThat(detail.items()).extracting(item -> item.sourceCheckItemId())
                .containsExactly(101L, null);
        assertThat(detail.items()).extracting(item -> item.checklistItemId()).doesNotHaveDuplicates();
        assertThat(detail.items().get(1).question()).isEqualTo("창틀 곰팡이는 괜찮은가?");

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO checklist_items (
                    checklist_id, origin, check_item_id, custom_question, stage, item_order
                ) VALUES (?, 'CUSTOM', 103, '출처가 뒤섞였는가?', 'ON_SITE', 3)
                """,
                saved.id()
        )).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO checklist_items (
                    checklist_id, origin, check_item_id, custom_question, stage, item_order
                ) VALUES (?, 'CUSTOM', NULL, '   ', 'ON_SITE', 3)
                """,
                saved.id()
        )).isInstanceOf(DataAccessException.class);
    }

    @DisplayName("checklist_items는 로컬 CUSTOM 구현에 필요한 PK·FK·UNIQUE·CHECK를 실제 MySQL에 가진다")
    @Test
    void createChecklistItemSchemaConstraints() {
        final List<String> constraints = jdbcTemplate.queryForList(
                """
                SELECT CONSTRAINT_NAME
                FROM information_schema.TABLE_CONSTRAINTS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'checklist_items'
                """,
                String.class
        );

        assertThat(constraints).contains(
                "PRIMARY",
                "uk_checklist_items_id_checklist",
                "uk_checklist_items_order",
                "uk_checklist_items_provided",
                "fk_checklist_items_checklist_stage",
                "fk_checklist_items_check_item_stage",
                "ck_checklist_items_origin",
                "ck_checklist_items_source"
        );
    }

    @DisplayName("체크리스트 목록 쿼리는 회원·단계·수정 시각 인덱스를 사용한다")
    @Test
    void useChecklistListIndex() {
        final long memberId = saveMember("checklist-explain-owner");
        for (int index = 0; index < 60; index++) {
            saveChecklist(
                    memberId,
                    "대표 체크리스트 " + index,
                    Instant.parse("2026-08-10T00:00:00Z").plusSeconds(index).toString(),
                    101
            );
        }

        final List<Map<String, Object>> explain = jdbcTemplate.queryForList(
                """
                EXPLAIN SELECT checklist.id,
                               checklist.name,
                               checklist.stage,
                               (SELECT COUNT(*) FROM checklist_items item WHERE item.checklist_id = checklist.id),
                               (SELECT COUNT(*)
                                FROM property_active_checklists active
                                WHERE active.checklist_id = checklist.id),
                               checklist.updated_at
                FROM checklists checklist
                WHERE checklist.member_id = ? AND checklist.stage = 'ON_SITE'
                ORDER BY checklist.updated_at DESC, checklist.id DESC
                LIMIT 20 OFFSET 0
                """,
                memberId
        );

        assertThat(explain).anySatisfy(row -> {
            assertThat(row.get("table")).isEqualTo("checklist");
            assertThat(selectedOrPossibleKey(row)).contains("idx_checklists_member_stage_updated");
        });
    }

    private String selectedOrPossibleKey(final Map<String, Object> row) {
        return String.valueOf(row.get("key")) + "," + row.get("possible_keys");
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

    private Checklist saveChecklist(
            final long memberId,
            final String name,
            final String now,
            final long... checkItemIds
    ) {
        final Instant timestamp = Instant.parse(now);
        final List<ChecklistItem> items = java.util.stream.IntStream.range(0, checkItemIds.length)
                .mapToObj(index -> new ChecklistItem(checkItemIds[index], CheckStage.ON_SITE, index + 1))
                .toList();
        return checklistRepository.save(new Checklist(
                0,
                memberId,
                new ChecklistName(name),
                CheckStage.ON_SITE,
                items,
                timestamp,
                timestamp
        ));
    }
}
