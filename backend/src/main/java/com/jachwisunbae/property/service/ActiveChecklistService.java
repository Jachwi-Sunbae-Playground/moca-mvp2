package com.jachwisunbae.property.service;

import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.checklist.service.ChecklistReferenceQueryService;
import com.jachwisunbae.checklist.service.dto.result.ChecklistReferenceResult;
import com.jachwisunbae.common.exception.client.BusinessRuleViolationException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.exception.server.DataInconsistencyException;
import com.jachwisunbae.common.time.DatabaseTime;
import com.jachwisunbae.property.domain.ActiveChecklist;
import com.jachwisunbae.property.repository.ActiveChecklistRepository;
import com.jachwisunbae.property.service.dto.result.ActiveChecklistResult;
import java.time.Clock;
import java.time.Instant;
import java.util.function.Supplier;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ActiveChecklistService {

    private static final int MAX_LOCK_ATTEMPTS = 3;
    private static final long LOCK_RETRY_BACKOFF_MILLIS = 20;

    private final PropertyAccessService propertyAccessService;
    private final ChecklistReferenceQueryService checklistReferenceQueryService;
    private final ActiveChecklistRepository activeChecklistRepository;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    public ActiveChecklistService(
            final PropertyAccessService propertyAccessService,
            final ChecklistReferenceQueryService checklistReferenceQueryService,
            final ActiveChecklistRepository activeChecklistRepository,
            final Clock clock,
            final PlatformTransactionManager transactionManager
    ) {
        this.propertyAccessService = propertyAccessService;
        this.checklistReferenceQueryService = checklistReferenceQueryService;
        this.activeChecklistRepository = activeChecklistRepository;
        this.clock = clock;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setTimeout(30);
    }

    public ActiveChecklistResult assign(
            final long memberId,
            final long propertyId,
            final CheckStage stage,
            final long checklistId
    ) {
        return executeWithLockRetry(() -> transactionTemplate.execute(status ->
                assignInTransaction(memberId, propertyId, stage, checklistId)
        ));
    }

    public void unassign(final long memberId, final long propertyId, final CheckStage stage) {
        executeWithLockRetry(() -> transactionTemplate.execute(status -> {
            unassignInTransaction(memberId, propertyId, stage);
            return null;
        }));
    }

    private ActiveChecklistResult assignInTransaction(
            final long memberId,
            final long propertyId,
            final CheckStage stage,
            final long checklistId
    ) {
        propertyAccessService.lockOwned(memberId, propertyId);
        final ChecklistReferenceResult checklist = checklistReferenceQueryService.getOwnedForUpdate(
                memberId,
                checklistId
        );
        if (checklist.stage() != stage) {
            throw new BusinessRuleViolationException(ErrorCode.CHECKLIST_STAGE_MISMATCH);
        }
        final var current = activeChecklistRepository.findOwned(memberId, propertyId, stage);
        if (current.isPresent() && current.get().uses(checklistId)) {
            return ActiveChecklistResult.from(propertyId, checklist);
        }

        final Instant now = DatabaseTime.normalize(clock.instant());
        if (!activeChecklistRepository.upsertOwned(
                ActiveChecklist.create(propertyId, memberId, stage, checklistId, now)
        )) {
            throw new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        propertyAccessService.updateLastActivity(memberId, propertyId, now);
        return ActiveChecklistResult.from(propertyId, checklist);
    }

    private void unassignInTransaction(final long memberId, final long propertyId, final CheckStage stage) {
        propertyAccessService.lockOwned(memberId, propertyId);
        if (activeChecklistRepository.deleteOwned(memberId, propertyId, stage)) {
            propertyAccessService.updateLastActivity(
                    memberId,
                    propertyId,
                    DatabaseTime.normalize(clock.instant())
            );
        }
    }

    private <T> T executeWithLockRetry(final Supplier<T> action) {
        for (int attempt = 1; attempt <= MAX_LOCK_ATTEMPTS; attempt++) {
            try {
                return action.get();
            } catch (DataInconsistencyException exception) {
                if (!isLockFailure(exception) || attempt == MAX_LOCK_ATTEMPTS) {
                    throw exception;
                }
                backoff(exception, attempt);
            }
        }
        throw new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private boolean isLockFailure(final Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof PessimisticLockingFailureException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void backoff(final DataInconsistencyException failure, final int attempt) {
        try {
            Thread.sleep(LOCK_RETRY_BACKOFF_MILLIS * attempt);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw failure;
        }
    }
}
