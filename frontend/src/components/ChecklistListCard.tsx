import { useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { getChecklistErrorMessage } from '../apis/checklistErrorMessages';
import { useRemoveChecklist } from '../hooks/query/useChecklistMutations';
import type { ChecklistSummary } from '../types/Checklist';
import type { PublicConfig } from '../types/PublicConfig';
import ConfirmDialog from './ConfirmDialog';
import Icon from './ui/Icon';
import styles from './ChecklistListCard.module.css';

const ChecklistListCard = ({ config, checklist }: { config: PublicConfig; checklist: ChecklistSummary }) => {
  const [isDeleteOpen, setIsDeleteOpen] = useState(false);
  const deleteButtonRef = useRef<HTMLButtonElement>(null);
  const remove = useRemoveChecklist(config, checklist.checklistId);

  const deleteChecklist = async () => {
    try {
      await remove.mutateAsync();
      setIsDeleteOpen(false);
    } catch {
      /* Keep the dialog open so the user can retry or cancel. */
    }
  };

  return (
    <li className={styles.card}>
      <Link className={styles.main} to={`/checklists/${checklist.checklistId}`}>
        <span className={styles.copy}>
          <strong>{checklist.name}</strong>
          <span>{checklist.itemCount}개 항목</span>
        </span>
      </Link>
      <div className={styles.actions}>
        <Link className={styles.edit} to={`/checklists/${checklist.checklistId}`} aria-label={`${checklist.name} 편집`}>
          <Icon name="edit" size={14} />
        </Link>
        <button
          ref={deleteButtonRef}
          className={styles.delete}
          type="button"
          aria-label={`${checklist.name} 삭제`}
          onClick={() => setIsDeleteOpen(true)}
        >
          <Icon name="trash" size={14} />
        </button>
      </div>
      <ConfirmDialog
        isOpen={isDeleteOpen}
        title={`${checklist.name}을 삭제할까요?`}
        description={
          <>
            <p>
              {checklist.itemCount}개 항목과 매물 {checklist.assignedPropertyCount}곳의 활성 연결이 함께 삭제됩니다.
            </p>
            <p>매물에 적용된 체크 결과는 유지됩니다.</p>
          </>
        }
        confirmLabel="체크리스트 삭제"
        isConfirming={remove.isPending}
        returnFocusRef={deleteButtonRef}
        onCancel={() => setIsDeleteOpen(false)}
        onConfirm={() => void deleteChecklist()}
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
