import { useEffect, useRef, useState } from 'react';
import type { CSSProperties } from 'react';
import type { MapCategory } from '../types/Map';
import type { PublicConfig } from '../types/PublicConfig';
import MapCategoryIcon, { createMapCategoryIconElement } from './MapCategoryIcon';
import styles from './MapCanvas.module.css';

export type MapMarker = {
  id: string;
  latitude: number;
  longitude: number;
  label: string;
  tone?: 'property' | 'current' | 'place' | 'selected' | 'cluster';
  category?: MapCategory;
  count?: number;
  placeId?: string;
  actionable?: boolean;
};

export type MapRadiusCircle = {
  radiusMeters: 500 | 1000 | 2000;
  label: string;
};

type MapCanvasProps = {
  config: PublicConfig;
  center: { latitude: number; longitude: number };
  markers?: MapMarker[];
  circles?: MapRadiusCircle[];
  level?: number;
  interactive?: boolean;
  showCenterPin?: boolean;
  showRadiusLabels?: boolean;
  selectedMarkerId?: string | null;
  onSelectMarker?: (marker: MapMarker) => void;
  onSelectLocation?: (latitude: number, longitude: number) => void;
  onCenterChange?: (latitude: number, longitude: number) => void;
  onLevelChange?: (level: number) => void;
  radiusCenter?: { latitude: number; longitude: number };
};

let kakaoSdkPromise: Promise<void> | null = null;

