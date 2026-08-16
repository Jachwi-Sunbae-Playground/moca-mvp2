import { useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { getChecklistErrorMessage } from '../apis/checklistErrorMessages';
import { useRemoveChecklist } from '../hooks/query/useChecklistMutations';
import type { ChecklistSummary } from '../types/Checklist';
import type { PublicConfig } from '../types/PublicConfig';
import ConfirmDialog from './ConfirmDialog';
import styles from './ChecklistListCard.module.css';

const ChecklistListCard = ({ config, checklist }: { config: PublicConfig; checklist: ChecklistSummary }) => {
  const [isOpen, setIsOpen] = useState(false);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const remove = useRemoveChecklist(config, checklist.checklistId);

  const confirm = async () => {
    try {
      await remove.mutateAsync();
      setIsOpen(false);
    } catch {
      // Keep the item and dialog visible so the user can safely retry.
    }
  };

  return (
    <li className={styles.card}>
      <Link className={styles.main} to={`/checklists/${checklist.checklistId}`}>
        <strong>{checklist.name}</strong>
        <span>
          {checklist.itemCount}개 항목 · 매물 {checklist.assignedPropertyCount}곳에서 사용
        </span>
      </Link>
      <div className={styles.actions}>
        <Link className="inline-link" to={`/checklists/${checklist.checklistId}`}>
          편집
        </Link>
        <button ref={triggerRef} type="button" className="text-danger-button" onClick={() => setIsOpen(true)}>
          삭제
        </button>
      </div>
      <ConfirmDialog
        isOpen={isOpen}
        title={`${checklist.name}을 삭제할까요?`}
        description={
          <>
            <p>
              {checklist.itemCount}개 항목과 현재 매물 {checklist.assignedPropertyCount}곳의 활성 연결이 함께
              삭제됩니다.
            </p>
            <p>이미 완료한 방문의 스냅샷은 유지되며 삭제는 되돌릴 수 없습니다.</p>
          </>
        }
        confirmLabel="체크리스트 삭제"
        isConfirming={remove.isPending}
        returnFocusRef={triggerRef}
        onCancel={() => setIsOpen(false)}
        onConfirm={() => void confirm()}
      >
        {remove.isError && (
          <p className="form-error" role="alert">
            {getChecklistErrorMessage(remove.error)} 체크리스트는 그대로 유지됩니다.
          </p>
        )}
      </ConfirmDialog>
    </li>
  );
};

export default ChecklistListCard;
