import { useCallback, useRef, useState } from 'react';
import type { KeyboardEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiError } from '../apis/apiClient';
import { getVisitErrorMessage } from '../apis/visitErrorMessages';
import { checklistStageMeta } from '../constants/checklist';
import ConfirmDialog from '../components/ConfirmDialog';
import PageHeading from '../components/PageHeading';
import VisitAggregateSummary from '../components/VisitAggregateSummary';
import VisitItemStatusControl from '../components/VisitItemStatusControl';
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
  const [selectedStage, setSelectedStage] = useState<ChecklistStage>(orderedStages[0]?.stage ?? 'ONLINE_PHONE');
  const [liveMessage, setLiveMessage] = useState('');
  const [isCompleteDialogOpen, setIsCompleteDialogOpen] = useState(false);
  const [isFlushingForCompletion, setIsFlushingForCompletion] = useState(false);
  const [completionFlushError, setCompletionFlushError] = useState(false);
  const completeButtonRef = useRef<HTMLButtonElement>(null);
  const property = usePropertyDetail(config, detail.propertyId);
  const completion = useCompleteVisit(config, detail);
  const autosave = useVisitAutosaveRegistry();
  const currentStage = orderedStages.find((stage) => stage.stage === selectedStage) ?? orderedStages[0];
  const title = property.data === undefined ? '방문 기록' : `${property.data.name} 방문`;

  const handleFlushFailure = useCallback(() => {
    setLiveMessage('저장하지 못한 항목이 있어 화면에 남았습니다. 채널별 다시 저장 버튼을 이용해 주세요.');
    window.setTimeout(() => autosave.focusFirstError(), 0);
  }, [autosave]);

  useAutosaveNavigationGuard({
    shouldFlush: autosave.hasPending,
    flush: autosave.flushAll,
    onFlushFailure: handleFlushFailure,
  });

  const selectStage = async (stage: ChecklistStage, focusTab = false) => {
    if (stage === currentStage?.stage) return;
    if (autosave.hasPending && !(await autosave.flushAll())) {
      handleFlushFailure();
      return;
    }
    setSelectedStage(stage);
    if (focusTab) window.setTimeout(() => document.getElementById(`visit-tab-${stage}`)?.focus(), 0);
  };

  const moveStageFocus = (event: KeyboardEvent<HTMLButtonElement>, currentIndex: number) => {
    let nextIndex: number | null = null;

    if (event.key === 'ArrowRight') nextIndex = (currentIndex + 1) % orderedStages.length;
    if (event.key === 'ArrowLeft') nextIndex = (currentIndex - 1 + orderedStages.length) % orderedStages.length;
    if (event.key === 'Home') nextIndex = 0;
    if (event.key === 'End') nextIndex = orderedStages.length - 1;
    if (nextIndex === null) return;

    event.preventDefault();
    const nextStage = orderedStages[nextIndex];
    if (nextStage === undefined) return;
    void selectStage(nextStage.stage, true);
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
    <main className="property-page visit-detail-page">
      <div className="page-container">
        <div className="sr-only" role="status" aria-live="polite" aria-atomic="true">
          {liveMessage}
        </div>
        <PageHeading
          title={title}
          description={`시작 ${formatDateTime(detail.startedAt)}`}
          backTo={`/properties/${detail.propertyId}/visits`}
          backLabel="방문 기록"
        />
        <div className="visit-detail-meta">
          <span data-status={detail.status}>{detail.status === 'COMPLETED' ? '방문 완료' : '확인 진행 중'}</span>
          {detail.completedAt !== null && (
            <time dateTime={detail.completedAt}>최초 완료 {formatDateTime(detail.completedAt)}</time>
          )}
          <Link to={`/properties/${detail.propertyId}`}>매물 상세</Link>
        </div>
        {property.isError && (
          <p className="form-error" role="alert">
            매물 이름을 불러오지 못했지만 방문 기록은 계속 확인할 수 있어요.
          </p>
        )}
        <VisitAggregateSummary summary={detail.summary} />
        {detail.status === 'COMPLETED' && (
          <p className="visit-completed-note">
            완료한 방문입니다. 최초 완료 시각은 유지되며 아래 항목은 계속 수정할 수 있어요.
          </p>
        )}

        <div className="visit-stage-tabs" role="tablist" aria-label="방문 확인 단계">
          {orderedStages.map((stage, index) => (
            <button
              key={stage.stage}
              id={`visit-tab-${stage.stage}`}
              type="button"
              role="tab"
              aria-selected={currentStage?.stage === stage.stage}
              aria-controls={`visit-panel-${stage.stage}`}
              tabIndex={currentStage?.stage === stage.stage ? 0 : -1}
              onClick={() => void selectStage(stage.stage)}
              onKeyDown={(event) => moveStageFocus(event, index)}
            >
              <span>{checklistStageMeta[stage.stage].label}</span>
              <small>
                {stage.summary.checkedCount}/{stage.summary.totalCount}
              </small>
            </button>
          ))}
        </div>

        {orderedStages.map((stage) => (
          <section
            key={stage.stage}
            id={`visit-panel-${stage.stage}`}
            role="tabpanel"
            aria-labelledby={`visit-tab-${stage.stage}`}
            className="visit-stage-panel"
            hidden={currentStage?.stage !== stage.stage}
          >
            <div className="visit-stage-panel__heading">
              <div>
                <p className="section-eyebrow">스냅샷 체크리스트</p>
                <h2>{stage.checklistName}</h2>
              </div>
              <VisitAggregateSummary summary={stage.summary} label={`${checklistStageMeta[stage.stage].label} 집계`} />
            </div>
            <p className="section-note">
              {stage.sourceChecklistId === null
                ? '원본 체크리스트는 삭제되었지만 이 방문의 질문은 그대로 보관됩니다.'
                : '방문 시작 당시 질문을 보관한 사본입니다. 현재 원본과 다를 수 있어요.'}
            </p>
            <ol className="visit-item-list">
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

        {detail.status === 'IN_PROGRESS' && (
          <>
            {completionFlushError && (
              <p className="form-error visit-completion-flush-error" role="alert">
                저장하지 못한 항목이 있어 방문을 완료하지 않았어요. 해당 상태나 메모를 다시 저장한 뒤 완료해 주세요.
              </p>
            )}
            <div className="sticky-form-action visit-complete-action">
              <button
                ref={completeButtonRef}
                className="primary-button"
                type="button"
                disabled={isFlushingForCompletion || completion.isPending}
                onClick={() => setIsCompleteDialogOpen(true)}
              >
                {autosave.hasPending ? '모두 저장하고 방문 완료' : '방문 완료'}
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
