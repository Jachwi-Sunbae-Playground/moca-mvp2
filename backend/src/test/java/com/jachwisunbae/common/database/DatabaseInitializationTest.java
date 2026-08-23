package com.jachwisunbae.common.database;

import static org.assertj.core.api.Assertions.assertThat;

import com.jachwisunbae.common.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class DatabaseInitializationTest extends IntegrationTest {

    private static final List<String> APPLICATION_TABLES = List.of(
            "members",
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

    private Long count(String tableName) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
    }
}
