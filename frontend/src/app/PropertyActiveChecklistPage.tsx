import { useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { ApiError } from '../apis/apiClient';
import { getChecklistErrorMessage } from '../apis/checklistErrorMessages';
import ChecklistStageTabs from '../components/ChecklistStageTabs';
import PageHeading from '../components/PageHeading';
import { checklistStageMeta, isChecklistStage } from '../constants/checklist';
import { useAssignActiveChecklist, useRemoveActiveChecklist } from '../hooks/query/useChecklistMutations';
import { useChecklistList } from '../hooks/query/useChecklists';
import { usePropertyDetail } from '../hooks/query/useProperties';
import type { ChecklistStage } from '../types/Checklist';
import type { PublicConfig } from '../types/PublicConfig';
import { parsePositiveId } from '../utils/propertyFormat';

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
  const newlyCreatedId = readNewChecklistId(location.state);
  const current = property.data?.activeChecklists.find((item) => item.stage === stage) ?? null;
  const [selectedId, setSelectedId] = useState<number | null>(newlyCreatedId);
  const items = useMemo(() => list.data?.pages.flatMap((page) => page.content) ?? [], [list.data]);

  useEffect(() => {
    if (selectedId === null && current !== null) setSelectedId(current.checklistId);
  }, [current, selectedId]);

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
      navigate(`/properties/${propertyId}`, { replace: true, state: { focusHeading: true } });
    } catch {
      /* Keep the selected checklist visible for retry. */
    }
  };

  const disconnect = async () => {
    try {
      await unassign.mutateAsync();
      navigate(`/properties/${propertyId}`, { replace: true, state: { focusHeading: true } });
    } catch {
      /* Keep the current connection visible for retry. */
    }
  };

  return (
    <main className="property-page checklist-page">
      <div className="page-container checklist-page__narrow">
        <PageHeading
          title={`${property.data.name} 체크리스트`}
          description={`${checklistStageMeta[stage].label} 단계에서 사용할 목록을 선택해요.`}
          backTo={`/properties/${propertyId}`}
          backLabel="매물 상세"
        />
        <ChecklistStageTabs
          stage={stage}
          getTo={(nextStage) => `/properties/${propertyId}/active-checklists/${nextStage}`}
        />

        {current !== null && (
          <section className="current-connection" aria-labelledby="current-connection-heading">
            <p className="section-eyebrow">현재 연결</p>
            <h2 id="current-connection-heading">{current.name}</h2>
            <p>{current.itemCount}개 항목 · 원본 변경이 이 매물에도 바로 반영돼요.</p>
            <button
              type="button"
              className="text-danger-button"
              disabled={unassign.isPending}
              onClick={() => void disconnect()}
            >
              {unassign.isPending ? '연결 해제 중…' : '이 단계 연결 해제'}
            </button>
            {unassign.isError && (
              <p className="form-error" role="alert">
                {getChecklistErrorMessage(unassign.error)} 기존 연결은 유지됩니다.
              </p>
            )}
          </section>
        )}

        <div className="section-heading-row active-selection-heading">
          <div>
            <p className="section-eyebrow">내 체크리스트</p>
            <h2>연결할 목록 선택</h2>
          </div>
          <Link className="secondary-link" to={createPath}>
            새로 만들기
          </Link>
        </div>

        {items.length === 0 ? (
          <div className="content-state">
            <strong>선택할 체크리스트가 없어요.</strong>
            <Link to={createPath}>이 단계 체크리스트 만들기</Link>
          </div>
        ) : (
          <fieldset className="active-checklist-options">
            <legend className="sr-only">연결할 체크리스트</legend>
            {items.map((item) => (
              <label key={item.checklistId} className={selectedId === item.checklistId ? 'is-selected' : undefined}>
                <input
                  type="radio"
                  name="active-checklist"
                  value={item.checklistId}
                  checked={selectedId === item.checklistId}
                  onChange={() => setSelectedId(item.checklistId)}
                />
                <span>
                  <strong>{item.name}</strong>
                  <small>
                    {item.itemCount}개 항목 · 매물 {item.assignedPropertyCount}곳에서 사용
                  </small>
                </span>
                {current?.checklistId === item.checklistId && <em>현재 연결</em>}
                {newlyCreatedId === item.checklistId && <em>방금 생성</em>}
              </label>
            ))}
          </fieldset>
        )}

        {list.hasNextPage && (
          <div className="load-more">
            {list.isFetchNextPageError && (
              <p role="alert">추가 목록을 불러오지 못했어요. 기존 선택지는 그대로 유지됩니다.</p>
            )}
            <button
              className="secondary-button"
              type="button"
              disabled={list.isFetchingNextPage}
              onClick={() => void list.fetchNextPage()}
            >
              {list.isFetchingNextPage
                ? '불러오는 중…'
                : list.isFetchNextPageError
                  ? '다시 불러오기'
                  : '체크리스트 더 보기'}
            </button>
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
        <div className="sticky-form-action">
          <button
            className="primary-button"
            type="button"
            disabled={selectedId === null || !selectionChanged || assign.isPending}
            onClick={() => void saveSelection()}
          >
            {assign.isPending ? '연결 중…' : current === null ? '이 체크리스트 연결' : '선택한 체크리스트로 교체'}
          </button>
        </div>
      </div>
    </main>
  );
};

export default PropertyActiveChecklistPage;
