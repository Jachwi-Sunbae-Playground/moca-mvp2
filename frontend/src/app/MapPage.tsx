import { useQuery } from '@tanstack/react-query';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { fetchNearby } from '../apis/mapApi';
import MapAddressSearchPanel from '../components/MapAddressSearchPanel';
import MapCanvas from '../components/MapCanvas';
import type { MapMarker, MapRadiusCircle } from '../components/MapCanvas';
import MapCategoryRail from '../components/MapCategoryRail';
import MapNearbySheet from '../components/MapNearbySheet';
import MapPlaceDetailCard from '../components/MapPlaceDetailCard';
import { clusterNearbyPlaces } from '../components/mapClustering';
import { ALL_MAP_CATEGORIES } from '../components/mapPresentation';
import Icon from '../components/ui/Icon';
import TopNavigation from '../components/ui/TopNavigation';
import { usePropertyList } from '../hooks/query/useProperties';
import type { MapAddress, MapCategory } from '../types/Map';
import type { PublicConfig } from '../types/PublicConfig';
import {
  coordinatesAreClose,
  readLastMapCenter,
  requestCurrentMapLocation,
  SEOUL_MAP_CENTER,
  writeLastMapCenter,
} from '../utils/mapLocation';
import styles from './MapPage.module.css';

const toggleCategory = (categories: MapCategory[], category: MapCategory): MapCategory[] =>
  categories.includes(category) ? categories.filter((item) => item !== category) : [...categories, category];

type MapRadius = 500 | 1000 | 2000;

const radiusLabel = (radius: MapRadius): string => (radius === 500 ? '500m' : `${radius / 1000}km`);
const levelForRadius = (radius: MapRadius): number => (radius === 500 ? 4 : radius === 1000 ? 5 : 6);

