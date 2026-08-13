package com.jachwisunbae.visit.service;

import com.jachwisunbae.checklist.service.ChecklistSnapshotSourceService;
import com.jachwisunbae.checklist.service.dto.result.ChecklistSnapshotSourceResult;
import com.jachwisunbae.common.exception.client.BusinessRuleViolationException;
import com.jachwisunbae.common.exception.client.InvalidCommandException;
import com.jachwisunbae.common.exception.client.ResourceNotFoundException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.exception.server.DataInconsistencyException;
import com.jachwisunbae.common.time.DatabaseTime;
import com.jachwisunbae.property.service.PropertyAccessService;
import com.jachwisunbae.visit.domain.CheckStatus;
import com.jachwisunbae.visit.domain.InlineMemo;
import com.jachwisunbae.visit.domain.Visit;
import com.jachwisunbae.visit.domain.VisitCheckItem;
import com.jachwisunbae.visit.domain.VisitStageSnapshot;
import com.jachwisunbae.visit.domain.VisitStatus;
import com.jachwisunbae.visit.repository.VisitCheckItemRepository;
import com.jachwisunbae.visit.repository.VisitQueryRepository;
import com.jachwisunbae.visit.repository.VisitRepository;
import com.jachwisunbae.visit.repository.VisitSnapshotRepository;
import com.jachwisunbae.visit.service.dto.command.CompleteVisitCommand;
import com.jachwisunbae.visit.service.dto.command.UpdateVisitItemMemoCommand;
import com.jachwisunbae.visit.service.dto.command.UpdateVisitItemStatusCommand;
import com.jachwisunbae.visit.service.dto.result.VisitCompleteResult;
import com.jachwisunbae.visit.service.dto.result.VisitDetailResult;
import com.jachwisunbae.visit.service.dto.result.VisitItemMemoResult;
import com.jachwisunbae.visit.service.dto.result.VisitItemStatusResult;
import com.jachwisunbae.visit.service.dto.result.VisitSummaryResult;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class VisitCommandService {

    private static final Logger log = LoggerFactory.getLogger(VisitCommandService.class);
    private static final int MAX_LOCK_ATTEMPTS = 3;
    private static final long LOCK_RETRY_BACKOFF_MILLIS = 20;

    private final PropertyAccessService propertyAccessService;
    private final ChecklistSnapshotSourceService checklistSnapshotSourceService;
    private final VisitRepository visitRepository;
    private final VisitSnapshotRepository visitSnapshotRepository;
    private final VisitCheckItemRepository visitCheckItemRepository;
    private final VisitQueryRepository visitQueryRepository;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    public VisitCommandService(
            final PropertyAccessService propertyAccessService,
            final ChecklistSnapshotSourceService checklistSnapshotSourceService,
            final VisitRepository visitRepository,
            final VisitSnapshotRepository visitSnapshotRepository,
            final VisitCheckItemRepository visitCheckItemRepository,
            final VisitQueryRepository visitQueryRepository,
            final Clock clock,
            final PlatformTransactionManager transactionManager
    ) {
        this.propertyAccessService = propertyAccessService;
        this.checklistSnapshotSourceService = checklistSnapshotSourceService;
        this.visitRepository = visitRepository;
        this.visitSnapshotRepository = visitSnapshotRepository;
        this.visitCheckItemRepository = visitCheckItemRepository;
        this.visitQueryRepository = visitQueryRepository;
        this.clock = clock;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setTimeout(30);
    }

    public VisitDetailResult startVisit(final long memberId, final long propertyId) {
        final VisitDetailResult result = executeWithLockRetry(() -> transactionTemplate.execute(status ->
                startVisitInTransaction(memberId, propertyId)
        ));
        log.info("event=visit_started memberId={} propertyId={} visitId={}",
                memberId, propertyId, result.visitId());
        return result;
    }

    public VisitItemStatusResult updateItemStatus(
            final long memberId,
            final long visitId,
            final long visitItemId,
            final UpdateVisitItemStatusCommand command
    ) {
        if (command.expectedStatusVersion() < 0) {
            throw new InvalidCommandException(ErrorCode.INVALID_REQUEST);
        }
        final CheckStatus checkStatus = CheckStatus.from(command.status());
        final VisitItemStatusResult result = executeWithLockRetry(() -> transactionTemplate.execute(status ->
                updateItemStatusInTransaction(
                        memberId,
                        visitId,
                        visitItemId,
                        checkStatus,
                        command.expectedStatusVersion()
                )
        ));
        log.info(
                "event=visit_item_status_changed memberId={} visitId={} visitItemId={} status={} statusVersion={}",
                memberId,
                visitId,
                visitItemId,
                result.item().status(),
                result.item().statusVersion()
        );
        return result;
    }

    public VisitItemMemoResult updateItemMemo(
            final long memberId,
            final long visitId,
            final long visitItemId,
            final UpdateVisitItemMemoCommand command
    ) {
        if (command.expectedMemoVersion() < 0) {
            throw new InvalidCommandException(ErrorCode.INVALID_REQUEST);
        }
        final VisitItemMemoResult result = executeWithLockRetry(() -> transactionTemplate.execute(status ->
                updateItemMemoInTransaction(
                        memberId,
                        visitId,
                        visitItemId,
                        command.memo(),
                        command.expectedMemoVersion()
                )
        ));
        log.info(
                "event=visit_item_memo_changed memberId={} visitId={} visitItemId={} memoVersion={}",
                memberId,
                visitId,
                visitItemId,
                result.memoVersion()
        );
        return result;
    }

    public VisitCompleteResult completeVisit(
            final long memberId,
            final long visitId,
            final CompleteVisitCommand command
    ) {
        VisitStatus.completionFrom(command.status());
        final CompletionOutcome outcome = executeWithLockRetry(() -> transactionTemplate.execute(status ->
                completeVisitInTransaction(memberId, visitId)
        ));
        if (outcome.firstCompletion()) {
            observeCompletion(memberId, outcome.result());
        }
        return outcome.result();
    }

    private VisitDetailResult startVisitInTransaction(final long memberId, final long propertyId) {
        propertyAccessService.lockOwned(memberId, propertyId);
        final List<ChecklistSnapshotSourceResult> sources =
                checklistSnapshotSourceService.loadForVisit(memberId, propertyId);
        if (sources.isEmpty()) {
            throw new BusinessRuleViolationException(ErrorCode.ACTIVE_CHECKLIST_REQUIRED);
        }
        if (sources.stream().anyMatch(source -> source.items().isEmpty())) {
            throw new DataInconsistencyException(ErrorCode.CHECKLIST_SNAPSHOT_FAILED);
        }
        final Instant now = DatabaseTime.normalize(clock.instant());
        final Visit visit = visitRepository.save(Visit.start(propertyId, memberId, now));
        visitSnapshotRepository.saveAll(toSnapshots(visit.id(), sources, now));
        propertyAccessService.updateLastActivity(memberId, propertyId, now);
        final var rows = visitQueryRepository.findOwnedDetail(memberId, visit.id());
        if (rows.isEmpty()) {
            throw new DataInconsistencyException(ErrorCode.CHECKLIST_SNAPSHOT_FAILED);
        }
        return VisitDetailResult.from(rows);
    }

    private VisitItemStatusResult updateItemStatusInTransaction(
            final long memberId,
            final long visitId,
            final long visitItemId,
            final CheckStatus status,
            final long expectedStatusVersion
    ) {
        final long propertyId = findOwnedPropertyId(memberId, visitId);
        lockVisitProperty(memberId, propertyId);
        findOwnedVisitForUpdate(memberId, visitId);
        final Instant now = DatabaseTime.normalize(clock.instant());
        if (!visitCheckItemRepository.updateStatus(
                memberId,
                visitId,
                visitItemId,
                status,
                expectedStatusVersion,
                now
        )) {
            visitCheckItemRepository.findOwnedStatus(memberId, visitId, visitItemId)
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VISIT_ITEM_NOT_FOUND));
            throw new BusinessRuleViolationException(ErrorCode.VISIT_ITEM_STATUS_VERSION_CONFLICT);
        }
        if (!visitRepository.updateActivity(memberId, visitId, now)) {
            throw new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        propertyAccessService.updateLastActivity(memberId, propertyId, now);
        final var item = visitCheckItemRepository.findOwnedStatus(memberId, visitId, visitItemId)
                .orElseThrow(() -> new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR));
        final VisitSummaryResult stageSummary = VisitSummaryResult.from(
                visitQueryRepository.findStageSummary(memberId, visitId, item.stage())
                        .orElseThrow(() -> new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR))
        );
        final VisitSummaryResult visitSummary = VisitSummaryResult.from(
                visitQueryRepository.findSummary(memberId, visitId)
                        .orElseThrow(() -> new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR))
        );
        return VisitItemStatusResult.from(item, stageSummary, visitSummary);
    }

    private VisitItemMemoResult updateItemMemoInTransaction(
            final long memberId,
            final long visitId,
            final long visitItemId,
            final InlineMemo memo,
            final long expectedMemoVersion
    ) {
        final long propertyId = findOwnedPropertyId(memberId, visitId);
        lockVisitProperty(memberId, propertyId);
        findOwnedVisitForUpdate(memberId, visitId);
        final Instant now = DatabaseTime.normalize(clock.instant());
        if (!visitCheckItemRepository.updateMemo(
                memberId,
                visitId,
                visitItemId,
                memo,
                expectedMemoVersion,
                now
        )) {
            visitCheckItemRepository.findOwnedMemo(memberId, visitId, visitItemId)
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VISIT_ITEM_NOT_FOUND));
            throw new BusinessRuleViolationException(ErrorCode.VISIT_ITEM_MEMO_VERSION_CONFLICT);
        }
        if (!visitRepository.updateActivity(memberId, visitId, now)) {
            throw new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        propertyAccessService.updateLastActivity(memberId, propertyId, now);
        final var item = visitCheckItemRepository.findOwnedMemo(memberId, visitId, visitItemId)
                .orElseThrow(() -> new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR));
        return VisitItemMemoResult.from(item);
    }

    private CompletionOutcome completeVisitInTransaction(final long memberId, final long visitId) {
        final long propertyId = findOwnedPropertyId(memberId, visitId);
        lockVisitProperty(memberId, propertyId);
        final Visit current = findOwnedVisitForUpdate(memberId, visitId);
        final Visit completed = current.complete(DatabaseTime.normalize(clock.instant()));
        if (current.status() == VisitStatus.IN_PROGRESS) {
            if (!visitRepository.complete(completed)) {
                throw new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
            propertyAccessService.updateLastActivity(memberId, propertyId, completed.updatedAt());
        }
        final VisitSummaryResult summary = VisitSummaryResult.from(
                visitQueryRepository.findSummary(memberId, visitId)
                        .orElseThrow(() -> new DataInconsistencyException(ErrorCode.INTERNAL_SERVER_ERROR))
        );
        return new CompletionOutcome(
                VisitCompleteResult.from(completed, summary),
                current.status() == VisitStatus.IN_PROGRESS
        );
    }

    private List<VisitStageSnapshot> toSnapshots(
            final long visitId,
            final List<ChecklistSnapshotSourceResult> sources,
            final Instant now
    ) {
        return sources.stream()
                .map(source -> new VisitStageSnapshot(
                        0,
                        visitId,
                        source.stage(),
                        source.checklistId(),
                        source.name(),
                        source.items().stream()
                                .map(item -> new VisitCheckItem(
                                        0,
                                        item.origin(),
                                        item.checklistItemId(),
                                        item.sourceCheckItemId(),
                                        item.question(),
                                        item.guide(),
                                        item.order(),
                                        CheckStatus.UNCONFIRMED,
                                        0,
                                        now,
                                        new InlineMemo(""),
                                        0,
                                        null
                                ))
                                .toList(),
                        now
                ))
                .toList();
    }

    private long findOwnedPropertyId(final long memberId, final long visitId) {
        return visitRepository.findOwnedPropertyId(memberId, visitId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VISIT_NOT_FOUND));
    }

    private void lockVisitProperty(final long memberId, final long propertyId) {
        try {
            propertyAccessService.lockOwned(memberId, propertyId);
        } catch (ResourceNotFoundException exception) {
            throw new ResourceNotFoundException(ErrorCode.VISIT_NOT_FOUND);
        }
    }

    private Visit findOwnedVisitForUpdate(final long memberId, final long visitId) {
        return visitRepository.findOwnedForUpdate(memberId, visitId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.VISIT_NOT_FOUND));
    }

    private void observeCompletion(final long memberId, final VisitCompleteResult result) {
        try {
            log.info("event=visit_completed memberId={} visitId={}", memberId, result.visitId());
            if (visitRepository.countCompletedProperties(memberId) >= 2) {
                log.info("event=repeat_usage_achieved memberId={}", memberId);
            }
        } catch (RuntimeException exception) {
            log.warn("event=visit_completion_observation_failed memberId={} visitId={} exceptionType={}",
                    memberId, result.visitId(), exception.getClass().getSimpleName());
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

    private record CompletionOutcome(VisitCompleteResult result, boolean firstCompletion) {
    }
}
