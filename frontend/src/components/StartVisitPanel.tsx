import { useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { getVisitErrorMessage, isAmbiguousVisitNetworkError } from '../apis/visitErrorMessages';
import { checklistStageMeta } from '../constants/checklist';
import { useStartPropertyVisit } from '../hooks/query/useVisitMutations';
import { CHECKLIST_STAGES } from '../types/Checklist';
import type { PublicConfig } from '../types/PublicConfig';
import type { PropertyDetail } from '../types/Property';
import ConfirmDialog from './ConfirmDialog';
import styles from './StartVisitPanel.module.css';

const StartVisitPanel = ({ config, property }: { config: PublicConfig; property: PropertyDetail }) => {
  const navigate = useNavigate();
  const buttonRef = useRef<HTMLButtonElement>(null);
  const [isOpen, setIsOpen] = useState(false);
  const startVisit = useStartPropertyVisit(config, property.propertyId);

  const start = async () => {
    try {
      const visit = await startVisit.mutateAsync();
      setIsOpen(false);
      navigate(`/visits/${visit.visitId}`);
    } catch {
      // The confirmation stays open with a retryable, non-destructive error.
    }
  };

  if (property.activeChecklists.length === 0) {
    return (
      <div className={styles.empty}>
        <strong>활성 체크리스트를 먼저 연결해 주세요.</strong>
        <p>방문을 시작하면 연결된 단계의 현재 질문을 독립된 스냅샷으로 보관합니다.</p>
        <ul>
          {CHECKLIST_STAGES.map((stage) => (
            <li key={stage}>
              <Link to={`/properties/${property.propertyId}/active-checklists/${stage}`}>
                {checklistStageMeta[stage].label} 체크리스트 연결
              </Link>
            </li>
          ))}
        </ul>
      </div>
    );
  }

  return (
    <div className={styles.panel}>
      <button ref={buttonRef} className="primary-button" type="button" onClick={() => setIsOpen(true)}>
        새 방문 시작
      </button>
      <p>시작할 때의 체크리스트를 복사하므로 이후 원본을 바꿔도 이 방문은 유지됩니다.</p>
      <ConfirmDialog
        isOpen={isOpen}
        title={`${property.name} 방문을 시작할까요?`}
        description={
          <>
            <p>
              활성 단계 {property.activeChecklists.length}개의 질문을 지금 상태 그대로 복사합니다. 시작한 방문은 삭제할
              수 없습니다.
            </p>
            <ul>
              {property.activeChecklists.map((checklist) => (
                <li key={checklist.stage}>
                  {checklistStageMeta[checklist.stage].label} · {checklist.name} · {checklist.itemCount}개
                </li>
              ))}
            </ul>
            <p>원본 체크리스트를 나중에 수정·교체·삭제해도 이 방문의 질문은 바뀌지 않습니다.</p>
          </>
        }
        confirmLabel="방문 시작"
        isConfirming={startVisit.isPending}
        returnFocusRef={buttonRef}
        tone="primary"
        onCancel={() => {
          startVisit.reset();
          setIsOpen(false);
        }}
        onConfirm={() => void start()}
      >
        {startVisit.isError && (
          <div className="form-error" role="alert">
            <strong>{getVisitErrorMessage(startVisit.error)}</strong>
            {isAmbiguousVisitNetworkError(startVisit.error) && (
              <p>
                요청 결과를 알 수 없어 자동으로 다시 시작하지 않습니다. 방문 기록에서 생성 여부를 먼저 확인해 주세요.
              </p>
            )}
            <Link to={`/properties/${property.propertyId}/visits`}>방문 기록 확인</Link>
          </div>
        )}
      </ConfirmDialog>
    </div>
  );
};

export default StartVisitPanel;
