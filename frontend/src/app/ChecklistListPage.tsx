import { Link, useParams } from 'react-router-dom';
import { getChecklistErrorMessage } from '../apis/checklistErrorMessages';
import ChecklistListCard from '../components/ChecklistListCard';
import ChecklistStageTabs from '../components/ChecklistStageTabs';
import AddItemLink from '../components/ui/AddItemLink';
import { Button } from '../components/ui/Button';
import TopNavigation from '../components/ui/TopNavigation';
import { isChecklistStage } from '../constants/checklist';
import { useChecklistList } from '../hooks/query/useChecklists';
import type { ChecklistStage } from '../types/Checklist';
import type { PublicConfig } from '../types/PublicConfig';
import styles from './ChecklistListPage.module.css';

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
        <Link to="/checklists">내 체크리스트로 돌아가기</Link>
      </div>
    </div>
  </main>
);

const ResolvedChecklistListPage = ({ config, stage }: { config: PublicConfig; stage: ChecklistStage }) => {
  const list = useChecklistList(config, stage);
  const items = list.data?.pages.flatMap((page) => page.content) ?? [];

  return (
    <main className={styles.page}>
      <div className={styles.container}>
        <div className={styles.navigationArea}>
          <TopNavigation title="체크리스트" className={styles.topNavigation} />
          <ChecklistStageTabs stage={stage} fullBleed />
          <p className={styles.description}>단계별로 내 체크리스트를 만들고 항목을 관리합니다.</p>
        </div>
        <h1 className="sr-only">체크리스트</h1>
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
        ) : items.length === 0 ? null : (
          <ul className={styles.list}>
            {items.map((item) => (
              <ChecklistListCard key={item.checklistId} config={config} checklist={item} />
            ))}
          </ul>
        )}
        {!list.isPending && !list.isError && (
          <div className={styles.createCard}>
            <AddItemLink to={`/checklists/new?stage=${stage}`}>새 체크리스트 만들기</AddItemLink>
          </div>
        )}
        {list.hasNextPage && (
          <div className="load-more">
            {list.isFetchNextPageError && (
              <p role="alert">추가 목록을 불러오지 못했어요. 기존 목록은 그대로 유지됩니다.</p>
            )}
            <Button
              variant="secondary"
              fullWidth
              isLoading={list.isFetchingNextPage}
              loadingLabel="불러오는 중…"
              onClick={() => void list.fetchNextPage()}
            >
              {list.isFetchNextPageError ? '다시 불러오기' : '체크리스트 더 보기'}
            </Button>
          </div>
        )}
      </div>
    </main>
  );
};

export default ChecklistListPage;
