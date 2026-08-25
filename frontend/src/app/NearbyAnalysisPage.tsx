import { useQuery } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import { fetchNearby } from '../apis/mapApi';
import MapCanvas from '../components/MapCanvas';
import type { MapMarker, MapRadiusCircle } from '../components/MapCanvas';
import MapCategoryRail from '../components/MapCategoryRail';
import MapNearbySheet from '../components/MapNearbySheet';
import MapPlaceDetailCard from '../components/MapPlaceDetailCard';
import { clusterNearbyPlaces } from '../components/mapClustering';
import { ALL_MAP_CATEGORIES } from '../components/mapPresentation';
import { ButtonLink } from '../components/ui/Button';
import EmptyState from '../components/ui/EmptyState';
import InlineNotice from '../components/ui/InlineNotice';
import TopNavigation from '../components/ui/TopNavigation';
import { usePropertyDetail } from '../hooks/query/useProperties';
import type { MapCategory } from '../types/Map';
import type { PublicConfig } from '../types/PublicConfig';
import { coordinatesAreClose, SEOUL_MAP_CENTER } from '../utils/mapLocation';
import { parsePositiveId } from '../utils/propertyFormat';
import styles from './MapPage.module.css';

const toggleCategory = (categories: MapCategory[], category: MapCategory): MapCategory[] =>
  categories.includes(category) ? categories.filter((item) => item !== category) : [...categories, category];

const radiusLabel = (radius: 500 | 1000 | 2000): string => (radius === 500 ? '500m' : `${radius / 1000}km`);
const levelForRadius = (radius: 500 | 1000 | 2000): number => (radius === 500 ? 4 : radius === 1000 ? 5 : 6);

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
  const [viewportCenter, setViewportCenter] = useState(center);
  const [radius, setRadius] = useState<500 | 1000 | 2000>(2000);
  const [mapLevel, setMapLevel] = useState(6);
  const [selectedCategories, setSelectedCategories] = useState<MapCategory[]>([]);
  const [listExpanded, setListExpanded] = useState(false);
  const [allMode, setAllMode] = useState(false);
  const [selectedPlaceId, setSelectedPlaceId] = useState<string | null>(null);
  const nearby = useQuery({
    queryKey: ['nearby', propertyId, latitude, longitude, radius, ALL_MAP_CATEGORIES.join(',')],
    queryFn: ({ signal }) => fetchNearby(config, latitude ?? 0, longitude ?? 0, radius, ALL_MAP_CATEGORIES, signal),
    enabled: latitude !== null && longitude !== null,
  });

  useEffect(() => {
    setViewportCenter(center);
  }, [center.latitude, center.longitude]);

  const filteredPlaces = useMemo(
    () => nearby.data?.places.filter((place) => selectedCategories.includes(place.category)) ?? [],
    [nearby.data?.places, selectedCategories],
  );
  const facilityMarkers = useMemo(() => clusterNearbyPlaces(filteredPlaces, mapLevel), [filteredPlaces, mapLevel]);
  const selectedPlace = useMemo(
    () => filteredPlaces.find((place) => place.providerPlaceId === selectedPlaceId) ?? null,
    [filteredPlaces, selectedPlaceId],
  );

  useEffect(() => {
    if (selectedPlaceId !== null && selectedPlace === null) setSelectedPlaceId(null);
  }, [selectedPlace, selectedPlaceId]);
  const markers = useMemo<MapMarker[]>(
    () => [
      {
        id: `selected-property-${propertyId}`,
        ...center,
        label: '선택한 매물',
        tone: 'selected',
      },
      ...facilityMarkers,
    ],
    [center, facilityMarkers, propertyId],
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
            center={viewportCenter}
            markers={markers}
            circles={circles}
            radiusCenter={center}
            level={mapLevel}
            showRadiusLabels
            selectedMarkerId={selectedPlace === null ? null : `place-${selectedPlace.providerPlaceId}`}
            onSelectMarker={(marker) => {
              if (marker.tone === 'cluster') {
                setViewportCenter({ latitude: marker.latitude, longitude: marker.longitude });
                setMapLevel((current) => Math.max(3, current - 1));
                setSelectedPlaceId(null);
                return;
              }
              if (marker.placeId !== undefined) {
                setSelectedPlaceId(marker.placeId);
                setListExpanded(false);
              }
            }}
            onCenterChange={(nextLatitude, nextLongitude) => {
              const nextCenter = { latitude: nextLatitude, longitude: nextLongitude };
              setViewportCenter((current) => (coordinatesAreClose(current, nextCenter) ? current : nextCenter));
            }}
            onLevelChange={setMapLevel}
          />

          <div className={styles.radiusFilters} aria-label="분석 반경">
            <button
              type="button"
              aria-pressed={allMode}
              onClick={() => {
                setSelectedPlaceId(null);
                setRadius(2000);
                setMapLevel(levelForRadius(2000));
                setViewportCenter(center);
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
                  setSelectedPlaceId(null);
                  setRadius(value);
                  setMapLevel(levelForRadius(value));
                  setViewportCenter(center);
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

          {selectedPlace !== null && (
            <MapPlaceDetailCard place={selectedPlace} onClose={() => setSelectedPlaceId(null)} />
          )}

          {!property.isPending && !nearby.isPending && !nearby.isError && nearby.data !== undefined && (
            <MapNearbySheet
              heading={`${property.data?.name ?? '선택한'} 매물 주변 ${radiusLabel(radius)}`}
              counts={nearby.data.counts}
              selectedCategories={selectedCategories}
              places={filteredPlaces}
              expanded={listExpanded}
              onToggleExpanded={() => {
                setListExpanded((current) => !current);
                if (!listExpanded) setSelectedPlaceId(null);
              }}
              onToggleCategory={(category) => {
                setSelectedCategories((current) => toggleCategory(current, category));
                setAllMode(false);
              }}
            />
          )}
        </section>
      )}
    </main>
  );
};

export default NearbyAnalysisPage;
