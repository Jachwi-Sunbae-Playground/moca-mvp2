package com.jachwisunbae.common.database;

import static org.assertj.core.api.Assertions.assertThat;

import com.jachwisunbae.common.IntegrationTest;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

class DatabaseInitializationTest extends IntegrationTest {

    private static final List<String> APPLICATION_TABLES = List.of(
            "members",
            "nickname_credentials",
            "properties",
            "property_photos",
            "main_property_photos",
            "system_memo_items",
            "property_memos",
            "property_memo_items",
            "system_check_items",
            "user_checklists",
            "user_checklist_items",
            "property_checklists",
            "property_checklist_items"
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DatabaseUpgradeInitializer databaseUpgradeInitializer;

    @Test
    void 현재_스키마와_기본_데이터로_새_DB를_초기화한다() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()",
                String.class
        );

        assertThat(tables).containsExactlyInAnyOrderElementsOf(APPLICATION_TABLES);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM system_check_items WHERE deleted_at IS NULL", Long.class)).isEqualTo(53);
        assertThat(jdbcTemplate.queryForList("""
                SELECT stage, item_type, COUNT(*) AS item_count
                FROM system_check_items
                WHERE deleted_at IS NULL
                GROUP BY stage, item_type
                ORDER BY FIELD(stage, 'ONLINE_PHONE', 'ON_SITE', 'PRE_CONTRACT'), item_type
                """)).containsExactly(
                java.util.Map.of("stage", "ONLINE_PHONE", "item_type", "CORE", "item_count", 6L),
                java.util.Map.of("stage", "ONLINE_PHONE", "item_type", "OPTIONAL", "item_count", 6L),
                java.util.Map.of("stage", "ON_SITE", "item_type", "CORE", "item_count", 8L),
                java.util.Map.of("stage", "ON_SITE", "item_type", "OPTIONAL", "item_count", 19L),
                java.util.Map.of("stage", "PRE_CONTRACT", "item_type", "CORE", "item_count", 7L),
                java.util.Map.of("stage", "PRE_CONTRACT", "item_type", "OPTIONAL", "item_count", 7L));
        assertThat(count("system_memo_items")).isEqualTo(4);
        assertThat(jdbcTemplate.queryForList(
                "SELECT label FROM system_memo_items ORDER BY display_order",
                String.class
        )).containsExactly("입주 가능일", "방 옵션", "관리비 및 공과금", "방문 일정");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT question FROM system_check_items WHERE id = 101",
                String.class
        )).isEqualTo("보증금과 월세, 관리비는 어떤가요?");
        assertThat(nullableColumn("user_checklist_items", "system_check_item_id")).isEqualTo("YES");
        assertThat(nullableColumn("property_checklist_items", "system_check_item_id")).isEqualTo("YES");
    }

    @Test
    void 기존_제공_문항은_삭제하지_않고_반복_업그레이드에서_비활성화한다() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO system_check_items (id, stage, item_type, question, deleted_at)
                VALUES (1, 'ONLINE_PHONE', 'CORE', '기존 질문 스냅샷 원본', NULL)
                ON DUPLICATE KEY UPDATE deleted_at = NULL
                """);

        databaseUpgradeInitializer.run(new DefaultApplicationArguments());
        LocalDateTime retiredAt = jdbcTemplate.queryForObject(
                "SELECT deleted_at FROM system_check_items WHERE id = 1", LocalDateTime.class);
        databaseUpgradeInitializer.run(new DefaultApplicationArguments());

        assertThat(retiredAt).isNotNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT question FROM system_check_items WHERE id = 1", String.class))
                .isEqualTo("기존 질문 스냅샷 원본");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT deleted_at FROM system_check_items WHERE id = 1", LocalDateTime.class))
                .isEqualTo(retiredAt);
    }

    @Test
    void 기존_회원은_NFKC_중복을_분리한_비밀번호_없는_닉네임을_이어받는다() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("DELETE FROM members WHERE email IN (?, ?)",
                "legacy-fullwidth@example.com", "legacy-ascii@example.com");
        jdbcTemplate.update("INSERT INTO members (email, name, last_login_at, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?)",
                "legacy-fullwidth@example.com", "Ａ", now, now, now);
        jdbcTemplate.update("INSERT INTO members (email, name, last_login_at, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?)",
                "legacy-ascii@example.com", "a", now, now, now);

        databaseUpgradeInitializer.run(new DefaultApplicationArguments());
        databaseUpgradeInitializer.run(new DefaultApplicationArguments());

        List<LegacyCredential> credentials = jdbcTemplate.query("""
                SELECT credential.member_id, credential.nickname, credential.nickname_key, credential.password_hash
                FROM nickname_credentials credential
                JOIN members member ON member.id = credential.member_id
                WHERE member.email IN (?, ?)
                ORDER BY credential.member_id
                """, (resultSet, rowNumber) -> new LegacyCredential(
                        resultSet.getLong("member_id"),
                        resultSet.getString("nickname"),
                        resultSet.getString("nickname_key"),
                        resultSet.getString("password_hash")),
                "legacy-fullwidth@example.com", "legacy-ascii@example.com");

        assertThat(credentials).hasSize(2);
        assertThat(credentials.get(0).nickname()).isEqualTo("A");
        assertThat(credentials.get(0).nicknameKey()).isEqualTo("a");
        assertThat(credentials.get(1).nickname()).isEqualTo("a #" + credentials.get(1).memberId());
        assertThat(credentials).allMatch(credential -> credential.passwordHash() == null);
    }

    private Long count(String tableName) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
    }

    private String nullableColumn(String tableName, String columnName) {
        return jdbcTemplate.queryForObject("""
                SELECT is_nullable
                FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
                """, String.class, tableName, columnName);
    }

    private record LegacyCredential(long memberId, String nickname, String nicknameKey, String passwordHash) {
    }
}
