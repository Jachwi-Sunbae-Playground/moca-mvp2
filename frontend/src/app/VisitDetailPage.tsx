import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { ApiError } from '../apis/apiClient';
import { getVisitErrorMessage } from '../apis/visitErrorMessages';
import { checklistStageMeta, isChecklistStage } from '../constants/checklist';
import ChecklistStageTabs from '../components/ChecklistStageTabs';
import ConfirmDialog from '../components/ConfirmDialog';
import VisitAggregateSummary from '../components/VisitAggregateSummary';
import VisitItemStatusControl from '../components/VisitItemStatusControl';
import { ButtonLink } from '../components/ui/Button';
import TopNavigation from '../components/ui/TopNavigation';
import { useVisitAutosaveRegistry } from '../hooks/query/useVisitAutosaveRegistry';
import { useCompleteVisit } from '../hooks/query/useVisitMutations';
import { usePropertyDetail } from '../hooks/query/useProperties';
import { useVisitDetail } from '../hooks/query/useVisits';
import { useAutosaveNavigationGuard } from '../hooks/useAutosaveNavigationGuard';
import { CHECKLIST_STAGES } from '../types/Checklist';
import type { ChecklistStage } from '../types/Checklist';
import type { PublicConfig } from '../types/PublicConfig';
import type { VisitDetail } from '../types/Visit';
import { formatDateTime, parsePositiveId } from '../utils/propertyFormat';
import styles from './VisitDetailPage.module.css';

const VisitDetailPage = ({ config }: { config: PublicConfig }) => {
  const visitId = parsePositiveId(useParams().visitId);
  if (visitId === null) return <InvalidVisitState />;
  return <ResolvedVisitDetailPage config={config} visitId={visitId} />;
};

const InvalidVisitState = () => (
  <main className="property-page">
    <div className="page-container content-state">
      <strong>올바른 방문 주소가 아니에요.</strong>
      <Link to="/properties">매물 목록으로 돌아가기</Link>
    </div>
  </main>
);

const ResolvedVisitDetailPage = ({ config, visitId }: { config: PublicConfig; visitId: number }) => {
  const visit = useVisitDetail(config, visitId);
  if (visit.isPending) {
    return (
      <main className="property-page">
        <div className="page-container content-state" role="status">
          <span className="spinner" />
          방문 상세를 불러오는 중이에요.
        </div>
      </main>
    );
  }
  if (visit.isError) {
    const notFound = visit.error instanceof ApiError && visit.error.code === 'VISIT_NOT_FOUND';
    return (
      <main className="property-page">
        <div className="page-container content-state content-state--error" role="alert">
          <strong>{notFound ? '방문 기록을 찾을 수 없어요.' : '방문 상세를 불러오지 못했어요.'}</strong>
          <span>{getVisitErrorMessage(visit.error)}</span>
          {!notFound && (
            <button className="inline-button" type="button" onClick={() => void visit.refetch()}>
              다시 시도
            </button>
          )}
          <Link to="/properties">매물 목록으로 돌아가기</Link>
        </div>
      </main>
    );
  }
  return <VisitDetailContent config={config} detail={visit.data} />;
};

