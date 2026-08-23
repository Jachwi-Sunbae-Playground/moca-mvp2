import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import MapCanvas from '../components/MapCanvas';
import type { MapMarker } from '../components/MapCanvas';
import { ButtonLink } from '../components/ui/Button';
import EmptyState from '../components/ui/EmptyState';
import Icon from '../components/ui/Icon';
import InlineNotice from '../components/ui/InlineNotice';
import TopNavigation from '../components/ui/TopNavigation';
import { usePropertyList } from '../hooks/query/useProperties';
import type { PublicConfig } from '../types/PublicConfig';
import { formatManwon } from '../utils/propertyFormat';
import styles from './MapPage.module.css';

const DEFAULT_CENTER = { latitude: 37.5665, longitude: 126.978 };

const MapPage = ({ config }: { config: PublicConfig }) => {
  const navigate = useNavigate();
  const properties = usePropertyList(config);
  const items = properties.data?.pages.flatMap((page) => page.content) ?? [];
  const mapped = items.filter((item) => item.location.latitude !== null && item.location.longitude !== null);
  const markers = useMemo<MapMarker[]>(
    () =>
      mapped.map((item) => ({
        id: String(item.propertyId),
        latitude: item.location.latitude ?? DEFAULT_CENTER.latitude,
        longitude: item.location.longitude ?? DEFAULT_CENTER.longitude,
        label: item.name,
        tone: 'property',
      })),
    [mapped],
  );
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [viewportCenter, setViewportCenter] = useState(DEFAULT_CENTER);
  const [locationFallback, setLocationFallback] = useState(false);
  const selected = mapped.find((item) => String(item.propertyId) === selectedId) ?? mapped[0];
  const center =
    selectedId !== null && selected?.location.latitude !== null && selected?.location.latitude !== undefined
      ? { latitude: selected.location.latitude, longitude: selected.location.longitude ?? DEFAULT_CENTER.longitude }
      : viewportCenter;

  useEffect(() => {
    if (!('geolocation' in navigator)) {
      setLocationFallback(true);
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (position) => setViewportCenter({ latitude: position.coords.latitude, longitude: position.coords.longitude }),
      () => setLocationFallback(true),
      { enableHighAccuracy: false, timeout: 3_000, maximumAge: 300_000 },
    );
  }, []);

  return (
    <main className={styles.page}>
      <TopNavigation
        title="지도에서 보기"
        endSlot={
          <ButtonLink to="/map/select-location" variant="secondary">
            <Icon name="plus" size={15} /> 매물 추가
          </ButtonLink>
        }
      />
      <section className={styles.mapSection} aria-label="매물 지도">
        {locationFallback && (
          <p className={styles.locationFallback} role="status">
            현재 위치 대신 서울 중심을 보여드려요. <Link to="/map/select-location">주소로 찾기</Link>
          </p>
        )}
        {properties.isPending ? (
          <div className={styles.state} role="status">
            지도에 매물을 표시하는 중이에요.
          </div>
        ) : properties.isError ? (
          <div className={styles.state}>
            <InlineNotice tone="error">지도용 매물 정보를 불러오지 못했어요.</InlineNotice>
            <button type="button" onClick={() => void properties.refetch()}>
              다시 시도
            </button>
          </div>
        ) : mapped.length === 0 ? (
          <EmptyState
            title="지도에 표시할 매물이 없어요"
            description="위치를 선택해 첫 매물을 등록해 보세요."
            action={<ButtonLink to="/map/select-location">지도에서 매물 추가</ButtonLink>}
          />
        ) : (
          <MapCanvas
            config={config}
            center={center}
            markers={markers}
            selectedMarkerId={selected === undefined ? null : String(selected.propertyId)}
            onSelectMarker={(id) => {
              setSelectedId(id);
              navigate(`/properties/${id}/nearby`);
            }}
          />
        )}
      </section>
      {selected !== undefined && (
        <section className={styles.propertySheet} aria-live="polite">
          <span className={styles.eyebrow}>선택한 매물</span>
          <h1>{selected.name}</h1>
          <p>{selected.location.address ?? '주소 미입력'}</p>
          <strong>
            보증금 {formatManwon(selected.depositAmount)} · 월세 {formatManwon(selected.monthlyRentAmount)}
          </strong>
          <div className={styles.sheetActions}>
            <Link to={`/properties/${selected.propertyId}`}>매물 보기</Link>
            <Link to={`/properties/${selected.propertyId}/nearby`}>주변 시설 분석</Link>
          </div>
        </section>
      )}
    </main>
  );
};

export default MapPage;
