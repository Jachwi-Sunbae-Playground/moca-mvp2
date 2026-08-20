import { Link, useParams } from 'react-router-dom';
import { ApiError } from '../apis/apiClient';
import { getPropertyErrorMessage } from '../apis/propertyErrorMessages';
import { getVisitErrorMessage } from '../apis/visitErrorMessages';
import PageHeading from '../components/PageHeading';
import VisitAggregateSummary from '../components/VisitAggregateSummary';
import { usePropertyDetail } from '../hooks/query/useProperties';
import { usePropertyVisits } from '../hooks/query/useVisits';
import type { PublicConfig } from '../types/PublicConfig';
import { formatDateTime, getVisitStatusLabel, parsePositiveId } from '../utils/propertyFormat';
import './PropertyVisitsPage.css';

const PropertyVisitsPage = ({ config }: { config: PublicConfig }) => {
  const propertyId = parsePositiveId(useParams().propertyId);
  if (propertyId === null) return <InvalidVisitsState />;
  return <ResolvedPropertyVisitsPage config={config} propertyId={propertyId} />;
};

const InvalidVisitsState = () => (
  <main className="property-page">
    <div className="page-container content-state">
      <strong>올바른 매물 방문 주소가 아니에요.</strong>
      <Link to="/properties">매물 목록으로 돌아가기</Link>
    </div>
  </main>
);

const ResolvedPropertyVisitsPage = ({ config, propertyId }: { config: PublicConfig; propertyId: number }) => {
  const property = usePropertyDetail(config, propertyId);
  const visits = usePropertyVisits(config, propertyId);
  const items = visits.data?.pages.flatMap((page) => page.content) ?? [];

  const isNotFound =
    (property.error instanceof ApiError && property.error.code === 'PROPERTY_NOT_FOUND') ||
    (visits.error instanceof ApiError && visits.error.code === 'PROPERTY_NOT_FOUND');

  if ((property.isPending || visits.isPending) && items.length === 0) {
    return (
      <main className="property-page">
        <div className="page-container content-state" role="status">
          <span className="spinner" />
          방문 기록을 불러오는 중이에요.
        </div>
      </main>
    );
  }

  if (isNotFound) {
    return (
      <main className="property-page">
        <div className="page-container content-state content-state--error" role="alert">
          <strong>매물을 찾을 수 없어요.</strong>
          <Link to="/properties">매물 목록으로 돌아가기</Link>
        </div>
      </main>
    );
  }

  return (
    <main className="property-page visit-list-page">
      <div className="page-container">
        <PageHeading
          title={`${property.data?.name ?? '매물'} 방문 기록`}
          description="방문할 때마다 당시 체크리스트와 확인 결과를 따로 보관해요."
          backTo={`/properties/${propertyId}`}
          backLabel="매물 상세"
        />
        {property.isError && (
          <p className="form-error" role="alert">
            매물 이름을 불러오지 못했어요. {getPropertyErrorMessage(property.error)}
          </p>
        )}
        {visits.isError && !visits.isFetchNextPageError && (
          <div className="content-state content-state--error" role="alert">
            <strong>방문 기록을 불러오지 못했어요.</strong>
            <span>{getVisitErrorMessage(visits.error)}</span>
            <button className="inline-button" type="button" onClick={() => void visits.refetch()}>
              다시 시도
            </button>
          </div>
        )}
        {visits.isSuccess && items.length === 0 && (
          <div className="content-state">
            <strong>아직 방문 기록이 없어요.</strong>
            <span>매물 상세에서 활성 체크리스트를 확인하고 첫 방문을 시작해 보세요.</span>
            <Link to={`/properties/${propertyId}`}>매물 상세로 이동</Link>
          </div>
        )}
        {items.length > 0 && (
          <ol className="visit-record-list" aria-label="방문 기록">
            {items.map((visit) => (
              <li key={visit.visitId}>
                <Link to={`/visits/${visit.visitId}`}>
                  <div className="visit-record-list__heading">
                    <strong>방문 #{visit.visitId}</strong>
                    <span data-status={visit.status}>{getVisitStatusLabel(visit.status)}</span>
                  </div>
                  <time dateTime={visit.startedAt}>시작 {formatDateTime(visit.startedAt)}</time>
                  {visit.completedAt !== null && (
                    <time dateTime={visit.completedAt}>완료 {formatDateTime(visit.completedAt)}</time>
                  )}
                  <VisitAggregateSummary summary={visit.summary} label={`방문 #${visit.visitId} 결과`} />
                </Link>
              </li>
            ))}
          </ol>
        )}
        {visits.hasNextPage && (
          <div className="load-more">
            {visits.isFetchNextPageError && <p role="alert">다음 방문을 불러오지 못했어요. 기존 기록은 유지됩니다.</p>}
            <button
              className="secondary-button"
              type="button"
              disabled={visits.isFetchingNextPage}
              onClick={() => void visits.fetchNextPage()}
            >
              {visits.isFetchingNextPage
                ? '추가 방문 불러오는 중…'
                : visits.isFetchNextPageError
                  ? '다시 불러오기'
                  : '방문 더 보기'}
            </button>
          </div>
        )}
      </div>
    </main>
  );
};

export default PropertyVisitsPage;
