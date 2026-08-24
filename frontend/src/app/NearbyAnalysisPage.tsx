import { useQuery } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { fetchNearby } from '../apis/mapApi';
import MapCanvas from '../components/MapCanvas';
import type { MapMarker, MapRadiusCircle } from '../components/MapCanvas';
import MapCategoryRail from '../components/MapCategoryRail';
import { ALL_MAP_CATEGORIES, getMapCategoryLabel } from '../components/mapPresentation';
import { ButtonLink } from '../components/ui/Button';
import EmptyState from '../components/ui/EmptyState';
import Icon from '../components/ui/Icon';
import InlineNotice from '../components/ui/InlineNotice';
import TopNavigation from '../components/ui/TopNavigation';
import { usePropertyDetail } from '../hooks/query/useProperties';
import type { MapCategory, NearbyPlace } from '../types/Map';
import type { PublicConfig } from '../types/PublicConfig';
import { SEOUL_MAP_CENTER } from '../utils/mapLocation';
import { parsePositiveId } from '../utils/propertyFormat';
import styles from './MapPage.module.css';

const toggleCategory = (categories: MapCategory[], category: MapCategory): MapCategory[] =>
  categories.includes(category) ? categories.filter((item) => item !== category) : [...categories, category];

const CATEGORY_SUMMARY_ANGLE: Record<MapCategory, number> = {
  HOSPITAL: 145,
  TRANSPORT: 215,
  SCHOOL: 90,
  CONVENIENCE: 25,
  AGENCY: 320,
};

const categorySummaryMarker = (
  category: MapCategory,
  places: NearbyPlace[],
  center: { latitude: number; longitude: number },
  radius: 500 | 1000 | 2000,
): MapMarker | null => {
  if (places.length === 0) return null;
  const angle = (CATEGORY_SUMMARY_ANGLE[category] * Math.PI) / 180;
  const distanceMeters = radius * 0.3;
  const latitude = center.latitude + (Math.sin(angle) * distanceMeters) / 111_320;
  const longitude =
    center.longitude + (Math.cos(angle) * distanceMeters) / (111_320 * Math.cos((center.latitude * Math.PI) / 180));
  return {
    id: `summary-${category}`,
    latitude,
    longitude,
    label: `${getMapCategoryLabel(category)} ${places.length}개`,
    tone: 'place',
    category,
    count: places.length,
  };
};

