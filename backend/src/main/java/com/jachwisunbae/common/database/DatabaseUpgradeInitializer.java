package com.jachwisunbae.common.database;

import java.io.IOException;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DatabaseUpgradeInitializer implements ApplicationRunner {

    private static final String UPGRADE_SCRIPT_PATTERN = "classpath*:db/upgrade/*.sql";
    private static final int MAX_NICKNAME_LENGTH = 30;

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final PathMatchingResourcePatternResolver resourceResolver =
            new PathMatchingResourcePatternResolver();

    public DatabaseUpgradeInitializer(DataSource dataSource, JdbcTemplate jdbcTemplate, Clock clock) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        Resource[] resources = resourceResolver.getResources(UPGRADE_SCRIPT_PATTERN);
        Arrays.sort(resources, (left, right) -> left.getFilename().compareTo(right.getFilename()));
        if (resources.length == 0) {
            return;
        }
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(resources);
        populator.execute(dataSource);
        backfillLegacyMembers();
    }

    private void backfillLegacyMembers() {
        Set<String> occupiedKeys = new HashSet<>(jdbcTemplate.queryForList(
                "SELECT nickname_key FROM nickname_credentials", String.class));
        List<LegacyMember> members = jdbcTemplate.query("""
                SELECT member.id, member.name
                FROM members member
                LEFT JOIN nickname_credentials credential ON credential.member_id = member.id
                WHERE credential.member_id IS NULL
                ORDER BY member.id
                """, (resultSet, rowNumber) -> new LegacyMember(
                resultSet.getLong("id"), resultSet.getString("name")));
        LocalDateTime now = LocalDateTime.now(clock);
        for (LegacyMember member : members) {
            AllocatedNickname allocated = allocateNickname(member, occupiedKeys);
            jdbcTemplate.update("""
                    INSERT INTO nickname_credentials (
                        member_id, nickname, nickname_key, password_hash, created_at, updated_at
                    ) VALUES (?, ?, ?, NULL, ?, ?)
                    """, member.id(), allocated.displayName(), allocated.key(), now, now);
        }
    }

    private AllocatedNickname allocateNickname(LegacyMember member, Set<String> occupiedKeys) {
        String base = normalizeLegacyName(member.name());
        for (int attempt = 0; ; attempt++) {
            String suffix = attempt == 0 ? "" : createSuffix(member.id(), attempt);
            String displayName = truncate(base, MAX_NICKNAME_LENGTH - suffix.codePointCount(0, suffix.length()))
                    + suffix;
            String key = displayName.toLowerCase(Locale.ROOT);
            if (occupiedKeys.add(key)) {
                return new AllocatedNickname(displayName, key);
            }
        }
    }

    private String normalizeLegacyName(String rawName) {
        String normalized = Normalizer.normalize(rawName == null ? "" : rawName, Normalizer.Form.NFKC);
        StringBuilder sanitized = new StringBuilder();
        normalized.codePoints()
                .filter(codePoint -> !Character.isISOControl(codePoint))
                .forEach(sanitized::appendCodePoint);
        String trimmed = sanitized.toString().trim();
        return truncate(trimmed.isEmpty() ? "기존 사용자" : trimmed, MAX_NICKNAME_LENGTH);
    }

    private String createSuffix(long memberId, int attempt) {
        if (attempt == 1) {
            return " #" + memberId;
        }
        return " #" + memberId + "-" + attempt;
    }

    private String truncate(String value, int maxCodePoints) {
        if (value.codePointCount(0, value.length()) <= maxCodePoints) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(0, maxCodePoints));
    }

    private record LegacyMember(long id, String name) {
    }

    private record AllocatedNickname(String displayName, String key) {
    }
}
