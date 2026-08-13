import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ApiError } from '../apis/apiClient';
import { getChecklistErrorMessage } from '../apis/checklistErrorMessages';
import { checklistQueryKeys } from './checklistQueryKeys';
import { queryClient } from './queryClient';
import ChecklistEditor from '../components/ChecklistEditor';
import ConfirmDialog from '../components/ConfirmDialog';
import PageHeading from '../components/PageHeading';
import { checklistStageMeta } from '../constants/checklist';
import { useRemoveChecklist, useUpdateChecklist } from '../hooks/query/useChecklistMutations';
import { useChecklistDetail } from '../hooks/query/useChecklists';
import { checklistItemToEditorItem } from '../types/ChecklistEditor';
import type { PublicConfig } from '../types/PublicConfig';
import { toUpdateChecklistItems } from '../utils/checklistEditor';
import { parsePositiveId } from '../utils/propertyFormat';

const ChecklistDetailPage = ({ config }: { config: PublicConfig }) => {
  const checklistId = parsePositiveId(useParams().resource);
  if (checklistId === null) return <InvalidChecklist />;
  return <ResolvedChecklistDetail config={config} checklistId={checklistId} />;
};

const InvalidChecklist = () => (
  <main className="property-page">
    <div className="page-container">
      <div className="content-state">
        <strong>올바른 체크리스트 주소가 아니에요.</strong>
        <Link to="/checklists">체크리스트 홈으로 돌아가기</Link>
      </div>
    </div>
  </main>
);

const ResolvedChecklistDetail = ({ config, checklistId }: { config: PublicConfig; checklistId: number }) => {
  const navigate = useNavigate();
  const detail = useChecklistDetail(config, checklistId);
  const update = useUpdateChecklist(config, checklistId);
  const remove = useRemoveChecklist(config, checklistId);
  const [isDeleteOpen, setIsDeleteOpen] = useState(false);
  const deleteRef = useRef<HTMLButtonElement>(null);
  const deleteSucceededRef = useRef(false);

  useEffect(
    () => () => {
      if (deleteSucceededRef.current) {
        queryClient.removeQueries({ queryKey: checklistQueryKeys.detail(checklistId), exact: true });
      }
    },
    [checklistId],
  );

  if (detail.isPending)
    return (
      <main className="property-page">
        <div className="page-container">
          <div className="content-state" role="status">
            <span className="spinner" />
            체크리스트를 불러오는 중이에요.
          </div>
        </div>
      </main>
    );
  if (detail.isError) {
    const notFound = detail.error instanceof ApiError && detail.error.code === 'CHECKLIST_NOT_FOUND';
    return (
      <main className="property-page">
        <div className="page-container">
          <div className="content-state content-state--error" role="alert">
            <strong>{notFound ? '체크리스트를 찾을 수 없어요.' : '체크리스트를 불러오지 못했어요.'}</strong>
            <span>{getChecklistErrorMessage(detail.error)}</span>
            {!notFound && (
              <button type="button" className="inline-button" onClick={() => void detail.refetch()}>
                다시 시도
              </button>
            )}
            <Link to="/checklists">체크리스트 홈으로 돌아가기</Link>
          </div>
        </div>
      </main>
    );
  }

  const checklist = detail.data;
  const deleteChecklist = async () => {
    try {
      await remove.mutateAsync();
      deleteSucceededRef.current = true;
      navigate(`/checklists/${checklist.stage}`, { replace: true, state: { focusHeading: true } });
    } catch {
      /* Keep the dialog open for retry. */
    }
  };

  return (
    <main className="property-page checklist-page">
      <div className="page-container page-container--form">
        <PageHeading
          title={checklist.name}
          description={`${checklistStageMeta[checklist.stage].label} · 매물 ${checklist.assignedPropertyCount}곳에서 사용 중`}
          backTo={`/checklists/${checklist.stage}`}
          backLabel="목록"
        />
        <div className="detail-actions">
          <button ref={deleteRef} type="button" className="danger-outline-button" onClick={() => setIsDeleteOpen(true)}>
            체크리스트 삭제
          </button>
        </div>
        <ChecklistEditor
          key={checklist.checklistId}
          config={config}
          stage={checklist.stage}
          initialName={checklist.name}
          initialItems={checklist.items.map(checklistItemToEditorItem)}
          submitLabel="변경 내용 저장"
          isSubmitting={update.isPending}
          serverError={update.isError ? getChecklistErrorMessage(update.error) : undefined}
          onSubmit={async ({ name, items }) => {
            return update.mutateAsync({ name, items: toUpdateChecklistItems(items) });
          }}
        />
        <ConfirmDialog
          isOpen={isDeleteOpen}
          title={`${checklist.name}을 삭제할까요?`}
          description={
            <>
              <p>
                {checklist.itemCount}개 항목과 매물 {checklist.assignedPropertyCount}곳의 활성 연결이 함께 삭제됩니다.
              </p>
              <p>완료한 방문 기록의 스냅샷은 유지됩니다.</p>
            </>
          }
          confirmLabel="체크리스트 삭제"
          isConfirming={remove.isPending}
          returnFocusRef={deleteRef}
          onCancel={() => setIsDeleteOpen(false)}
          onConfirm={() => void deleteChecklist()}
        >
          {remove.isError && (
            <p className="form-error" role="alert">
              {getChecklistErrorMessage(remove.error)} 체크리스트는 그대로 유지됩니다.
            </p>
          )}
        </ConfirmDialog>
      </div>
    </main>
  );
};

export default ChecklistDetailPage;