const radiusLabel = (radius: 500 | 1000 | 2000): string => (radius === 500 ? '500m' : `${radius / 1000}km`);

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
  const center = latitude === null || longitude === null ? SEOUL_MAP_CENTER : { latitude, longitude };
  const [radius, setRadius] = useState<500 | 1000 | 2000>(2000);
  const [selectedCategories, setSelectedCategories] = useState<MapCategory[]>(ALL_MAP_CATEGORIES);
  const [listExpanded, setListExpanded] = useState(false);
  const [allMode, setAllMode] = useState(false);
  const nearby = useQuery({
    queryKey: ['nearby', propertyId, latitude, longitude, radius, ALL_MAP_CATEGORIES.join(',')],
    queryFn: ({ signal }) => fetchNearby(config, latitude ?? 0, longitude ?? 0, radius, ALL_MAP_CATEGORIES, signal),
    enabled: latitude !== null && longitude !== null,
  });

  const filteredPlaces = useMemo(
    () => nearby.data?.places.filter((place) => selectedCategories.includes(place.category)) ?? [],
    [nearby.data?.places, selectedCategories],
  );
  const summaryMarkers = useMemo(
    () =>
      selectedCategories
        .map((category) =>
          categorySummaryMarker(
            category,
            filteredPlaces.filter((place) => place.category === category),
            center,
            radius,
          ),
        )
        .filter((marker): marker is MapMarker => marker !== null),
    [center, filteredPlaces, radius, selectedCategories],
  );
  const markers = useMemo<MapMarker[]>(
    () => [
      {
        id: `selected-property-${propertyId}`,
        ...center,
        label: '선택한 매물',
        tone: 'selected',
      },
      ...summaryMarkers,
    ],
    [center, propertyId, summaryMarkers],
  );
  const circles = useMemo<MapRadiusCircle[]>(
    () =>
      ([500, 1000, 2000] as const)
        .filter((value) => value <= radius)
        .map((value) => ({ radiusMeters: value, label: radiusLabel(value) })),
    [radius],
  );
  return (
    <main className={`${styles.page} ${styles.nearbyPage}`}>
      <TopNavigation
        className={styles.mapNavigation}
        title="매물 주변 분석"
        backTo="/map"
        backLabel="매물 지도로 돌아가기"
      />
      {property.isError ? (
        <div className={styles.fullState}>
          <InlineNotice tone="error">매물 위치를 불러오지 못했어요.</InlineNotice>
          <button type="button" onClick={() => void property.refetch()}>
            다시 시도
          </button>
        </div>
      ) : !property.isPending && (latitude === null || longitude === null) ? (
        <div className={styles.fullState}>
          <EmptyState
            title="먼저 매물 위치를 등록해 주세요"
            description="위치를 저장하면 반경별 주변 시설을 분석할 수 있어요."
            action={<ButtonLink to={`/properties/${propertyId}/edit`}>위치 등록하기</ButtonLink>}
          />
        </div>
      ) : (
        <section className={styles.mapStage} aria-label="매물 주변 분석 지도">
          <MapCanvas
            config={config}
            center={center}
            markers={markers}
            circles={circles}
            level={radius === 500 ? 4 : radius === 1000 ? 5 : 6}
            showRadiusLabels
          />

          <div className={styles.radiusFilters} aria-label="분석 반경">
            <button
              type="button"
              aria-pressed={allMode}
              onClick={() => {
                setRadius(2000);
                setSelectedCategories(ALL_MAP_CATEGORIES);
                setAllMode(true);
              }}
            >
              전체
            </button>
            {([500, 1000, 2000] as const).map((value) => (
              <button
                key={value}
                type="button"
                aria-pressed={!allMode && radius === value}
                onClick={() => {
                  setRadius(value);
                  setAllMode(false);
                }}
              >
                {radiusLabel(value)}
              </button>
            ))}
          </div>

          <MapCategoryRail
            selectedCategories={selectedCategories}
            counts={nearby.data?.counts}
            onToggle={(category) => {
              setSelectedCategories((current) => toggleCategory(current, category));
              setAllMode(false);
            }}
          />

          {property.isPending && (
            <p className={styles.mapNotice} role="status">
              매물 위치를 확인하는 중이에요.
            </p>
          )}
          {nearby.isPending && latitude !== null && longitude !== null && (
            <p className={styles.mapNotice} role="status">
              주변 시설을 분석하는 중이에요.
            </p>
          )}
          {nearby.isError && (
            <div className={styles.mapNotice} role="alert">
              주변 시설을 불러오지 못했어요.
              <button type="button" onClick={() => void nearby.refetch()}>
                다시 시도
              </button>
            </div>
          )}

          {!property.isPending && !nearby.isPending && !nearby.isError && (
            <section className={styles.nearbySheet} data-expanded={listExpanded || undefined}>
              <div className={styles.sheetHeader}>
                <div>
                  <span>{property.data?.name}</span>
                  <h1>선택한 매물 주변 {radiusLabel(radius)}</h1>
                </div>
                <button
                  type="button"
                  aria-expanded={listExpanded}
                  aria-controls="nearby-place-list"
                  onClick={() => setListExpanded((current) => !current)}
                >
                  {listExpanded ? '목록 접기' : '시설 목록 보기'}
                  <Icon name={listExpanded ? 'chevron-down' : 'chevron-up'} size={17} />
                </button>
              </div>

              <ul className={styles.summaryChips} aria-label="선택한 주변 시설 집계">
                {selectedCategories.map((category) => (
                  <li key={category} data-category={category}>
                    <span aria-hidden="true" />
                    {getMapCategoryLabel(category)} {nearby.data?.counts[category] ?? 0}개
                  </li>
                ))}
                {selectedCategories.length === 0 && <li>시설 카테고리를 선택해 주세요.</li>}
              </ul>

              {listExpanded && (
                <div className={styles.expandedPlaces} id="nearby-place-list">
                  {filteredPlaces.length === 0 ? (
                    <p>이 반경에는 선택한 시설이 없어요.</p>
                  ) : (
                    <ul aria-label="주변 시설 목록">
                      {filteredPlaces.map((place) => (
                        <li key={place.providerPlaceId}>
                          <span data-category={place.category}>{getMapCategoryLabel(place.category)}</span>
                          <strong>{place.name}</strong>
                          <small>
                            {place.distanceMeters}m · {place.address}
                          </small>
                        </li>
                      ))}
                    </ul>
                  )}
                  <Link to="/me">지도 데이터 모드와 이용 안내</Link>
                </div>
              )}
            </section>
          )}
        </section>
      )}
    </main>
  );
};

export default NearbyAnalysisPage;
