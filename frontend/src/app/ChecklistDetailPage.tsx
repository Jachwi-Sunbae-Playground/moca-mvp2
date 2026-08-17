import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { ApiError } from '../apis/apiClient';
import { getChecklistErrorMessage } from '../apis/checklistErrorMessages';
import { checklistQueryKeys } from './checklistQueryKeys';
import { queryClient } from './queryClient';
import ChecklistEditor from '../components/ChecklistEditor';
import ConfirmDialog from '../components/ConfirmDialog';
import TopNavigation from '../components/ui/TopNavigation';
import { checklistStageMeta } from '../constants/checklist';
import { useRemoveChecklist, useUpdateChecklist } from '../hooks/query/useChecklistMutations';
import { useChecklistDetail } from '../hooks/query/useChecklists';
import { checklistItemToEditorItem } from '../types/ChecklistEditor';
import type { PublicConfig } from '../types/PublicConfig';
import { toUpdateChecklistItems } from '../utils/checklistEditor';
import { parsePositiveId } from '../utils/propertyFormat';
import styles from './ChecklistDetailPage.module.css';

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
        <Link to="/checklists">내 체크리스트로 돌아가기</Link>
      </div>
    </div>
  </main>
);

const ResolvedChecklistDetail = ({ config, checklistId }: { config: PublicConfig; checklistId: number }) => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
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
            <Link to="/checklists">내 체크리스트로 돌아가기</Link>
          </div>
        </div>
      </main>
    );
  }

  const checklist = detail.data;
  const isAddingItems = searchParams.get('mode') === 'add-items';
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
    <main className={`property-page checklist-page checklist-editor-page ${styles.page}`}>
      <div className={`page-container page-container--form ${styles.container}`}>
        <TopNavigation
          className={styles.topNavigation}
          title={isAddingItems ? '체크 항목 편집' : checklistStageMeta[checklist.stage].label}
          backLabel={isAddingItems ? '체크리스트 편집으로 돌아가기' : '체크리스트 목록으로 돌아가기'}
          {...(isAddingItems
            ? { onBack: () => setSearchParams({}, { replace: true }) }
            : { backTo: `/checklists/${checklist.stage}` })}
          {...(!isAddingItems && {
            endSlot: (
              <button
                ref={deleteRef}
                type="button"
                className="top-navigation-danger-action"
                aria-label="체크리스트 삭제"
                onClick={() => setIsDeleteOpen(true)}
              >
                삭제
              </button>
            ),
          })}
        />
        <h1 className="sr-only">{checklist.name}</h1>
        <ChecklistEditor
          key={checklist.checklistId}
          config={config}
          stage={checklist.stage}
          initialName={checklist.name}
          initialItems={checklist.items.map(checklistItemToEditorItem)}
          submitLabel="변경 내용 저장"
          isSubmitting={update.isPending}
          serverError={update.isError ? getChecklistErrorMessage(update.error) : undefined}
          viewMode={isAddingItems ? 'ADD_ITEMS' : 'EDIT'}
          onViewModeChange={(mode) =>
            setSearchParams(mode === 'ADD_ITEMS' ? { mode: 'add-items' } : {}, { replace: mode === 'EDIT' })
          }
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
