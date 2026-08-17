import { useEffect, useRef, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { getPropertyErrorMessage } from '../apis/propertyErrorMessages';
import PropertyCard from '../components/PropertyCard';
import AddItemLink from '../components/ui/AddItemLink';
import { Button, ButtonLink } from '../components/ui/Button';
import EmptyState from '../components/ui/EmptyState';
import Icon from '../components/ui/Icon';
import InlineNotice from '../components/ui/InlineNotice';
import SearchField from '../components/ui/SearchField';
import TopNavigation from '../components/ui/TopNavigation';
import { usePropertyList } from '../hooks/query/useProperties';
import type { PublicConfig } from '../types/PublicConfig';
import styles from './PropertyListPage.module.css';

type PropertyListPageProps = { config: PublicConfig };
type PropertyStatusFilter = 'ALL' | 'INCOMPLETE' | 'COMPLETED';

const propertyStatusFilters: Array<{ value: PropertyStatusFilter; label: string }> = [
  { value: 'ALL', label: '전체' },
  { value: 'INCOMPLETE', label: '미완료' },
  { value: 'COMPLETED', label: '완료' },
];

const PropertyListPage = ({ config }: PropertyListPageProps) => {
  const location = useLocation();
  const [draftQuery, setDraftQuery] = useState('');
  const [query, setQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<PropertyStatusFilter>('ALL');
  const headingRef = useRef<HTMLHeadingElement>(null);
  const properties = usePropertyList(config, query);
  const items = properties.data?.pages.flatMap((page) => page.content) ?? [];
  const filteredItems = items.filter((property) => {
    if (statusFilter === 'ALL') return true;
    const isCompleted = property.recentVisit?.status === 'COMPLETED';
    return statusFilter === 'COMPLETED' ? isCompleted : !isCompleted;
  });
  const totalElements = properties.data?.pages[0]?.totalElements ?? 0;
  const hasInitialError = properties.isError && !properties.isFetchNextPageError;
  const shouldFocusHeading = (location.state as { focusHeading?: boolean } | null)?.focusHeading === true;

  useEffect(() => {
    if (shouldFocusHeading) headingRef.current?.focus();
  }, [shouldFocusHeading]);

  const search = () => setQuery(draftQuery.trim());
  const propertySearch = (
    <div className={styles.search}>
      <SearchField
        label="매물 이름 검색"
        value={draftQuery}
        maxLength={50}
        placeholder="매물 이름으로 검색"
        showSubmitButton={false}
        onValueChange={setDraftQuery}
        onSubmit={search}
        onClear={() => setQuery('')}
      />
    </div>
  );

  return (
    <main className={styles.page}>
      <div className={styles.content}>
        <TopNavigation
          title="기록한 매물"
          endSlot={
            <ButtonLink className={styles.addButton} to="/properties/new" variant="text" aria-label="새 매물 등록">
              <Icon name="plus" size={15} /> 매물 추가
            </ButtonLink>
          }
        />
        <h1 ref={headingRef} className="sr-only" tabIndex={-1}>
          내 매물
        </h1>

        <div className={styles.searchRow}>{propertySearch}</div>

        {properties.isSuccess && items.length > 0 && (
          <div className={styles.statusFilters} aria-label="매물 진행 상태 필터">
            {propertyStatusFilters.map((filter) => (
              <button
                key={filter.value}
                type="button"
                aria-pressed={statusFilter === filter.value}
                onClick={() => setStatusFilter(filter.value)}
              >
                {filter.label}
              </button>
            ))}
          </div>
        )}

        {properties.isSuccess && (
          <div className={styles.listToolbar}>
            <span className={styles.totalCount}>
              전체 <strong>{totalElements}</strong>
            </span>
          </div>
        )}

        {properties.isPending && (
          <div className={styles.loading} role="status">
            <span className={styles.spinner} aria-hidden="true" />
            매물 목록을 불러오는 중이에요.
          </div>
        )}

        {hasInitialError && (
          <div className={styles.errorState}>
            <InlineNotice tone="error">
              <strong>매물 목록을 불러오지 못했어요.</strong>
              <span>{getPropertyErrorMessage(properties.error)}</span>
            </InlineNotice>
            <Button variant="secondary" fullWidth onClick={() => void properties.refetch()}>
              다시 시도
            </Button>
          </div>
        )}

        {properties.isSuccess && items.length === 0 && (
          <>
            <EmptyState
              title={query.length > 0 ? '검색 결과가 없어요.' : '아직 등록한 매물이 없어요.'}
              description={
                query.length > 0
                  ? '다른 이름으로 검색해 보세요.'
                  : '기본 정보부터 현장 체크까지 한 흐름으로 관리할 수 있어요.'
              }
              action={
                query.length === 0 ? (
                  <ButtonLink to="/properties/new">
                    <Icon name="plus" size={15} />첫 매물 등록하기
                  </ButtonLink>
                ) : undefined
              }
            />
            {query.length === 0 && (
              <section className={styles.guide} aria-labelledby="property-guide-heading">
                <div className={styles.sectionHeading}>
                  <h2 id="property-guide-heading">이렇게 진행해요</h2>
                  <span>4 STEPS</span>
                </div>
                <ol className={styles.steps}>
                  {['매물 등록', '정보 입력', '체크 선택', '현장 체크'].map((label, index) => (
                    <li key={label}>
                      <strong>{String(index + 1).padStart(2, '0')}</strong>
                      <span>{label}</span>
                    </li>
                  ))}
                </ol>
                <InlineNotice>입력 내용은 언제든 수정할 수 있으며 단계별로 이어서 진행할 수 있어요.</InlineNotice>
              </section>
            )}
          </>
        )}

        {!hasInitialError && filteredItems.length > 0 && (
          <section className={styles.cardList} aria-label="매물 목록">
            {filteredItems.map((property) => (
              <PropertyCard key={property.propertyId} property={property} />
            ))}
          </section>
        )}

        {properties.isSuccess && items.length > 0 && filteredItems.length === 0 && (
          <EmptyState title="해당 상태의 매물이 없어요." description="다른 상태를 선택해 보세요." />
        )}

        {!hasInitialError && properties.hasNextPage && (
          <div className={styles.loadMore}>
            {properties.isFetchNextPageError && (
              <InlineNotice tone="error">다음 매물을 불러오지 못했어요. 기존 목록은 그대로 유지됩니다.</InlineNotice>
            )}
            <Button
              variant="secondary"
              fullWidth
              isLoading={properties.isFetchingNextPage}
              loadingLabel="추가 매물 불러오는 중…"
              onClick={() => void properties.fetchNextPage()}
            >
              {properties.isFetchNextPageError ? '다시 불러오기' : '매물 더 보기'}
            </Button>
          </div>
        )}

        {!hasInitialError && items.length > 0 && (
          <div className={styles.bottomAdd}>
            <AddItemLink to="/properties/new">매물 추가</AddItemLink>
          </div>
        )}
      </div>
    </main>
  );
};

export default PropertyListPage;
