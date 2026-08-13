package com.jachwisunbae.property.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.checklist.service.ChecklistReferenceQueryService;
import com.jachwisunbae.checklist.service.dto.result.ChecklistReferenceResult;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.exception.server.DataInconsistencyException;
import com.jachwisunbae.property.domain.ActiveChecklist;
import com.jachwisunbae.property.repository.ActiveChecklistRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

class ActiveChecklistServiceTest {

    @DisplayName("MySQL 잠금 실패는 새 트랜잭션에서 제한 재시도한다")
    @Test
    void retryLockFailure() {
        final Collaborators collaborators = collaborators();
        when(collaborators.activeChecklistRepository().upsertOwned(any(ActiveChecklist.class)))
                .thenThrow(new DataInconsistencyException(
                        ErrorCode.INTERNAL_SERVER_ERROR,
                        new CannotAcquireLockException("lock timeout")
                ))
                .thenReturn(true);

        final var result = collaborators.service().assign(1, 2, CheckStage.ON_SITE, 3);

        assertThat(result.checklistId()).isEqualTo(3L);
        verify(collaborators.activeChecklistRepository(), times(2)).upsertOwned(any(ActiveChecklist.class));
        verify(collaborators.propertyAccessService(), times(2)).lockOwned(1, 2);
        verify(collaborators.transactionManager()).rollback(any(TransactionStatus.class));
        verify(collaborators.transactionManager()).commit(any(TransactionStatus.class));
    }

    @DisplayName("잠금 외 DB 실패는 재시도하지 않는다")
    @Test
    void doNotRetryOtherDatabaseFailure() {
        final Collaborators collaborators = collaborators();
        when(collaborators.activeChecklistRepository().upsertOwned(any(ActiveChecklist.class)))
                .thenThrow(new DataInconsistencyException(
                        ErrorCode.INTERNAL_SERVER_ERROR,
                        new DataAccessResourceFailureException("database unavailable")
                ));

        assertThatThrownBy(() -> collaborators.service().assign(1, 2, CheckStage.ON_SITE, 3))
                .isInstanceOf(DataInconsistencyException.class);
        verify(collaborators.activeChecklistRepository()).upsertOwned(any(ActiveChecklist.class));
        verify(collaborators.propertyAccessService()).lockOwned(1, 2);
    }

    private Collaborators collaborators() {
        final PropertyAccessService propertyAccessService = mock(PropertyAccessService.class);
        final ChecklistReferenceQueryService checklistReferenceQueryService = mock(
                ChecklistReferenceQueryService.class
        );
        final ActiveChecklistRepository activeChecklistRepository = mock(ActiveChecklistRepository.class);
        final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        when(checklistReferenceQueryService.getOwnedForUpdate(1, 3))
                .thenReturn(new ChecklistReferenceResult(3, "현장", CheckStage.ON_SITE, 1));
        when(activeChecklistRepository.findOwned(1, 2, CheckStage.ON_SITE)).thenReturn(Optional.empty());
        final Clock clock = Clock.fixed(Instant.parse("2026-08-11T00:00:00Z"), ZoneOffset.UTC);
        final ActiveChecklistService service = new ActiveChecklistService(
                propertyAccessService,
                checklistReferenceQueryService,
                activeChecklistRepository,
                clock,
                transactionManager
        );
        return new Collaborators(
                service,
                propertyAccessService,
                activeChecklistRepository,
                transactionManager
        );
    }

    private record Collaborators(
            ActiveChecklistService service,
            PropertyAccessService propertyAccessService,
            ActiveChecklistRepository activeChecklistRepository,
            PlatformTransactionManager transactionManager
    ) {
    }
}
