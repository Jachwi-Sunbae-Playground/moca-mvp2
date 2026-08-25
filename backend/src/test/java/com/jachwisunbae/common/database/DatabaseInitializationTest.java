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
        assertThat(count("system_check_items")).isEqualTo(18);
        assertThat(count("system_memo_items")).isEqualTo(4);
        assertThat(jdbcTemplate.queryForList(
                "SELECT label FROM system_memo_items ORDER BY display_order",
                String.class
        )).containsExactly("입주 가능일", "방 옵션", "관리비 및 공과금", "방문 일정");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT question FROM system_check_items WHERE id = 1",
                String.class
        )).isEqualTo("매물의 정확한 주소와 동·층·호수를 확인했나요?");
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

    private record LegacyCredential(long memberId, String nickname, String nicknameKey, String passwordHash) {
    }
}
