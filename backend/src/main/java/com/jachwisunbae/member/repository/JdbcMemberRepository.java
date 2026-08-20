package com.jachwisunbae.member.repository;

import com.jachwisunbae.member.entity.Member;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Optional;

@Repository
public class JdbcMemberRepository implements MemberRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcMemberRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Member> findById(final Long memberId) {
        String sql = """
                SELECT id, email, name, created_at, updated_at
                FROM members
                WHERE id = ?
                """;

        return jdbcTemplate.query(sql, memberRowMapper(), memberId)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<Member> findByIdForUpdate(final Long memberId) {
        String sql = """
                SELECT id, email, name, created_at, updated_at
                FROM members
                WHERE id = ?
                FOR UPDATE
                """;
        return jdbcTemplate.query(sql, memberRowMapper(), memberId).stream().findFirst();
    }

    @Override
    public Optional<Member> findByEmail(final String email) {
        String sql = """
                SELECT id, email, name, created_at, updated_at
                FROM members
                WHERE email = ?
                """;

        return jdbcTemplate.query(sql, memberRowMapper(), email)
                .stream()
                .findFirst();
    }

    @Override
    public Member save(final Member member) {
        String sql = """
                INSERT INTO members (
                    email, name,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, member.getEmail());
            statement.setString(2, member.getName());
            statement.setObject(3, member.getCreatedAt());
            statement.setObject(4, member.getUpdatedAt());
            return statement;
        }, keyHolder);
        return Member.reconstruct(keyHolder.getKey().longValue(), member.getEmail(), member.getName(),
                member.getCreatedAt(), member.getUpdatedAt());
    }

    @Override
    public void update(final Member member) {
        String sql = """
                UPDATE members
                SET email = ?, name = ?, updated_at = ?
                WHERE id = ?
                """;
        jdbcTemplate.update(sql, member.getEmail(), member.getName(), member.getUpdatedAt(), member.getId());
    }

    private RowMapper<Member> memberRowMapper() {
        return (resultSet, rowNumber) -> Member.reconstruct(
                resultSet.getLong("id"),
                resultSet.getString("email"),
                resultSet.getString("name"),
                resultSet.getTimestamp("created_at").toLocalDateTime(),
                resultSet.getTimestamp("updated_at").toLocalDateTime()
        );
    }
}
