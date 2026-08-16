import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { getPropertyErrorMessage } from '../apis/propertyErrorMessages';
import PageHeading from '../components/PageHeading';
import PropertyCard from '../components/PropertyCard';
import { usePropertyList } from '../hooks/query/useProperties';
import type { PublicConfig } from '../types/PublicConfig';
import styles from './PropertyListPage.module.css';

type PropertyListPageProps = { config: PublicConfig };

const PropertyListPage = ({ config }: PropertyListPageProps) => {
  const location = useLocation();
  const [draftQuery, setDraftQuery] = useState('');
  const [query, setQuery] = useState('');
  const properties = usePropertyList(config, query);
  const items = properties.data?.pages.flatMap((page) => page.content) ?? [];

  const search = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setQuery(draftQuery.trim());
  };

  return (
    <main className="property-page">
      <div className="page-container">
        <PageHeading
          title="내 매물"
          description="직접 본 매물과 다시 확인할 내용을 한곳에 모아요."
          focusOnMount={(location.state as { focusHeading?: boolean } | null)?.focusHeading === true}
        />
        <div className="page-primary-action">
          <Link className="primary-link" to="/properties/new">
            + 새 매물 등록
          </Link>
        </div>
        <form className={styles.search} role="search" onSubmit={search}>
          <label className="sr-only" htmlFor="property-search-input">
            매물 이름 검색
          </label>
          <input
            id="property-search-input"
            value={draftQuery}
            maxLength={50}
            placeholder="매물 이름으로 검색"
            onChange={(event) => setDraftQuery(event.target.value)}
          />
          <button type="submit">검색</button>
        </form>

        {properties.isPending && (
          <div className="content-state" role="status">
            <span className="spinner" />
            매물 목록을 불러오는 중이에요.
          </div>
        )}
        {properties.isError && !properties.isFetchNextPageError && (
          <div className="content-state content-state--error" role="alert">
            <strong>매물 목록을 불러오지 못했어요.</strong>
            <span>{getPropertyErrorMessage(properties.error)}</span>
            <button className="inline-button" type="button" onClick={() => void properties.refetch()}>
              다시 시도
            </button>
          </div>
        )}
        {properties.isSuccess && items.length === 0 && (
          <div className="content-state">
            <strong>{query.length > 0 ? '검색 결과가 없어요.' : '아직 등록한 매물이 없어요.'}</strong>
            <span>
              {query.length > 0 ? '다른 이름으로 검색해 보세요.' : '첫 매물을 등록하고 방문 기록을 준비해 보세요.'}
            </span>
          </div>
        )}
        {items.length > 0 && (
          <section className={styles.cardList} aria-label="매물 목록">
            {items.map((property) => (
              <PropertyCard key={property.propertyId} property={property} />
            ))}
          </section>
        )}

        {properties.hasNextPage && (
          <div className="load-more">
            {properties.isFetchNextPageError && (
              <p role="alert">다음 매물을 불러오지 못했어요. 기존 목록은 그대로 유지됩니다.</p>
            )}
            <button
              className="secondary-button"
              type="button"
              disabled={properties.isFetchingNextPage}
              onClick={() => void properties.fetchNextPage()}
            >
              {properties.isFetchingNextPage
                ? '추가 매물 불러오는 중…'
                : properties.isFetchNextPageError
                  ? '다시 불러오기'
                  : '매물 더 보기'}
            </button>
          </div>
        )}
      </div>
    </main>
  );
};

export default PropertyListPage;
