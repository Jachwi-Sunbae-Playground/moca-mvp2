import { useEffect, useState } from 'react';
import {
  getVisitErrorMessage,
  isVisitMemoVersionConflict,
  isVisitStatusVersionConflict,
} from '../apis/visitErrorMessages';
import { VISIT_ITEM_STATUSES, visitItemStatusMeta } from '../constants/visit';
import { useVisitItemAutosave } from '../hooks/query/useVisitItemAutosave';
import type { VisitItemAutosaveHandle } from '../hooks/query/useVisitAutosaveRegistry';
import type { PublicConfig } from '../types/PublicConfig';
import type { VisitSnapshotItem } from '../types/Visit';
import { formatDateTime } from '../utils/propertyFormat';
import styles from './VisitItemStatusControl.module.css';

const VisitItemStatusControl = ({
  config,
  visitId,
  propertyId,
  item,
  announce,
  register,
  onActivityChange,
}: {
  config: PublicConfig;
  visitId: number;
  propertyId: number;
  item: VisitSnapshotItem;
  announce: (message: string) => void;
  register: (visitItemId: number, handle: VisitItemAutosaveHandle) => () => void;
  onActivityChange: () => void;
}) => {
  const [isMemoOpen, setIsMemoOpen] = useState(item.inlineMemo.trim().length > 0);
  const autosave = useVisitItemAutosave({
    config,
    visitId,
    propertyId,
    item,
    announce,
    register,
    onActivityChange,
  });
  const statusStateId = `visit-status-state-${item.visitItemId}`;
  const statusErrorId = `visit-status-error-${item.visitItemId}`;
  const memoId = `visit-memo-${item.visitItemId}`;
  const memoHelpId = `visit-memo-help-${item.visitItemId}`;
  const memoStateId = `visit-memo-state-${item.visitItemId}`;
  const memoErrorId = `visit-memo-error-${item.visitItemId}`;
  const statusHasError = autosave.statusPhase === 'error';
  const memoHasError = autosave.memoPhase === 'error';

  useEffect(() => {
    if (memoHasError) setIsMemoOpen(true);
  }, [memoHasError]);

  return (
    <li className={styles.card} data-status={autosave.displayedStatus} data-pending={autosave.isPending}>
      <fieldset aria-describedby={`${statusStateId}${statusHasError ? ` ${statusErrorId}` : ''}`}>
        <legend className="sr-only">
          {item.order}번. {item.question}
        </legend>
        <div className={styles.cardContent}>
          <div className={styles.questionArea}>
            <strong aria-hidden="true">{item.question}</strong>
          </div>

          <div className={styles.statusOptions}>
            {VISIT_ITEM_STATUSES.map((status) => {
              const meta = visitItemStatusMeta[status];
              return (
                <label key={status} data-status={status}>
                  <input
                    type="radio"
                    name={`visit-item-${item.visitItemId}`}
                    value={status}
                    checked={autosave.displayedStatus === status}
                    onClick={() => {
                      if (autosave.displayedStatus === status) autosave.selectStatus(status);
                    }}
                    onChange={() => autosave.selectStatus(status)}
                  />
                  <span className={styles.statusCircle} aria-hidden="true" />
                  <strong>{meta.label}</strong>
                  <small className="sr-only">{meta.description}</small>
                </label>
              );
            })}
          </div>
        </div>
      </fieldset>

      <button
        type="button"
        className={styles.memoToggle}
        aria-expanded={isMemoOpen}
        aria-controls={`${memoId}-panel`}
        aria-label={`${item.question} 메모 ${isMemoOpen ? '닫기' : '열기'}`}
        onClick={() => setIsMemoOpen((current) => !current)}
      >
        <span aria-hidden="true" />
      </button>

      {isMemoOpen && (
        <div id={`${memoId}-panel`} className={styles.memoPanel}>
          <div className={styles.memo}>
            <div className="sr-only">
              <label htmlFor={memoId}>한 줄 메모</label>
              <span data-limit={autosave.memoLimitReached}>
                {autosave.memoCount}/200<span>자</span>
              </span>
            </div>
            <input
              id={memoId}
              type="text"
              value={autosave.memoDraft}
              placeholder="메모 입력…"
              aria-invalid={memoHasError}
              aria-describedby={`${memoHelpId} ${memoStateId}${memoHasError ? ` ${memoErrorId}` : ''}`}
              onChange={(event) => autosave.changeMemo(event.target.value)}
              onBlur={() => void autosave.flushMemo()}
            />
            <span id={memoHelpId} className="sr-only">
              줄바꿈 없이 200자까지, 입력을 멈추면 1초 뒤 저장해요. 공백도 입력한 그대로 보존합니다.
            </span>
          </div>
        </div>
      )}

      <div id={statusStateId} className="sr-only">
        {autosave.statusPhase === 'saving' && <span>상태 저장 중… · 현재 v{autosave.statusVersion}</span>}
        {autosave.statusPhase === 'refreshing' && <span>최신 상태 버전 확인 중…</span>}
        {autosave.statusPhase !== 'saving' && autosave.statusPhase !== 'refreshing' && (
          <span>
            상태 v{autosave.statusVersion} · 마지막 저장 {formatDateTime(autosave.statusSavedAt)}
          </span>
        )}
      </div>
      {statusHasError && (
        <div id={statusErrorId} className={styles.error} role="alert" data-autosave-error="true">
          <strong>
            {isVisitStatusVersionConflict(autosave.statusError)
              ? '최신 상태와 다시 겹쳤어요. 자동 재시도를 멈췄습니다.'
              : getVisitErrorMessage(autosave.statusError)}
          </strong>
          <span>마지막 선택은 화면에 유지됩니다.</span>
          <button type="button" className="inline-button" onClick={autosave.retryStatus}>
            상태 다시 저장
          </button>
        </div>
      )}

      <div id={memoStateId} className="sr-only">
        {autosave.memoPhase === 'saving' && <span>메모 저장 중… · 현재 v{autosave.memoVersion}</span>}
        {autosave.memoPhase === 'refreshing' && <span>최신 메모 버전 확인 중…</span>}
        {autosave.memoPhase !== 'saving' && autosave.memoPhase !== 'refreshing' && (
          <span>
            메모 v{autosave.memoVersion} ·{' '}
            {autosave.memoSavedAt === null
              ? '아직 저장하지 않음'
              : `마지막 저장 ${formatDateTime(autosave.memoSavedAt)}`}
          </span>
        )}
      </div>
      {memoHasError && (
        <div id={memoErrorId} className={styles.error} role="alert" data-autosave-error="true">
          <strong>
            {isVisitMemoVersionConflict(autosave.memoError)
              ? '최신 메모와 다시 겹쳤어요. 자동 재시도를 멈췄습니다.'
              : getVisitErrorMessage(autosave.memoError)}
          </strong>
          <span>작성한 메모는 지우지 않았습니다.</span>
          <button type="button" className="inline-button" onClick={autosave.retryMemo}>
            메모 다시 저장
          </button>
        </div>
      )}
    </li>
  );
};

export default VisitItemStatusControl;
