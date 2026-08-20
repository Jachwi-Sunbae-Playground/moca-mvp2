import { useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { ApiError } from '../apis/apiClient';
import { getChecklistErrorMessage } from '../apis/checklistErrorMessages';
import ChecklistStartOptions from '../components/ChecklistStartOptions';
import type { ChecklistStartMode } from '../components/ChecklistStartOptions';
import ChecklistStageTabs from '../components/ChecklistStageTabs';
import BottomActionArea from '../components/ui/BottomActionArea';
import { Button } from '../components/ui/Button';
import TopNavigation from '../components/ui/TopNavigation';
import { isChecklistStage } from '../constants/checklist';
import { useAssignActiveChecklist } from '../hooks/query/useChecklistMutations';
import { useChecklistList } from '../hooks/query/useChecklists';
import { usePropertyChecklistOverview, usePropertyDetail } from '../hooks/query/useProperties';
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
  const overview = usePropertyChecklistOverview(config, propertyId);
  const list = useChecklistList(config, stage);
  const assign = useAssignActiveChecklist(config, propertyId, stage);
  const newlyCreatedId = readNewChecklistId(location.state);
  const overviewStage = overview.data?.stages.find((item) => item.stage === stage);
  const legacyCurrent = property.data?.activeChecklists.find((item) => item.stage === stage) ?? null;
  const current =
    overviewStage?.applied === true && overviewStage.sourceChecklistId !== null && overviewStage.checklistName !== null
      ? {
          checklistId: overviewStage.sourceChecklistId,
          name: overviewStage.checklistName,
        }
      : legacyCurrent;
  const [selectedId, setSelectedId] = useState<number | null>(newlyCreatedId);
  const items = useMemo(() => list.data?.pages.flatMap((page) => page.content) ?? [], [list.data]);

  useEffect(() => {
    if (selectedId === null && current !== null) setSelectedId(current.checklistId);
  }, [current, selectedId]);

  if (property.isPending || overview.isPending || list.isPending)
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

  if (property.isError || overview.isError || list.isError) {
    const error = property.error ?? overview.error ?? list.error;
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
                  void overview.refetch();
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
  const createPath = (startMode?: ChecklistStartMode) => {
    const query = new URLSearchParams({ stage, returnTo: returnPath });
    if (startMode !== undefined) query.set('start', startMode);
    return `/checklists/new?${query.toString()}`;
  };
  const selectionChanged = selectedId !== null && selectedId !== current?.checklistId;

  const saveSelection = async () => {
    if (selectedId === null) return;
    try {
      const applied = await assign.mutateAsync(selectedId);
      navigate(`/properties/${propertyId}/checklists/${applied.propertyChecklistId}`, { replace: true });
    } catch {
      /* Keep the selected checklist visible for retry. */
    }
  };

  const toggleSelection = (checklistId: number) => {
    if (current?.checklistId === checklistId && typeof overviewStage?.propertyChecklistId === 'number') {
      navigate(`/properties/${propertyId}/checklists/${overviewStage.propertyChecklistId}`);
      return;
    }
    setSelectedId(checklistId);
  };

  return (
    <main className={`${styles.page} property-page checklist-page active-checklist-page`}>
      <div className="page-container checklist-page__narrow">
        <TopNavigation title="내 체크리스트" backTo={`/properties/${propertyId}`} backLabel="매물 상세로 돌아가기" />
        <h1 className="sr-only">{property.data.name} 체크리스트 연결</h1>
        <ChecklistStageTabs
          stage={stage}
          fullBleed
          getTo={(nextStage) => `/properties/${propertyId}/active-checklists/${nextStage}`}
        />

        <p className={styles.description}>이 단계에서 사용할 체크리스트를 선택해요.</p>

        {items.length === 0 ? (
          <section className={styles.startOptions} aria-label="새 체크리스트 시작 방식">
            <p>바로 사용할 구성을 선택해 체크리스트를 만들어 주세요.</p>
            <ChecklistStartOptions onSelect={(mode) => navigate(createPath(mode))} />
          </section>
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
                  disabled={assign.isPending}
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

        <Link className={styles.createCard} to={createPath()}>
          <span aria-hidden="true">+</span> 새 체크리스트 만들기
        </Link>

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
        {selectedId !== null && selectionChanged && (
          <div className={styles.bottomAction}>
            <BottomActionArea divider={false}>
              <Button
                variant="soft"
                fullWidth
                isLoading={assign.isPending}
                loadingLabel="연결 중…"
                type="button"
                disabled={assign.isPending}
                onClick={() => void saveSelection()}
              >
                {current === null ? '이 체크리스트 연결' : '선택한 체크리스트로 교체'}
              </Button>
            </BottomActionArea>
          </div>
        )}
      </div>
    </main>
  );
};

export default PropertyActiveChecklistPage;
