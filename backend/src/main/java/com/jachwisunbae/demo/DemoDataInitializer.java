package com.jachwisunbae.demo;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "demo.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final String demoName;

    public DemoDataInitializer(JdbcTemplate jdbcTemplate, Clock clock,
                               @Value("${demo.seed.nickname:이자취}") String demoName) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
        this.demoName = demoName;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        LocalDateTime now = LocalDateTime.now(clock);
        jdbcTemplate.update("INSERT INTO members (email, name, last_login_at, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE name = VALUES(name)",
                "demo@moca.local", demoName, now, now, now);
        Long memberId = jdbcTemplate.queryForObject(
                "SELECT id FROM members WHERE email = ?", Long.class, "demo@moca.local");
        jdbcTemplate.update("INSERT INTO nickname_credentials "
                        + "(member_id, nickname, nickname_key, password_hash, created_at, updated_at) "
                        + "VALUES (?, ?, ?, NULL, ?, ?) ON DUPLICATE KEY UPDATE nickname = VALUES(nickname)",
                memberId, demoName, demoName.trim().toLowerCase(Locale.ROOT), now, now);
        if (memberId == null || count("properties", "member_id", memberId) > 0) {
            return;
        }
        long firstPropertyId = insertProperty(memberId, "신림역 원룸", 10_000_000L, 550_000L,
                "zigbang.com/oneroom/12345", "서울 관악구 신림로 12길 3", "서울 관악구 신림동 1433-12",
                "37.4841234", "126.9291234", now.minusHours(1));
        insertProperty(memberId, "망원동 투룸", 30_000_000L, 750_000L,
                "드림공인중개사 010-9977-9012", "서울 마포구 망원로 14길 22", "서울 마포구 망원동 57-18",
                "37.5562140", "126.9017210", now.minusHours(3));
        initializeMemo(firstPropertyId);
        initializeChecklists(memberId, firstPropertyId);
    }

    private long insertProperty(long memberId, String name, long deposit, long rent, String source,
                                String roadAddress, String jibunAddress, String latitude, String longitude,
                                LocalDateTime activityAt) {
        jdbcTemplate.update("INSERT INTO properties (member_id, name, deposit_amount, monthly_rent_amount, "
                        + "discovery_source, road_address, jibun_address, latitude, longitude, created_at, updated_at, last_activity_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                memberId, name, deposit, rent, source, roadAddress, jibunAddress, latitude, longitude,
                activityAt, activityAt, activityAt);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void initializeMemo(long propertyId) {
        jdbcTemplate.update("INSERT INTO property_memos (property_id, free_memo) VALUES (?, ?)",
                propertyId, "채광이 좋고 역에서 가깝다. 계약 전에 등기부등본을 다시 확인하기.");
        Long memoId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        List<String> contents = List.of("3월 25일 이후", "에어컨 · 냉장고 · 세탁기 · 인덕션",
                "월 7만원 (수도 포함)", "8월 26일 오후 3시");
        for (int index = 0; index < contents.size(); index++) {
            int itemId = index + 1;
            jdbcTemplate.update("INSERT INTO property_memo_items "
                            + "(property_memo_id, system_memo_item_id, label, display_order, content) "
                            + "SELECT ?, id, label, display_order, ? FROM system_memo_items WHERE id = ?",
                    memoId, contents.get(index), itemId);
        }
    }

    private void initializeChecklists(long memberId, long propertyId) {
        for (String stage : List.of("ONLINE_PHONE", "ON_SITE", "PRE_CONTRACT")) {
            String name = switch (stage) {
                case "ONLINE_PHONE" -> "나의 원룸 체크리스트 ver1";
                case "ON_SITE" -> "나의 원룸 체크리스트 ver2";
                default -> "계약 전 필수 체크리스트";
            };
            jdbcTemplate.update("INSERT INTO user_checklists (member_id, name, stage) VALUES (?, ?, ?)",
                    memberId, name, stage);
            Long checklistId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            List<Long> itemIds = jdbcTemplate.queryForList(
                    "SELECT id FROM system_check_items WHERE stage = ? AND deleted_at IS NULL ORDER BY id",
                    Long.class, stage);
            for (int index = 0; index < itemIds.size(); index++) {
                jdbcTemplate.update("INSERT INTO user_checklist_items "
                                + "(user_checklist_id, system_check_item_id, stage, item_type, question, display_order) "
                                + "SELECT ?, id, stage, item_type, question, ? FROM system_check_items WHERE id = ?",
                        checklistId, index + 1, itemIds.get(index));
            }
            jdbcTemplate.update("INSERT INTO property_checklists "
                            + "(property_id, user_checklist_id, checklist_name, stage) VALUES (?, ?, ?, ?)",
                    propertyId, checklistId, name, stage);
            Long propertyChecklistId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            for (int index = 0; index < itemIds.size(); index++) {
                String status = index == 0 ? "GOOD" : index == 1 ? "CAUTION" : "UNCONFIRMED";
                String memo = index == 1 && "ON_SITE".equals(stage) ? "아침에는 수압이 조금 약하다고 함" : "";
                jdbcTemplate.update("INSERT INTO property_checklist_items "
                                + "(property_checklist_id, system_check_item_id, display_order, status, memo, question) "
                                + "SELECT ?, id, ?, ?, ?, question FROM system_check_items WHERE id = ?",
                        propertyChecklistId, index + 1, status, memo, itemIds.get(index));
            }
        }
    }

    private int count(String table, String column, long value) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?", Integer.class, value);
        return count == null ? 0 : count;
    }
}
