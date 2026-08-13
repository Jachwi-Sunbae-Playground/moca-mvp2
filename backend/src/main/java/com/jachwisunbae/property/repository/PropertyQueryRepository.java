package com.jachwisunbae.property.repository;

import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.exception.server.DataInconsistencyException;
import com.jachwisunbae.common.page.PageQuery;
import com.jachwisunbae.property.domain.DiscoverySource;
import com.jachwisunbae.property.domain.DiscoverySourceType;
import com.jachwisunbae.property.domain.Money;
import com.jachwisunbae.property.domain.PreVisitMemoField;
import com.jachwisunbae.property.domain.PropertyMemo;
import com.jachwisunbae.property.domain.PropertyName;
import com.jachwisunbae.property.domain.PropertyPreVisitMemo;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class PropertyQueryRepository {

    private static final String SUMMARY_COLUMNS = """
            p.id,
            p.name,
            p.deposit_amount,
            p.monthly_rent_amount,
            p.discovery_source_type,
            p.discovery_source,
            (SELECT COUNT(*) FROM property_photos pp WHERE pp.property_id = p.id) AS photo_count,
            rv.id AS recent_visit_id,
            rv.status AS recent_visit_status,
            rv.started_at AS recent_visit_started_at,
            rv.completed_at AS recent_visit_completed_at,
            rvs.total_count AS recent_visit_total_count,
            rvs.good_count AS recent_visit_good_count,
            rvs.caution_count AS recent_visit_caution_count,
            rvs.unconfirmed_count AS recent_visit_unconfirmed_count,
            p.last_activity_at
            """;
    private static final String RECENT_VISIT_JOINS = """
            LEFT JOIN visits rv ON rv.id = (
                SELECT candidate.id
                FROM visits candidate
                WHERE candidate.property_id = p.id
                ORDER BY candidate.started_at DESC, candidate.id DESC
                LIMIT 1
            )
            LEFT JOIN (
                SELECT vss.visit_id,
                       COUNT(vci.id) AS total_count,
                       SUM(CASE WHEN vci.status = 'GOOD' THEN 1 ELSE 0 END) AS good_count,
                       SUM(CASE WHEN vci.status = 'CAUTION' THEN 1 ELSE 0 END) AS caution_count,
                       SUM(CASE WHEN vci.status = 'UNCONFIRMED' THEN 1 ELSE 0 END) AS unconfirmed_count
                FROM visit_stage_snapshots vss
                JOIN visit_check_items vci ON vci.visit_stage_snapshot_id = vss.id
                GROUP BY vss.visit_id
            ) rvs ON rvs.visit_id = rv.id
            """;
    private static final String FIND_ALL_SQL = """
            SELECT %s
            FROM properties p
            %s
            WHERE p.member_id = ?
            ORDER BY p.last_activity_at DESC, p.id DESC
            LIMIT ? OFFSET ?
            """.formatted(SUMMARY_COLUMNS, RECENT_VISIT_JOINS);
    private static final String FIND_BY_NAME_SQL = """
            SELECT %s
            FROM properties p
            %s
            WHERE p.member_id = ?
              AND p.name LIKE ? ESCAPE '!'
            ORDER BY p.last_activity_at DESC, p.id DESC
            LIMIT ? OFFSET ?
            """.formatted(SUMMARY_COLUMNS, RECENT_VISIT_JOINS);
    private static final String COUNT_ALL_SQL = """
            SELECT COUNT(*)
            FROM properties
            WHERE member_id = ?
            """;
    private static final String COUNT_BY_NAME_SQL = """
            SELECT COUNT(*)
            FROM properties
            WHERE member_id = ?
              AND name LIKE ? ESCAPE '!'
            """;
    private static final String FIND_DETAIL_SQL = """
            SELECT p.id,
                   p.name,
                   p.deposit_amount,
                   p.monthly_rent_amount,
                   p.discovery_source_type,
                   p.discovery_source,
                   COALESCE(pre_visit_memo.viewing_schedule, '') AS memo_viewing_schedule,
                   COALESCE(pre_visit_memo.move_in_availability, '') AS memo_move_in_availability,
                   COALESCE(pre_visit_memo.provisional_deposit, '') AS memo_provisional_deposit,
                   COALESCE(pre_visit_memo.room_options, '') AS memo_room_options,
                   COALESCE(pre_visit_memo.maintenance_and_utilities, '') AS memo_maintenance_and_utilities,
                   COALESCE(pre_visit_memo.commute_time, '') AS memo_commute_time,
                   COALESCE(pre_visit_memo.government_support, '') AS memo_government_support,
                   COALESCE(pre_visit_memo.additional_memo, p.memo) AS memo_additional_memo,
                   COALESCE(pre_visit_memo.saved_at, p.memo_updated_at, p.updated_at) AS memo_saved_at,
                   (SELECT COUNT(*) FROM property_photos pp WHERE pp.property_id = p.id) AS photo_count,
                   rv.id AS recent_visit_id,
                   rv.status AS recent_visit_status,
                   rv.started_at AS recent_visit_started_at,
                   rv.completed_at AS recent_visit_completed_at,
                   rvs.total_count AS recent_visit_total_count,
                   rvs.good_count AS recent_visit_good_count,
                   rvs.caution_count AS recent_visit_caution_count,
                   rvs.unconfirmed_count AS recent_visit_unconfirmed_count,
                   (SELECT COUNT(*) FROM visits visit_count WHERE visit_count.property_id = p.id) AS visit_count,
                   p.created_at,
                   p.updated_at,
                   p.last_activity_at
            FROM properties p
            %s
            LEFT JOIN property_pre_visit_memos pre_visit_memo
                   ON pre_visit_memo.property_id = p.id
                  AND pre_visit_memo.member_id = p.member_id
            WHERE p.id = ?
              AND p.member_id = ?
            """.formatted(RECENT_VISIT_JOINS);
    private static final RowMapper<PropertySummaryProjection> SUMMARY_ROW_MAPPER = (resultSet, rowNumber) ->
            new PropertySummaryProjection(
                    resultSet.getLong("id"),
                    new PropertyName(resultSet.getString("name")),
                    new Money(resultSet.getLong("deposit_amount")),
                    new Money(resultSet.getLong("monthly_rent_amount")),
                    new DiscoverySource(
                            DiscoverySourceType.valueOf(resultSet.getString("discovery_source_type")),
                            resultSet.getString("discovery_source")
                    ),
                    recentVisit(resultSet),
                    resultSet.getInt("photo_count"),
                    resultSet.getTimestamp("last_activity_at").toInstant()
            );
    private static final RowMapper<PropertyDetailProjection> DETAIL_ROW_MAPPER = (resultSet, rowNumber) -> {
        return new PropertyDetailProjection(
                resultSet.getLong("id"),
                new PropertyName(resultSet.getString("name")),
                new Money(resultSet.getLong("deposit_amount")),
                new Money(resultSet.getLong("monthly_rent_amount")),
                new DiscoverySource(
                        DiscoverySourceType.valueOf(resultSet.getString("discovery_source_type")),
                        resultSet.getString("discovery_source")
                ),
                new PropertyPreVisitMemo(
                        new PreVisitMemoField(resultSet.getString("memo_viewing_schedule")),
                        new PreVisitMemoField(resultSet.getString("memo_move_in_availability")),
                        new PreVisitMemoField(resultSet.getString("memo_provisional_deposit")),
                        new PreVisitMemoField(resultSet.getString("memo_room_options")),
                        new PreVisitMemoField(resultSet.getString("memo_maintenance_and_utilities")),
                        new PreVisitMemoField(resultSet.getString("memo_commute_time")),
                        new PreVisitMemoField(resultSet.getString("memo_government_support")),
                        new PropertyMemo(resultSet.getString("memo_additional_memo")),
                        resultSet.getTimestamp("memo_saved_at").toInstant()
                ),
                resultSet.getInt("photo_count"),
                recentVisit(resultSet),
                resultSet.getInt("visit_count"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant(),
                resultSet.getTimestamp("last_activity_at").toInstant()
        );
    };

    private static RecentVisitProjection recentVisit(final java.sql.ResultSet resultSet)
            throws java.sql.SQLException {
        final Long visitId = resultSet.getObject("recent_visit_id", Long.class);
        if (visitId == null) {
            return null;
        }
        final Timestamp completedAt = resultSet.getTimestamp("recent_visit_completed_at");
        final int totalCount = resultSet.getInt("recent_visit_total_count");
        final int unconfirmedCount = resultSet.getInt("recent_visit_unconfirmed_count");
        return new RecentVisitProjection(
                visitId,
                resultSet.getString("recent_visit_status"),
                resultSet.getTimestamp("recent_visit_started_at").toInstant(),
                completedAt == null ? null : completedAt.toInstant(),
                totalCount,
                totalCount - unconfirmedCount,
                resultSet.getInt("recent_visit_good_count"),
                resultSet.getInt("recent_visit_caution_count"),
                unconfirmedCount
        );
    }

    private final JdbcTemplate jdbcTemplate;

    public PropertyQueryRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PropertySummaryProjection> findAllOwned(
            final long memberId,
            final String query,
            final PageQuery pageQuery
    ) {
        try {
            if (query.isEmpty()) {
                return jdbcTemplate.query(
                        FIND_ALL_SQL,
                        SUMMARY_ROW_MAPPER,
                        memberId,
                        pageQuery.size(),
                        pageQuery.offset()
                );
            }
            return jdbcTemplate.query(
                    FIND_BY_NAME_SQL,
                    SUMMARY_ROW_MAPPER,
                    memberId,
                    likePattern(query),
                    pageQuery.size(),
                    pageQuery.offset()
            );
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public long countAllOwned(final long memberId, final String query) {
        try {
            final Long count = query.isEmpty()
                    ? jdbcTemplate.queryForObject(COUNT_ALL_SQL, Long.class, memberId)
                    : jdbcTemplate.queryForObject(COUNT_BY_NAME_SQL, Long.class, memberId, likePattern(query));
            return count == null ? 0 : count;
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public Optional<PropertyDetailProjection> findOwnedDetail(final long memberId, final long propertyId) {
        try {
            return jdbcTemplate.query(FIND_DETAIL_SQL, DETAIL_ROW_MAPPER, propertyId, memberId)
                    .stream()
                    .findFirst();
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    private String likePattern(final String query) {
        final String escaped = query
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }

    private DataInconsistencyException dataInconsistency(final DataAccessException cause) {
        return new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR, cause);
    }
}
