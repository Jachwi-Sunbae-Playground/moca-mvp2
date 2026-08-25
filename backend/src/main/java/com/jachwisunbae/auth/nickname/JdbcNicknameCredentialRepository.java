package com.jachwisunbae.auth.nickname;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcNicknameCredentialRepository implements NicknameCredentialRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcNicknameCredentialRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<NicknameCredential> findByNicknameKey(String nicknameKey) {
        return jdbcTemplate.query("""
                SELECT member_id, nickname, nickname_key, password_hash, created_at, updated_at
                FROM nickname_credentials
                WHERE nickname_key = ?
                """, rowMapper(), nicknameKey).stream().findFirst();
    }

    @Override
    public Optional<NicknameCredential> findByMemberId(Long memberId) {
        return jdbcTemplate.query("""
                SELECT member_id, nickname, nickname_key, password_hash, created_at, updated_at
                FROM nickname_credentials
                WHERE member_id = ?
                """, rowMapper(), memberId).stream().findFirst();
    }

    @Override
    public void save(NicknameCredential credential) {
        jdbcTemplate.update("""
                INSERT INTO nickname_credentials (
                    member_id, nickname, nickname_key, password_hash, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?)
                """, credential.memberId(), credential.nickname(), credential.nicknameKey(),
                credential.passwordHash(), credential.createdAt(), credential.updatedAt());
    }

    private RowMapper<NicknameCredential> rowMapper() {
        return (resultSet, rowNumber) -> new NicknameCredential(
                resultSet.getLong("member_id"),
                resultSet.getString("nickname"),
                resultSet.getString("nickname_key"),
                resultSet.getString("password_hash"),
                resultSet.getTimestamp("created_at").toLocalDateTime(),
                resultSet.getTimestamp("updated_at").toLocalDateTime());
    }
}
