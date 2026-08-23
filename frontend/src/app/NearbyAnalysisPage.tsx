import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { fetchNearby } from '../apis/mapApi';
import MapCanvas from '../components/MapCanvas';
import { ButtonLink } from '../components/ui/Button';
import EmptyState from '../components/ui/EmptyState';
import InlineNotice from '../components/ui/InlineNotice';
import TopNavigation from '../components/ui/TopNavigation';
import { usePropertyDetail } from '../hooks/query/useProperties';
import type { MapCategory } from '../types/Map';
import type { PublicConfig } from '../types/PublicConfig';
import { parsePositiveId } from '../utils/propertyFormat';
import styles from './MapPage.module.css';

const categoryOptions: Array<{ value: MapCategory; label: string }> = [
  { value: 'HOSPITAL', label: '병원' },
  { value: 'TRANSPORT', label: '교통' },
  { value: 'SCHOOL', label: '학교' },
  { value: 'CONVENIENCE', label: '편의점' },
  { value: 'AGENCY', label: '중개사' },
];

const NearbyAnalysisPage = ({ config }: { config: PublicConfig }) => {
  const propertyId = parsePositiveId(useParams().propertyId);
  if (propertyId === null)
    return <EmptyState title="올바른 매물 주소가 아니에요" description="매물 목록에서 다시 선택해 주세요." />;
  return <ResolvedNearbyAnalysisPage config={config} propertyId={propertyId} />;
};

const ResolvedNearbyAnalysisPage = ({ config, propertyId }: { config: PublicConfig; propertyId: number }) => {
  const property = usePropertyDetail(config, propertyId);
  const latitude = property.data?.location.latitude ?? null;
  const longitude = property.data?.location.longitude ?? null;
  const [radius, setRadius] = useState<500 | 1000 | 2000>(2000);
  const [activeCategory, setActiveCategory] = useState<'ALL' | MapCategory>('ALL');
  const categories = categoryOptions.map((option) => option.value);
  const nearby = useQuery({
    queryKey: ['nearby', propertyId, latitude, longitude, radius, categories.join(',')],
    queryFn: ({ signal }) => fetchNearby(config, latitude ?? 0, longitude ?? 0, radius, categories, signal),
    enabled: latitude !== null && longitude !== null,
  });

  return (
    <main className={styles.page}>
      <TopNavigation title="주변 시설 분석" backTo="/map" backLabel="매물 지도로 돌아가기" />
      <div className={styles.nearbyContent}>
        {property.isPending ? (
          <p role="status" className={styles.helper}>
            매물 위치를 확인하는 중이에요.
          </p>
        ) : property.isError ? (
          <InlineNotice tone="error">매물 위치를 불러오지 못했어요.</InlineNotice>
        ) : latitude === null || longitude === null ? (
          <EmptyState
            title="먼저 매물 위치를 등록해 주세요"
            description="위치를 저장하면 반경별 주변 시설을 분석할 수 있어요."
            action={<ButtonLink to={`/properties/${propertyId}/edit`}>위치 등록하기</ButtonLink>}
          />
        ) : (
          <>
            <header className={styles.nearbyHeading}>
              <span>반경 {radius === 500 ? '500m' : `${radius / 1000}km`}</span>
              <h1>{property.data.name} 주변은 어때요?</h1>
              <p>{property.data.location.address}</p>
            </header>
            {nearby.isPending ? (
              <p role="status" className={styles.helper}>
                주변 시설을 분석하는 중이에요.
              </p>
            ) : nearby.isError ? (
              <div className={styles.state}>
                <InlineNotice tone="error">주변 시설을 불러오지 못했어요.</InlineNotice>
                <button type="button" onClick={() => void nearby.refetch()}>
                  다시 시도
                </button>
              </div>
            ) : (
              <>
                <div className={styles.radiusFilters} aria-label="분석 반경">
                  {([500, 1000, 2000] as const).map((value) => (
                    <button key={value} type="button" aria-pressed={radius === value} onClick={() => setRadius(value)}>
                      {value === 500 ? '500m' : `${value / 1000}km`}
                    </button>
                  ))}
                </div>
                <ul className={styles.countGrid} aria-label="주변 시설 집계와 필터">
                  <li>
                    <button
                      type="button"
                      aria-pressed={activeCategory === 'ALL'}
                      onClick={() => {
                        setActiveCategory('ALL');
                        setRadius(2000);
                      }}
                    >
                      <span>전체</span>
                      <strong>{nearby.data.places.length}</strong>
                    </button>
                  </li>
                  {categoryOptions.map((category) => (
                    <li key={category.value}>
                      <button
                        type="button"
                        aria-pressed={activeCategory === category.value}
                        onClick={() => setActiveCategory(category.value)}
                      >
                        <span>{category.label}</span>
                        <strong>{nearby.data.counts[category.value]}</strong>
                      </button>
                    </li>
                  ))}
                </ul>
                <MapCanvas
                  config={config}
                  center={nearby.data.center}
                  markers={nearby.data.places
                    .filter((place) => activeCategory === 'ALL' || place.category === activeCategory)
                    .map((place) => ({
                      id: place.providerPlaceId,
                      latitude: place.latitude,
                      longitude: place.longitude,
                      label: place.name,
                      tone: 'place',
                    }))}
                />
                {nearby.data.places.filter((place) => activeCategory === 'ALL' || place.category === activeCategory)
                  .length === 0 ? (
                  <EmptyState
                    title="이 반경에는 표시할 시설이 없어요"
                    description="다른 반경에서 다시 확인해 보세요."
                  />
                ) : (
                  <ul className={styles.placeList} aria-label="주변 시설 목록">
                    {nearby.data.places
                      .filter((place) => activeCategory === 'ALL' || place.category === activeCategory)
                      .map((place) => (
                        <li key={place.providerPlaceId}>
                          <span>{categoryOptions.find((item) => item.value === place.category)?.label}</span>
                          <strong>{place.name}</strong>
                          <small>
                            {place.distanceMeters}m · {place.address}
                          </small>
                        </li>
                      ))}
                  </ul>
                )}
              </>
            )}
          </>
        )}
        <Link className={styles.sourceNote} to="/me">
          지도 데이터 모드와 이용 안내
        </Link>
      </div>
    </main>
  );
};

export default NearbyAnalysisPage;