const VisitDetailContent = ({ config, detail }: { config: PublicConfig; detail: VisitDetail }) => {
  const orderedStages = CHECKLIST_STAGES.flatMap((stage) => detail.stages.filter((item) => item.stage === stage));
  const [searchParams, setSearchParams] = useSearchParams();
  const [selectedStage, setSelectedStage] = useState<ChecklistStage>(() => {
    const requestedStage = searchParams.get('stage');
    return isChecklistStage(requestedStage) ? requestedStage : (orderedStages[0]?.stage ?? 'ONLINE_PHONE');
  });
  const [liveMessage, setLiveMessage] = useState('');
  const [isCompleteDialogOpen, setIsCompleteDialogOpen] = useState(false);
  const [isFlushingForCompletion, setIsFlushingForCompletion] = useState(false);
  const [completionFlushError, setCompletionFlushError] = useState(false);
  const completeButtonRef = useRef<HTMLButtonElement>(null);
  const property = usePropertyDetail(config, detail.propertyId);
  const completion = useCompleteVisit(config, detail);
  const autosave = useVisitAutosaveRegistry();
  const currentStage = orderedStages.find((stage) => stage.stage === selectedStage);
  const title = property.data === undefined ? '방문 기록' : `${property.data.name} 방문`;

  useEffect(() => {
    const requestedStage = searchParams.get('stage');
    if (isChecklistStage(requestedStage) && requestedStage !== selectedStage) setSelectedStage(requestedStage);
  }, [searchParams, selectedStage]);

  const handleFlushFailure = useCallback(() => {
    setLiveMessage('저장하지 못한 항목이 있어 화면에 남았습니다. 채널별 다시 저장 버튼을 이용해 주세요.');
    window.setTimeout(() => autosave.focusFirstError(), 0);
  }, [autosave]);

  useAutosaveNavigationGuard({
    shouldFlush: autosave.hasPending,
    flush: autosave.flushAll,
    onFlushFailure: handleFlushFailure,
  });

  const selectStage = async (stage: ChecklistStage) => {
    if (stage === selectedStage) return true;
    if (autosave.hasPending && !(await autosave.flushAll())) {
      handleFlushFailure();
      return false;
    }
    setSelectedStage(stage);
    setSearchParams({ stage }, { replace: true });
    return true;
  };

  const complete = async () => {
    setCompletionFlushError(false);
    setIsFlushingForCompletion(true);
    try {
      if (!(await autosave.flushAll())) {
        setCompletionFlushError(true);
        setIsCompleteDialogOpen(false);
        handleFlushFailure();
        return;
      }
      await completion.mutateAsync();
      setIsCompleteDialogOpen(false);
      setLiveMessage('방문을 완료했어요. 완료 후에도 항목을 계속 수정할 수 있어요.');
    } catch {
      setLiveMessage('방문을 완료하지 못했어요. 진행 중 상태는 유지됩니다.');
    } finally {
      setIsFlushingForCompletion(false);
    }
  };

  const handleCompletionDialogClose = useCallback(() => {
    if (completionFlushError) autosave.focusFirstError();
  }, [autosave, completionFlushError]);

  return (
    <main className={`property-page ${styles.page}`}>
      <div className={`page-container ${styles.container}`}>
        <div className="sr-only" role="status" aria-live="polite" aria-atomic="true">
          {liveMessage}
        </div>
        <TopNavigation
          title={property.data === undefined ? '매물 체크리스트' : `${property.data.name} 체크리스트`}
          backTo={`/properties/${detail.propertyId}`}
          backLabel="매물 상세로 돌아가기"
        />
        <h1 className="sr-only">{title}</h1>
        <div className="sr-only">
          <time dateTime={detail.startedAt}>시작 {formatDateTime(detail.startedAt)}</time>
          <span data-status={detail.status}>{detail.status === 'COMPLETED' ? '방문 완료' : '확인 진행 중'}</span>
          {detail.completedAt !== null && (
            <time dateTime={detail.completedAt}>최초 완료 {formatDateTime(detail.completedAt)}</time>
          )}
        </div>
        {property.isError && (
          <p className="form-error" role="alert">
            매물 이름을 불러오지 못했지만 방문 기록은 계속 확인할 수 있어요.
          </p>
        )}
        <div className="sr-only">
          <VisitAggregateSummary summary={detail.summary} />
        </div>
        <div className={styles.stageTabs}>
          <ChecklistStageTabs
            stage={selectedStage}
            variant="progress"
            idPrefix={`visit-stage-${detail.visitId}`}
            onSelect={selectStage}
          />
        </div>

        {detail.status === 'COMPLETED' && (
          <p className={styles.completedNote}>
            완료한 방문입니다. 최초 완료 시각은 유지되며 아래 항목은 계속 수정할 수 있어요.
          </p>
        )}

        {currentStage === undefined && (
          <section className={styles.unavailableStage} aria-labelledby={`unavailable-stage-${selectedStage}`}>
            <h2 id={`unavailable-stage-${selectedStage}`}>{checklistStageMeta[selectedStage].shortLabel}</h2>
            <strong>이 단계에 연결된 체크리스트가 없어요.</strong>
            <p>체크리스트를 선택하면 새 체크에서 이 단계의 항목을 확인할 수 있어요.</p>
            <ButtonLink to={`/properties/${detail.propertyId}/active-checklists/${selectedStage}`} variant="secondary">
              체크리스트 선택하러 가기
            </ButtonLink>
          </section>
        )}

        {orderedStages.map((stage) => (
          <section
            key={stage.stage}
            id={`visit-panel-${stage.stage}`}
            role="tabpanel"
            aria-labelledby={`visit-stage-${detail.visitId}-${stage.stage}`}
            className={styles.stagePanel}
            hidden={currentStage?.stage !== stage.stage}
          >
            <div className={styles.stageHeading}>
              <h2>전체</h2>
              <strong aria-label={`${stage.summary.totalCount}개 중 ${stage.summary.checkedCount}개 확인`}>
                {stage.summary.checkedCount}/{stage.summary.totalCount}
              </strong>
            </div>
            <p className="sr-only">
              체크리스트 이름: {stage.checklistName}.{' '}
              {stage.sourceChecklistId === null
                ? '원본 체크리스트는 삭제되었지만 이 방문의 질문은 그대로 보관됩니다.'
                : '방문 시작 당시 질문을 보관한 사본입니다. 현재 원본과 다를 수 있어요.'}
            </p>
            <ol className={styles.itemList}>
              {stage.items.map((item) => (
                <VisitItemStatusControl
                  key={item.visitItemId}
                  config={config}
                  visitId={detail.visitId}
                  propertyId={detail.propertyId}
                  item={item}
                  announce={setLiveMessage}
                  register={autosave.register}
                  onActivityChange={autosave.notify}
                />
              ))}
            </ol>
          </section>
        ))}

        {detail.status === 'IN_PROGRESS' && currentStage !== undefined && (
          <>
            {completionFlushError && (
              <p className="form-error visit-completion-flush-error" role="alert">
                저장하지 못한 항목이 있어 방문을 완료하지 않았어요. 해당 상태나 메모를 다시 저장한 뒤 완료해 주세요.
              </p>
            )}
            <div className={styles.completeAction}>
              <button
                ref={completeButtonRef}
                className={styles.completeButton}
                type="button"
                disabled={isFlushingForCompletion || completion.isPending}
                onClick={() => setIsCompleteDialogOpen(true)}
              >
                {autosave.hasPending ? '모두 저장하고 체크 완료' : '체크 완료 및 저장'}
              </button>
            </div>
          </>
        )}

        <ConfirmDialog
          isOpen={isCompleteDialogOpen}
          title="이 방문을 완료할까요?"
          description={
            <>
              <p>
                전체 {detail.summary.totalCount}개 중 {detail.summary.checkedCount}개를 확인했습니다.
              </p>
              <ul>
                <li>괜찮음 {detail.summary.goodCount}개</li>
                <li>주의 {detail.summary.cautionCount}개</li>
                <li>미확인 {detail.summary.unconfirmedCount}개</li>
              </ul>
              <p>
                미확인 항목이 있어도 완료할 수 있습니다. 완료는 취소할 수 없지만, 항목 선택은 이후에도 수정할 수
                있습니다.
              </p>
              <p>저장 대기 중인 상태와 메모가 있다면 모두 저장된 뒤에만 방문을 완료합니다.</p>
            </>
          }
          confirmLabel="방문 완료"
          isConfirming={completion.isPending || isFlushingForCompletion}
          returnFocusRef={completeButtonRef}
          tone="primary"
          onCancel={() => setIsCompleteDialogOpen(false)}
          onConfirm={() => void complete()}
          onAfterClose={handleCompletionDialogClose}
        >
          {completion.isError && (
            <p className="form-error" role="alert">
              {getVisitErrorMessage(completion.error)} 진행 중 상태는 유지됩니다.
            </p>
          )}
        </ConfirmDialog>
      </div>
    </main>
  );
};

export default VisitDetailPage;