const MapPage = ({ config }: { config: PublicConfig }) => {
  const navigate = useNavigate();
  const properties = usePropertyList(config);
  const items = properties.data?.pages.flatMap((page) => page.content) ?? [];
  const mapped = items.filter((item) => item.location.latitude !== null && item.location.longitude !== null);
  const [viewportCenter, setViewportCenter] = useState(() => readLastMapCenter() ?? SEOUL_MAP_CENTER);
  const [currentPosition, setCurrentPosition] = useState(viewportCenter);
  const [locationStatus, setLocationStatus] = useState<'locating' | 'ready' | 'fallback'>('locating');
  const [searchOpen, setSearchOpen] = useState(false);
  const [radius, setRadius] = useState<MapRadius>(2000);
  const [mapLevel, setMapLevel] = useState(6);
  const [allMode, setAllMode] = useState(false);
  const [selectedCategories, setSelectedCategories] = useState<MapCategory[]>([]);
  const [listExpanded, setListExpanded] = useState(false);
  const [locationLabel, setLocationLabel] = useState('현재 위치');
  const [selectedPlaceId, setSelectedPlaceId] = useState<string | null>(null);

  const nearby = useQuery({
    queryKey: ['map-explore-nearby', currentPosition.latitude.toFixed(4), currentPosition.longitude.toFixed(4), radius],
    queryFn: ({ signal }) =>
      fetchNearby(config, currentPosition.latitude, currentPosition.longitude, radius, ALL_MAP_CATEGORIES, signal),
    enabled: locationStatus !== 'locating',
  });

  const moveToCurrentLocation = useCallback(async () => {
    setLocationStatus('locating');
    try {
      const coordinate = await requestCurrentMapLocation();
      setViewportCenter(coordinate);
      setCurrentPosition(coordinate);
      writeLastMapCenter(coordinate);
      setLocationLabel('현재 위치');
      setLocationStatus('ready');
    } catch {
      const fallback = readLastMapCenter() ?? SEOUL_MAP_CENTER;
      setViewportCenter(fallback);
      setCurrentPosition(fallback);
      setLocationStatus('fallback');
      setLocationLabel('서울 중심');
      setSearchOpen(true);
    }
  }, []);

  useEffect(() => {
    void moveToCurrentLocation();
  }, [moveToCurrentLocation]);

  const filteredPlaces = useMemo(
    () => (nearby.data?.places ?? []).filter((place) => selectedCategories.includes(place.category)),
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

  const markers = useMemo<MapMarker[]>(() => {
    const propertyMarkers: MapMarker[] = mapped.map((item) => ({
      id: `property-${item.propertyId}`,
      latitude: item.location.latitude ?? SEOUL_MAP_CENTER.latitude,
      longitude: item.location.longitude ?? SEOUL_MAP_CENTER.longitude,
      label: item.name,
      tone: 'property',
      actionable: true,
    }));
    return [
      ...propertyMarkers,
      ...facilityMarkers,
      {
        id: 'current-location',
        ...currentPosition,
        label: locationStatus === 'fallback' ? '선택한 지도 위치' : '현재 위치',
        tone: 'current',
      },
    ];
  }, [currentPosition, facilityMarkers, locationStatus, mapped]);

  const circles = useMemo<MapRadiusCircle[]>(
    () =>
      ([500, 1000, 2000] as const)
        .filter((value) => value <= radius)
        .map((value) => ({ radiusMeters: value, label: radiusLabel(value) })),
    [radius],
  );

  const selectRadius = (nextRadius: MapRadius, nextAllMode = false) => {
    setSelectedPlaceId(null);
    setRadius(nextRadius);
    setMapLevel(levelForRadius(nextRadius));
    setViewportCenter(currentPosition);
    writeLastMapCenter(currentPosition);
    setAllMode(nextAllMode);
  };

  const applySearchedAddress = (address: MapAddress) => {
    const coordinate = { latitude: address.latitude, longitude: address.longitude };
    setViewportCenter(coordinate);
    setCurrentPosition(coordinate);
    setMapLevel(levelForRadius(radius));
    writeLastMapCenter(coordinate);
    setLocationLabel(address.roadAddress ?? address.jibunAddress ?? address.address ?? '선택한 위치');
    setSelectedPlaceId(null);
    setLocationStatus('ready');
  };

  return (
    <main className={`${styles.page} ${styles.explorePage}`}>
      <TopNavigation
        className={styles.mapNavigation}
        title="지도에서 위치 확인"
        backTo="/properties"
        backLabel="매물 목록으로 돌아가기"
        meta="13-1"
      />
      <section className={styles.mapStage} aria-label="매물 지도">
        <MapCanvas
          config={config}
          center={viewportCenter}
          markers={markers}
          circles={circles}
          radiusCenter={currentPosition}
          level={mapLevel}
          showRadiusLabels
          selectedMarkerId={selectedPlace === null ? 'current-location' : `place-${selectedPlace.providerPlaceId}`}
          onSelectMarker={(marker) => {
            if (marker.id.startsWith('property-')) {
              navigate(`/properties/${marker.id.slice('property-'.length)}/nearby`);
              return;
            }
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
          onCenterChange={(latitude, longitude) => {
            const coordinate = { latitude, longitude };
            setViewportCenter((current) => (coordinatesAreClose(current, coordinate) ? current : coordinate));
            writeLastMapCenter(coordinate);
          }}
          onLevelChange={setMapLevel}
        />

        <div className={styles.radiusFilters} aria-label="지도 반경">
          <button
            type="button"
            aria-pressed={allMode}
            onClick={() => {
              setSelectedCategories(ALL_MAP_CATEGORIES);
              selectRadius(2000, true);
            }}
          >
            전체
          </button>
          {([500, 1000, 2000] as const).map((value) => (
            <button
              key={value}
              type="button"
              aria-pressed={!allMode && radius === value}
              onClick={() => selectRadius(value)}
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

        <MapAddressSearchPanel
          config={config}
          isOpen={searchOpen}
          onClose={() => setSearchOpen(false)}
          onSelect={applySearchedAddress}
        />

        {locationStatus === 'locating' && (
          <p className={styles.mapNotice} role="status">
            현재 위치를 확인하는 중이에요.
          </p>
        )}
        {locationStatus === 'fallback' && !searchOpen && (
          <p className={styles.mapNotice} role="status">
            현재 위치를 확인하지 못했어요. 서울 중심을 표시합니다.
          </p>
        )}
        {properties.isError && <p className={styles.mapNotice}>매물 위치를 불러오지 못했어요.</p>}
        {nearby.isError && (
          <div className={styles.mapNotice} role="alert">
            시설 정보를 불러오지 못했어요.
            <button type="button" onClick={() => void nearby.refetch()}>
              다시 시도
            </button>
          </div>
        )}
        <div className={styles.mapControls}>
          <button type="button" aria-label="주소 검색 열기" onClick={() => setSearchOpen(true)}>
            <Icon name="search" size={20} />
          </button>
          <button
            type="button"
            aria-label="내 현재 위치로 이동"
            disabled={locationStatus === 'locating'}
            onClick={() => void moveToCurrentLocation()}
          >
            <Icon name="target" size={22} />
          </button>
        </div>

        <Link className={styles.addPropertyButton} to="/map/select-location" aria-label="지도에서 매물 추가">
          <Icon name="plus" size={28} />
        </Link>

        {selectedPlace !== null && (
          <MapPlaceDetailCard place={selectedPlace} avoidControls onClose={() => setSelectedPlaceId(null)} />
        )}

        {locationStatus !== 'locating' && !nearby.isPending && !nearby.isError && nearby.data !== undefined && (
          <MapNearbySheet
            eyebrow={locationLabel}
            heading={`${locationLabel === '현재 위치' ? '현재 위치' : '선택한 위치'} 주변 ${radiusLabel(radius)}`}
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
    </main>
  );
};

export default MapPage;
