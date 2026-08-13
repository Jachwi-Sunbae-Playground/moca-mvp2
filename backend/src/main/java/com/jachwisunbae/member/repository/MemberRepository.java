package com.jachwisunbae.member.repository;

import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.exception.server.DataInconsistencyException;
import com.jachwisunbae.member.domain.Member;
import com.jachwisunbae.member.domain.OAuthProvider;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MemberRepository {

    private static final String UPSERT_MEMBER_SQL = """
            INSERT INTO members (
                oauth_provider,
                oauth_subject,
                email,
                display_name,
                last_login_at,
                created_at,
                updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                email = VALUES(email),
                display_name = VALUES(display_name),
                last_login_at = VALUES(last_login_at),
                updated_at = VALUES(updated_at)
            """;
    private static final String FIND_BY_ID_SQL = """
            SELECT id,
                   oauth_provider,
                   oauth_subject,
                   email,
                   display_name,
                   last_login_at,
                   created_at,
                   updated_at
            FROM members
            WHERE id = ?
            """;
    private static final String FIND_BY_PROVIDER_AND_SUBJECT_SQL = """
            SELECT id,
                   oauth_provider,
                   oauth_subject,
                   email,
                   display_name,
                   last_login_at,
                   created_at,
                   updated_at
            FROM members
            WHERE oauth_provider = ?
              AND oauth_subject = ?
            """;
    private static final RowMapper<Member> MEMBER_ROW_MAPPER = (resultSet, rowNumber) -> new Member(
            resultSet.getLong("id"),
            OAuthProvider.valueOf(resultSet.getString("oauth_provider")),
            resultSet.getString("oauth_subject"),
            resultSet.getString("email"),
            resultSet.getString("display_name"),
            resultSet.getTimestamp("last_login_at").toInstant(),
            resultSet.getTimestamp("created_at").toInstant(),
            resultSet.getTimestamp("updated_at").toInstant()
    );

    private final JdbcTemplate jdbcTemplate;

    public MemberRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Member upsert(
            final OAuthProvider provider,
            final String subject,
            final String email,
            final String displayName,
        final Instant loginAt
    ) {
        try {
            jdbcTemplate.update(
                    UPSERT_MEMBER_SQL,
                    provider.name(),
                    subject,
                    email,
                    displayName,
                    Timestamp.from(loginAt),
                    Timestamp.from(loginAt),
                    Timestamp.from(loginAt)
            );
            return findByProviderAndSubject(provider, subject).orElseThrow(this::dataInconsistency);
        } catch (DataAccessException exception) {
            throw new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR, exception);
        }
    }

    public Optional<Member> findById(final long memberId) {
        try {
            return jdbcTemplate.query(FIND_BY_ID_SQL, MEMBER_ROW_MAPPER, memberId).stream().findFirst();
        } catch (DataAccessException exception) {
            throw new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR, exception);
        }
    }

    public Optional<Member> findByProviderAndSubject(
            final OAuthProvider provider,
            final String subject
    ) {
        try {
            return jdbcTemplate.query(
                    FIND_BY_PROVIDER_AND_SUBJECT_SQL,
                    MEMBER_ROW_MAPPER,
                    provider.name(),
                    subject
            ).stream().findFirst();
        } catch (DataAccessException exception) {
            throw new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR, exception);
        }
    }

    private DataInconsistencyException dataInconsistency() {
        return new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
