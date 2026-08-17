import { useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { ApiError } from '../apis/apiClient';
import { getChecklistErrorMessage } from '../apis/checklistErrorMessages';
import ChecklistStageTabs from '../components/ChecklistStageTabs';
import BottomActionArea from '../components/ui/BottomActionArea';
import { Button, ButtonLink } from '../components/ui/Button';
import TopNavigation from '../components/ui/TopNavigation';
import { isChecklistStage } from '../constants/checklist';
import { useAssignActiveChecklist, useRemoveActiveChecklist } from '../hooks/query/useChecklistMutations';
import { useChecklistList } from '../hooks/query/useChecklists';
import { usePropertyDetail } from '../hooks/query/useProperties';
import { useStartPropertyVisit } from '../hooks/query/useVisitMutations';
import type { ChecklistStage } from '../types/Checklist';
import type { PublicConfig } from '../types/PublicConfig';
import { parsePositiveId } from '../utils/propertyFormat';
import styles from './PropertyActiveChecklistPage.module.css';

const readNewChecklistId = (state: unknown): number | null => {
  if (typeof state !== 'object' || state === null || !('newChecklistId' in state)) return null;
  const value = state.newChecklistId;
  return typeof value === 'number' && Number.isSafeInteger(value) && value > 0 ? value : null;
};

const PropertyActiveChecklistPage = ({ config }: { config: PublicConfig }) => {
  const params = useParams();
  const propertyId = parsePositiveId(params.propertyId);
  if (propertyId === null || !isChecklistStage(params.stage)) return <InvalidActiveChecklist />;
  return <ResolvedPropertyActiveChecklist config={config} propertyId={propertyId} stage={params.stage} />;
};

const InvalidActiveChecklist = () => (
  <main className="property-page">
    <div className="page-container">
      <div className="content-state">
        <strong>올바른 매물 체크리스트 주소가 아니에요.</strong>
        <Link to="/properties">매물 목록으로 돌아가기</Link>
      </div>
    </div>
  </main>
);

const ResolvedPropertyActiveChecklist = ({
  config,
  propertyId,
  stage,
}: {
  config: PublicConfig;
  propertyId: number;
  stage: ChecklistStage;
}) => {
  const location = useLocation();
  const navigate = useNavigate();
  const property = usePropertyDetail(config, propertyId);
  const list = useChecklistList(config, stage);
  const assign = useAssignActiveChecklist(config, propertyId, stage);
  const unassign = useRemoveActiveChecklist(config, propertyId, stage);
  const startVisit = useStartPropertyVisit(config, propertyId);
  const newlyCreatedId = readNewChecklistId(location.state);
  const current = property.data?.activeChecklists.find((item) => item.stage === stage) ?? null;
  const [selectedId, setSelectedId] = useState<number | null>(newlyCreatedId);
  const [disconnectingId, setDisconnectingId] = useState<number | null>(null);
  const items = useMemo(() => list.data?.pages.flatMap((page) => page.content) ?? [], [list.data]);

  useEffect(() => {
    if (disconnectingId !== null) {
      if (current?.checklistId !== disconnectingId) setDisconnectingId(null);
      return;
    }
    if (selectedId === null && current !== null) setSelectedId(current.checklistId);
  }, [current, disconnectingId, selectedId]);

  if (property.isPending || list.isPending)
    return (
      <main className="property-page">
        <div className="page-container">
          <div className="content-state" role="status">
            <span className="spinner" />
            연결 정보를 불러오는 중이에요.
          </div>
        </div>
      </main>
    );

  if (property.isError || list.isError) {
    const error = property.error ?? list.error;
    const propertyNotFound = error instanceof ApiError && error.code === 'PROPERTY_NOT_FOUND';
    return (
      <main className="property-page">
        <div className="page-container">
          <div className="content-state content-state--error" role="alert">
            <strong>{propertyNotFound ? '매물을 찾을 수 없어요.' : '연결 정보를 불러오지 못했어요.'}</strong>
            <span>{getChecklistErrorMessage(error)}</span>
            {!propertyNotFound && (
              <button
                type="button"
                className="inline-button"
                onClick={() => {
                  void property.refetch();
                  void list.refetch();
                }}
              >
                다시 시도
              </button>
            )}
            <Link to="/properties">매물 목록으로 돌아가기</Link>
          </div>
        </div>
      </main>
    );
  }

  const returnPath = `/properties/${propertyId}/active-checklists/${stage}`;
  const createPath = `/checklists/new?stage=${stage}&returnTo=${encodeURIComponent(returnPath)}`;
  const selectionChanged = selectedId !== null && selectedId !== current?.checklistId;

  const saveSelection = async () => {
    if (selectedId === null) return;
    try {
      await assign.mutateAsync(selectedId);
      const visit = await startVisit.mutateAsync();
      navigate(`/visits/${visit.visitId}?stage=${stage}`, { replace: true });
    } catch {
      /* Keep the selected checklist visible for retry. */
    }
  };

  const disconnect = async (checklistId: number) => {
    setDisconnectingId(checklistId);
    setSelectedId(null);
    try {
      await unassign.mutateAsync();
    } catch {
      setDisconnectingId(null);
      setSelectedId(checklistId);
    }
  };

  const toggleSelection = (checklistId: number) => {
    if (selectedId !== checklistId) {
      setSelectedId(checklistId);
      return;
    }

    if (current?.checklistId === checklistId) {
      void disconnect(checklistId);
      return;
    }

    setSelectedId(current?.checklistId ?? null);
  };

  return (
    <main className={`${styles.page} property-page checklist-page active-checklist-page`}>
      <div className="page-container checklist-page__narrow">
        <TopNavigation
          title="체크리스트 연결"
          backTo={`/properties/${propertyId}`}
          backLabel="매물 상세로 돌아가기"
          endSlot={
            <ButtonLink className={styles.createLink} variant="text" to={createPath}>
              새로 만들기
            </ButtonLink>
          }
        />
        <h1 className="sr-only">{property.data.name} 체크리스트 연결</h1>
        <ChecklistStageTabs
          stage={stage}
          fullBleed
          getTo={(nextStage) => `/properties/${propertyId}/active-checklists/${nextStage}`}
        />

        <div className={`section-heading-row active-selection-heading ${styles.selectionHeading}`}>
          <h2>연결할 목록 선택</h2>
        </div>

        {items.length === 0 ? (
          <div className="content-state">
            <strong>선택할 체크리스트가 없어요.</strong>
            <ButtonLink variant="soft" to={createPath}>
              이 단계 체크리스트 만들기
            </ButtonLink>
          </div>
        ) : (
          <fieldset className="active-checklist-options">
            <legend className="sr-only">연결할 체크리스트</legend>
            {items.map((item) => (
              <label key={item.checklistId} className={selectedId === item.checklistId ? 'is-selected' : undefined}>
                <input
                  type="checkbox"
                  name="active-checklist"
                  value={item.checklistId}
                  checked={selectedId === item.checklistId}
                  disabled={unassign.isPending || disconnectingId !== null}
                  onChange={() => toggleSelection(item.checklistId)}
                />
                <span>
                  <strong>{item.name}</strong>
                  <small>
                    {item.itemCount}개 항목 · 매물 {item.assignedPropertyCount}곳에서 사용
                  </small>
                </span>
                {newlyCreatedId === item.checklistId && <em>방금 생성</em>}
              </label>
            ))}
          </fieldset>
        )}

        {unassign.isError && (
          <p className="form-error" role="alert">
            {getChecklistErrorMessage(unassign.error)} 기존 연결은 유지됩니다.
          </p>
        )}

        {list.hasNextPage && (
          <div className={styles.loadMore}>
            {list.isFetchNextPageError && (
              <p role="alert">추가 목록을 불러오지 못했어요. 기존 선택지는 그대로 유지됩니다.</p>
            )}
            <Button
              variant="secondary"
              fullWidth
              type="button"
              disabled={list.isFetchingNextPage}
              onClick={() => void list.fetchNextPage()}
            >
              {list.isFetchingNextPage
                ? '불러오는 중…'
                : list.isFetchNextPageError
                  ? '다시 불러오기'
                  : '체크리스트 더 보기'}
            </Button>
          </div>
        )}
        {selectionChanged && current !== null && (
          <p className="form-notice">확인하면 현재 연결을 선택한 체크리스트로 교체합니다.</p>
        )}
        {assign.isError && (
          <p className="form-error" role="alert">
            {getChecklistErrorMessage(assign.error)} 기존 연결은 유지됩니다.
          </p>
        )}
        {startVisit.isError && (
          <p className="form-error" role="alert">
            체크리스트는 연결했지만 체크 화면을 열지 못했어요. 매물 상세에서 다시 시작해 주세요.
          </p>
        )}
        <div className={styles.bottomAction}>
          <BottomActionArea divider={false}>
            <Button
              variant="soft"
              fullWidth
              isLoading={assign.isPending || startVisit.isPending}
              loadingLabel={assign.isPending ? '연결 중…' : '체크 화면 여는 중…'}
              type="button"
              disabled={selectedId === null || !selectionChanged || assign.isPending || startVisit.isPending}
              onClick={() => void saveSelection()}
            >
              {current === null ? '이 체크리스트 연결' : selectionChanged ? '선택한 체크리스트로 교체' : '연결됨'}
            </Button>
          </BottomActionArea>
        </div>
      </div>
    </main>
  );
};

export default PropertyActiveChecklistPage;
