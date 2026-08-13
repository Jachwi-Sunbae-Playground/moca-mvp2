package com.jachwisunbae.property.repository;

import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.exception.server.DataInconsistencyException;
import com.jachwisunbae.property.domain.PreVisitMemoField;
import com.jachwisunbae.property.domain.PropertyMemo;
import com.jachwisunbae.property.domain.PropertyPreVisitMemo;
import java.sql.Timestamp;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PropertyPreVisitMemoRepository {

    private static final String FIND_OWNED_SQL = """
            SELECT memo.viewing_schedule,
                   memo.move_in_availability,
                   memo.provisional_deposit,
                   memo.room_options,
                   memo.maintenance_and_utilities,
                   memo.commute_time,
                   memo.government_support,
                   memo.additional_memo,
                   memo.saved_at
            FROM property_pre_visit_memos memo
            JOIN properties property ON property.id = memo.property_id
                                    AND property.member_id = memo.member_id
            WHERE memo.property_id = ?
              AND memo.member_id = ?
              AND property.member_id = ?
            """;
    private static final String UPSERT_OWNED_SQL = """
            INSERT INTO property_pre_visit_memos (
                property_id,
                member_id,
                viewing_schedule,
                move_in_availability,
                provisional_deposit,
                room_options,
                maintenance_and_utilities,
                commute_time,
                government_support,
                additional_memo,
                saved_at,
                created_at,
                updated_at
            )
            SELECT property.id,
                   property.member_id,
                   ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
            FROM properties property
            WHERE property.id = ?
              AND property.member_id = ?
            ON DUPLICATE KEY UPDATE
                member_id = VALUES(member_id),
                viewing_schedule = VALUES(viewing_schedule),
                move_in_availability = VALUES(move_in_availability),
                provisional_deposit = VALUES(provisional_deposit),
                room_options = VALUES(room_options),
                maintenance_and_utilities = VALUES(maintenance_and_utilities),
                commute_time = VALUES(commute_time),
                government_support = VALUES(government_support),
                additional_memo = VALUES(additional_memo),
                saved_at = VALUES(saved_at),
                updated_at = VALUES(updated_at)
            """;

    private final JdbcTemplate jdbcTemplate;

    public PropertyPreVisitMemoRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<PropertyPreVisitMemo> findOwned(final long memberId, final long propertyId) {
        try {
            return jdbcTemplate.query(
                    FIND_OWNED_SQL,
                    (resultSet, rowNumber) -> new PropertyPreVisitMemo(
                            new PreVisitMemoField(resultSet.getString("viewing_schedule")),
                            new PreVisitMemoField(resultSet.getString("move_in_availability")),
                            new PreVisitMemoField(resultSet.getString("provisional_deposit")),
                            new PreVisitMemoField(resultSet.getString("room_options")),
                            new PreVisitMemoField(resultSet.getString("maintenance_and_utilities")),
                            new PreVisitMemoField(resultSet.getString("commute_time")),
                            new PreVisitMemoField(resultSet.getString("government_support")),
                            new PropertyMemo(resultSet.getString("additional_memo")),
                            resultSet.getTimestamp("saved_at").toInstant()
                    ),
                    propertyId,
                    memberId,
                    memberId
            ).stream().findFirst();
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    public boolean upsertOwned(
            final long memberId,
            final long propertyId,
            final PropertyPreVisitMemo memo
    ) {
        try {
            final Timestamp savedAt = Timestamp.from(memo.savedAt());
            return jdbcTemplate.update(
                    UPSERT_OWNED_SQL,
                    memo.viewingSchedule().value(),
                    memo.moveInAvailability().value(),
                    memo.provisionalDeposit().value(),
                    memo.roomOptions().value(),
                    memo.maintenanceAndUtilities().value(),
                    memo.commuteTime().value(),
                    memo.governmentSupport().value(),
                    memo.additionalMemo().content(),
                    savedAt,
                    savedAt,
                    savedAt,
                    propertyId,
                    memberId
            ) > 0;
        } catch (DataAccessException exception) {
            throw dataInconsistency(exception);
        }
    }

    private DataInconsistencyException dataInconsistency(final DataAccessException cause) {
        return new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR, cause);
    }
}
