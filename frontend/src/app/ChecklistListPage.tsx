import { Link, useParams } from 'react-router-dom';
import { getChecklistErrorMessage } from '../apis/checklistErrorMessages';
import ChecklistListCard from '../components/ChecklistListCard';
import ChecklistStageTabs from '../components/ChecklistStageTabs';
import PageHeading from '../components/PageHeading';
import { checklistStageMeta, isChecklistStage } from '../constants/checklist';
import { useChecklistList } from '../hooks/query/useChecklists';
import type { ChecklistStage } from '../types/Checklist';
import type { PublicConfig } from '../types/PublicConfig';

const ChecklistListPage = ({ config }: { config: PublicConfig }) => {
  const { resource: stageParam } = useParams();
  if (!isChecklistStage(stageParam)) return <InvalidStage />;
  return <ResolvedChecklistListPage config={config} stage={stageParam} />;
};

const InvalidStage = () => (
  <main className="property-page">
    <div className="page-container">
      <div className="content-state">
        <strong>올바른 체크리스트 단계가 아니에요.</strong>
        <Link to="/checklists">체크리스트 홈으로 돌아가기</Link>
      </div>
    </div>
  </main>
);

const ResolvedChecklistListPage = ({ config, stage }: { config: PublicConfig; stage: ChecklistStage }) => {
  const list = useChecklistList(config, stage);
  const items = list.data?.pages.flatMap((page) => page.content) ?? [];

  return (
    <main className="property-page checklist-page">
      <div className="page-container checklist-page__narrow">
        <PageHeading
          title="내 체크리스트"
          description={checklistStageMeta[stage].description}
          backTo="/checklists"
          backLabel="체크리스트 홈"
        />
        <ChecklistStageTabs stage={stage} />
        <div className="page-primary-action">
          <Link className="primary-link" to={`/checklists/new?stage=${stage}`}>
            새 체크리스트
          </Link>
        </div>
        {list.isPending ? (
          <div className="content-state" role="status">
            <span className="spinner" />
            체크리스트를 불러오는 중이에요.
          </div>
        ) : list.isError ? (
          <div className="content-state content-state--error" role="alert">
            <strong>체크리스트를 불러오지 못했어요.</strong>
            <span>{getChecklistErrorMessage(list.error)}</span>
            <button className="inline-button" type="button" onClick={() => void list.refetch()}>
              다시 시도
            </button>
          </div>
        ) : items.length === 0 ? (
          <div className="content-state">
            <strong>이 단계에 만든 체크리스트가 없어요.</strong>
            <span>프리셋으로 빠르게 시작해 보세요.</span>
          </div>
        ) : (
          <ul className="checklist-list">
            {items.map((item) => (
              <ChecklistListCard key={item.checklistId} config={config} checklist={item} />
            ))}
          </ul>
        )}
        {list.hasNextPage && (
          <div className="load-more">
            {list.isFetchNextPageError && (
              <p role="alert">추가 목록을 불러오지 못했어요. 기존 목록은 그대로 유지됩니다.</p>
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
      </div>
    </main>
  );
};

export default ChecklistListPage;