const loadKakaoSdk = (key: string): Promise<void> => {
  if (window.kakao?.maps !== undefined) {
    return new Promise((resolve) => window.kakao?.maps.load(resolve));
  }
  if (kakaoSdkPromise !== null) return kakaoSdkPromise;

  kakaoSdkPromise = new Promise((resolve, reject) => {
    const existing = document.querySelector<HTMLScriptElement>('script[data-moca-kakao-map]');
    const ready = () => {
      if (window.kakao?.maps === undefined) {
        kakaoSdkPromise = null;
        reject(new Error('Kakao Maps SDK를 불러오지 못했습니다.'));
        return;
      }
      window.kakao.maps.load(resolve);
    };
    if (existing !== null) {
      existing.addEventListener('load', ready, { once: true });
      existing.addEventListener(
        'error',
        () => {
          kakaoSdkPromise = null;
          reject(new Error('Kakao Maps SDK를 불러오지 못했습니다.'));
        },
        { once: true },
      );
      return;
    }
    const script = document.createElement('script');
    script.dataset.mocaKakaoMap = 'true';
    script.async = true;
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${encodeURIComponent(key)}&autoload=false`;
    script.addEventListener('load', ready, { once: true });
    script.addEventListener(
      'error',
      () => {
        kakaoSdkPromise = null;
        reject(new Error('Kakao Maps SDK를 불러오지 못했습니다.'));
      },
      { once: true },
    );
    document.head.append(script);
  });
  return kakaoSdkPromise;
};

const markerSymbol = (marker: MapMarker): string => {
  if (marker.tone === 'cluster') return String(marker.count ?? '');
  if (marker.tone === 'current') return '◎';
  if (marker.tone === 'property' || marker.tone === 'selected') return '⌂';
  return '•';
};

const markerClassName = (marker: MapMarker, selectedMarkerId: string | null): string =>
  [
    styles.marker,
    marker.tone === 'property' ? styles.propertyMarker : '',
    marker.tone === 'current' ? styles.currentMarker : '',
    marker.tone === 'selected' ? styles.selectedMarker : '',
    marker.tone === 'place' ? styles.placeMarker : '',
    marker.tone === 'cluster' ? styles.clusterMarker : '',
    selectedMarkerId === marker.id ? styles.activeMarker : '',
  ]
    .filter(Boolean)
    .join(' ');

const createMarkerContent = (
  marker: MapMarker,
  selectedMarkerId: string | null,
  onSelectMarker?: (marker: MapMarker) => void,
): HTMLElement => {
  const canSelect = marker.actionable === true && onSelectMarker !== undefined;
  const element = document.createElement(canSelect ? 'button' : 'div');
  element.className = markerClassName(marker, selectedMarkerId);
  element.dataset.tone = marker.tone ?? 'property';
  if (marker.category !== undefined) element.dataset.category = marker.category;
  element.setAttribute('aria-label', marker.label);
  if (!canSelect) element.setAttribute('role', 'img');
  else {
    element.setAttribute('type', 'button');
    element.addEventListener('click', (event) => {
      event.stopPropagation();
      onSelectMarker(marker);
    });
  }

  const usesCategoryIcon = (marker.tone === 'place' || marker.tone === 'cluster') && marker.category !== undefined;
  if (usesCategoryIcon && marker.category !== undefined) {
    element.append(createMapCategoryIconElement(marker.category, styles.categoryIcon));
  } else {
    const icon = document.createElement(marker.tone === 'cluster' ? 'strong' : 'span');
    icon.className = styles.markerIcon;
    icon.setAttribute('aria-hidden', 'true');
    icon.textContent = markerSymbol(marker);
    element.append(icon);
  }

  if (marker.count !== undefined && marker.category !== undefined) {
    const count = document.createElement('strong');
    count.className = styles.markerCount;
    count.textContent = String(marker.count);
    element.append(count);
  }

  if (marker.tone === 'current' || marker.tone === 'selected') {
    const caption = document.createElement('span');
    caption.className = styles.markerCaption;
    caption.textContent = marker.label;
    element.append(caption);
  }
  return element;
};

const clamp = (value: number, min: number, max: number): number => Math.min(Math.max(value, min), max);

const demoMarkerStyle = (marker: MapMarker, center: { latitude: number; longitude: number }): CSSProperties => ({
  left: `${clamp(50 + (marker.longitude - center.longitude) * 3_100, 9, 91)}%`,
  top: `${clamp(50 - (marker.latitude - center.latitude) * 4_200, 10, 88)}%`,
});

const MapCanvas = ({
  config,
  center,
  markers = [],
  circles = [],
  level = 5,
  interactive = false,
  showCenterPin = false,
  showRadiusLabels = false,
  selectedMarkerId = null,
  onSelectMarker,
  onSelectLocation,
  onCenterChange,
  onLevelChange,
  radiusCenter = center,
}: MapCanvasProps) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<KakaoMap | null>(null);
  const overlaysRef = useRef<KakaoCustomOverlay[]>([]);
  const circlesRef = useRef<KakaoCircle[]>([]);
  const callbackRef = useRef({ onSelectLocation, onCenterChange, onLevelChange });
  const [mapReady, setMapReady] = useState(false);
  const [sdkError, setSdkError] = useState(false);
  const liveMode = config.mapProviderMode === 'kakao' && (config.kakaoMapJavaScriptKey ?? '') !== '';

  callbackRef.current = { onSelectLocation, onCenterChange, onLevelChange };

  useEffect(() => {
    if (!liveMode || containerRef.current === null) return;
    let disposed = false;
    let map: KakaoMap | null = null;
    let clickListener: ((event: { latLng: KakaoLatLng }) => void) | null = null;
    let idleListener: ((event: { latLng: KakaoLatLng }) => void) | null = null;

    setSdkError(false);
    setMapReady(false);
    void loadKakaoSdk(config.kakaoMapJavaScriptKey ?? '')
      .then(() => {
        if (disposed || containerRef.current === null || window.kakao?.maps === undefined) return;
        const maps = window.kakao.maps;
        map = new maps.Map(containerRef.current, {
          center: new maps.LatLng(center.latitude, center.longitude),
          level,
        });
        mapRef.current = map;
        clickListener = (event) => callbackRef.current.onSelectLocation?.(event.latLng.getLat(), event.latLng.getLng());
        idleListener = () => {
          if (map === null) return;
          const nextCenter = map.getCenter();
          callbackRef.current.onCenterChange?.(nextCenter.getLat(), nextCenter.getLng());
          callbackRef.current.onLevelChange?.(map.getLevel());
        };
        maps.event.addListener(map, 'click', clickListener);
        maps.event.addListener(map, 'idle', idleListener);
        setMapReady(true);
      })
      .catch(() => {
        if (!disposed) setSdkError(true);
      });

    return () => {
      disposed = true;
      if (map !== null && window.kakao?.maps !== undefined) {
        if (clickListener !== null) window.kakao.maps.event.removeListener(map, 'click', clickListener);
        if (idleListener !== null) window.kakao.maps.event.removeListener(map, 'idle', idleListener);
      }
      overlaysRef.current.forEach((overlay) => overlay.setMap(null));
      circlesRef.current.forEach((circle) => circle.setMap(null));
      overlaysRef.current = [];
      circlesRef.current = [];
      mapRef.current = null;
    };
  }, [config.kakaoMapJavaScriptKey, liveMode]);

  useEffect(() => {
    const maps = window.kakao?.maps;
    if (!liveMode || !mapReady || maps === undefined || mapRef.current === null) return;
    const current = mapRef.current.getCenter();
    if (
      Math.abs(current.getLat() - center.latitude) < 0.0000001 &&
      Math.abs(current.getLng() - center.longitude) < 0.0000001
    )
      return;
    mapRef.current.setCenter(new maps.LatLng(center.latitude, center.longitude));
  }, [center.latitude, center.longitude, liveMode, mapReady]);

  useEffect(() => {
    const map = mapRef.current;
    if (!liveMode || !mapReady || map === null || map.getLevel() === level) return;
    map.setLevel(level);
  }, [level, liveMode, mapReady]);

  useEffect(() => {
    const maps = window.kakao?.maps;
    const map = mapRef.current;
    if (!liveMode || !mapReady || maps === undefined || map === null) return;
    overlaysRef.current.forEach((overlay) => overlay.setMap(null));
    overlaysRef.current = markers.map(
      (marker) =>
        new maps.CustomOverlay({
          map,
          position: new maps.LatLng(marker.latitude, marker.longitude),
          content: createMarkerContent(marker, selectedMarkerId, onSelectMarker),
          xAnchor: 0.5,
          yAnchor: marker.tone === 'property' || marker.tone === 'current' || marker.tone === 'selected' ? 0.82 : 0.5,
          zIndex:
            selectedMarkerId === marker.id
              ? 10
              : marker.tone === 'selected'
                ? 9
                : marker.tone === 'property'
                  ? 8
                  : marker.tone === 'current'
                    ? 7
                    : 5,
        }),
    );
    return () => {
      overlaysRef.current.forEach((overlay) => overlay.setMap(null));
      overlaysRef.current = [];
    };
  }, [liveMode, mapReady, markers, onSelectMarker, selectedMarkerId]);

  useEffect(() => {
    const maps = window.kakao?.maps;
    const map = mapRef.current;
    if (!liveMode || !mapReady || maps === undefined || map === null) return;
    circlesRef.current.forEach((circle) => circle.setMap(null));
    circlesRef.current = circles.map(
      (circle) =>
        new maps.Circle({
          map,
          center: new maps.LatLng(radiusCenter.latitude, radiusCenter.longitude),
          radius: circle.radiusMeters,
          strokeWeight: 2,
          strokeColor: '#6ea8fe',
          strokeOpacity: 0.56,
          fillColor: '#8ab8ff',
          fillOpacity: 0.08,
        }),
    );
    return () => {
      circlesRef.current.forEach((circle) => circle.setMap(null));
      circlesRef.current = [];
    };
  }, [circles, liveMode, mapReady, radiusCenter.latitude, radiusCenter.longitude]);

  return (
    <div
      className={`${styles.canvas} ${liveMode ? styles.live : styles.demo}`}
      aria-label={liveMode ? 'Kakao 지도' : '데모 지도'}
      onClick={(event) => {
        if (liveMode || !interactive || onSelectLocation === undefined) return;
        const rect = event.currentTarget.getBoundingClientRect();
        const latitude = center.latitude + (0.5 - (event.clientY - rect.top) / rect.height) * 0.018;
        const longitude = center.longitude + ((event.clientX - rect.left) / rect.width - 0.5) * 0.024;
        onSelectLocation(latitude, longitude);
      }}
    >
      {liveMode && <div ref={containerRef} className={styles.liveLayer} />}
      {!liveMode && (
        <>
          <span className={styles.roadOne} />
          <span className={styles.roadTwo} />
          <span className={styles.park}>MOCA PARK</span>
          {circles.map((circle) => (
            <span
              key={circle.radiusMeters}
              className={styles.radiusCircle}
              style={{ width: `${(circle.radiusMeters / 2000) * 84}%` }}
              aria-hidden="true"
            />
          ))}
          {markers.map((marker) => {
            const usesCategoryIcon =
              (marker.tone === 'place' || marker.tone === 'cluster') && marker.category !== undefined;
            const markerNode = (
              <>
                {usesCategoryIcon && marker.category !== undefined ? (
                  <MapCategoryIcon category={marker.category} className={styles.categoryIcon} />
                ) : (
                  <span className={styles.markerIcon} aria-hidden="true">
                    {markerSymbol(marker)}
                  </span>
                )}
                {marker.count !== undefined && marker.category !== undefined && (
                  <strong className={styles.markerCount}>{marker.count}</strong>
                )}
                {(marker.tone === 'current' || marker.tone === 'selected') && (
                  <span className={styles.markerCaption}>{marker.label}</span>
                )}
              </>
            );
            const className = markerClassName(marker, selectedMarkerId);
            const canSelect = marker.actionable === true && onSelectMarker !== undefined;
            return !canSelect ? (
              <div
                key={marker.id}
                className={`${styles.demoMarkerPosition} ${className}`}
                data-tone={marker.tone ?? 'property'}
                data-category={marker.category}
                style={demoMarkerStyle(marker, center)}
                role="img"
                aria-label={marker.label}
              >
                {markerNode}
              </div>
            ) : (
              <button
                key={marker.id}
                type="button"
                className={`${styles.demoMarkerPosition} ${className}`}
                data-tone={marker.tone ?? 'property'}
                data-category={marker.category}
                style={demoMarkerStyle(marker, center)}
                aria-label={marker.label}
                onClick={(event) => {
                  event.stopPropagation();
                  onSelectMarker(marker);
                }}
              >
                {markerNode}
              </button>
            );
          })}
          <span className={styles.demoBadge}>DEMO MAP</span>
        </>
      )}
      {showCenterPin && (
        <span className={styles.fixedCenterPin} aria-label="선택할 지도 중심" role="img">
          <span aria-hidden="true">◎</span>
          <small>선택 위치</small>
        </span>
      )}
      {showRadiusLabels && (
        <div className={styles.radiusLabels} aria-hidden="true">
          {circles
            .slice()
            .reverse()
            .map((circle) => (
              <span key={circle.radiusMeters}>{circle.label}</span>
            ))}
        </div>
      )}
      {sdkError && <div className={styles.sdkError}>지도를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.</div>}
    </div>
  );
};

export default MapCanvas;
