import { useQuery } from '@tanstack/react-query';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { fetchNearby } from '../apis/mapApi';
import MapAddressSearchPanel from '../components/MapAddressSearchPanel';
import MapCanvas from '../components/MapCanvas';
import type { MapMarker } from '../components/MapCanvas';
import MapCategoryRail from '../components/MapCategoryRail';
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

const MapPage = ({ config }: { config: PublicConfig }) => {
  const navigate = useNavigate();
  const properties = usePropertyList(config);
  const items = properties.data?.pages.flatMap((page) => page.content) ?? [];
  const mapped = items.filter((item) => item.location.latitude !== null && item.location.longitude !== null);
  const [viewportCenter, setViewportCenter] = useState(() => readLastMapCenter() ?? SEOUL_MAP_CENTER);
  const [queryCenter, setQueryCenter] = useState(viewportCenter);
  const [currentPosition, setCurrentPosition] = useState(viewportCenter);
  const [locationStatus, setLocationStatus] = useState<'locating' | 'ready' | 'fallback'>('locating');
  const [searchOpen, setSearchOpen] = useState(false);
  const [selectedCategories, setSelectedCategories] = useState<MapCategory[]>(ALL_MAP_CATEGORIES);

  const nearby = useQuery({
    queryKey: [
      'map-explore-nearby',
      queryCenter.latitude.toFixed(4),
      queryCenter.longitude.toFixed(4),
      selectedCategories.slice().sort().join(','),
    ],
    queryFn: ({ signal }) =>
      fetchNearby(config, queryCenter.latitude, queryCenter.longitude, 2000, selectedCategories, signal),
    enabled: locationStatus !== 'locating' && selectedCategories.length > 0,
  });

  const moveToCurrentLocation = useCallback(async () => {
    setLocationStatus('locating');
    try {
      const coordinate = await requestCurrentMapLocation();
      setViewportCenter(coordinate);
      setQueryCenter(coordinate);
      setCurrentPosition(coordinate);
      writeLastMapCenter(coordinate);
      setLocationStatus('ready');
    } catch {
      const fallback = readLastMapCenter() ?? SEOUL_MAP_CENTER;
      setViewportCenter(fallback);
      setQueryCenter(fallback);
      setCurrentPosition(fallback);
      setLocationStatus('fallback');
      setSearchOpen(true);
    }
  }, []);

  useEffect(() => {
    void moveToCurrentLocation();
  }, [moveToCurrentLocation]);

  useEffect(() => {
    const timeout = window.setTimeout(() => setQueryCenter(viewportCenter), 450);
    return () => window.clearTimeout(timeout);
  }, [viewportCenter]);

  const markers = useMemo<MapMarker[]>(() => {
    const propertyMarkers: MapMarker[] = mapped.map((item) => ({
      id: `property-${item.propertyId}`,
      latitude: item.location.latitude ?? SEOUL_MAP_CENTER.latitude,
      longitude: item.location.longitude ?? SEOUL_MAP_CENTER.longitude,
      label: item.name,
      tone: 'property',
      actionable: true,
    }));
    const facilityMarkers: MapMarker[] =
      nearby.data?.places.map((place) => ({
        id: `place-${place.providerPlaceId}`,
        latitude: place.latitude,
        longitude: place.longitude,
        label: place.name,
        tone: 'place',
        category: place.category,
      })) ?? [];
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
  }, [currentPosition, locationStatus, mapped, nearby.data?.places]);

  const applySearchedAddress = (address: MapAddress) => {
    const coordinate = { latitude: address.latitude, longitude: address.longitude };
    setViewportCenter(coordinate);
    setQueryCenter(coordinate);
    setCurrentPosition(coordinate);
    writeLastMapCenter(coordinate);
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
          selectedMarkerId="current-location"
          onSelectMarker={(id) => {
            if (id.startsWith('property-')) navigate(`/properties/${id.slice('property-'.length)}/nearby`);
          }}
          onCenterChange={(latitude, longitude) => {
            const coordinate = { latitude, longitude };
            setViewportCenter((current) => (coordinatesAreClose(current, coordinate) ? current : coordinate));
            writeLastMapCenter(coordinate);
          }}
        />

        <MapCategoryRail
          selectedCategories={selectedCategories}
          counts={nearby.data?.counts}
          onToggle={(category) => setSelectedCategories((current) => toggleCategory(current, category))}
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
        {nearby.isError && selectedCategories.length > 0 && (
          <div className={styles.mapNotice} role="alert">
            시설 정보를 불러오지 못했어요.
            <button type="button" onClick={() => void nearby.refetch()}>
              다시 시도
            </button>
          </div>
        )}
        {selectedCategories.length === 0 && (
          <p className={styles.mapNotice} role="status">
            매물만 표시 중이에요. 시설 카테고리를 선택할 수 있어요.
          </p>
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
      </section>
    </main>
  );
};

export default MapPage;
