import { useEffect, useRef } from 'react';
import type { PublicConfig } from '../types/PublicConfig';
import styles from './MapCanvas.module.css';

export type MapMarker = {
  id: string;
  latitude: number;
  longitude: number;
  label: string;
  tone?: 'property' | 'place';
};

type MapCanvasProps = {
  config: PublicConfig;
  center: { latitude: number; longitude: number };
  markers?: MapMarker[];
  interactive?: boolean;
  selectedMarkerId?: string | null;
  onSelectMarker?: (id: string) => void;
  onSelectLocation?: (latitude: number, longitude: number) => void;
};

const loadKakaoSdk = (key: string): Promise<void> =>
  new Promise((resolve, reject) => {
    const existing = document.querySelector<HTMLScriptElement>('script[data-moca-kakao-map]');
    const ready = () => {
      if (window.kakao?.maps === undefined) return reject(new Error('Kakao Maps SDK를 불러오지 못했습니다.'));
      window.kakao.maps.load(resolve);
    };
    if (existing !== null) {
      if (window.kakao?.maps !== undefined) ready();
      else existing.addEventListener('load', ready, { once: true });
      return;
    }
    const script = document.createElement('script');
    script.dataset.mocaKakaoMap = 'true';
    script.async = true;
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${encodeURIComponent(key)}&autoload=false`;
    script.addEventListener('load', ready, { once: true });
    script.addEventListener('error', () => reject(new Error('Kakao Maps SDK를 불러오지 못했습니다.')), { once: true });
    document.head.append(script);
  });

const MapCanvas = ({
  config,
  center,
  markers = [],
  interactive = false,
  selectedMarkerId = null,
  onSelectMarker,
  onSelectLocation,
}: MapCanvasProps) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const liveMode = config.mapProviderMode === 'kakao' && (config.kakaoMapJavaScriptKey ?? '') !== '';

  useEffect(() => {
    if (!liveMode || containerRef.current === null) return;
    let disposed = false;
    void loadKakaoSdk(config.kakaoMapJavaScriptKey ?? '').then(() => {
      if (disposed || containerRef.current === null || window.kakao?.maps === undefined) return;
      const maps = window.kakao.maps;
      const map = new maps.Map(containerRef.current, {
        center: new maps.LatLng(center.latitude, center.longitude),
        level: 5,
      });
      markers.forEach((marker) => {
        const sdkMarker = new maps.Marker({
          map,
          position: new maps.LatLng(marker.latitude, marker.longitude),
          title: marker.label,
        });
        if (onSelectMarker !== undefined) maps.event.addListener(sdkMarker, 'click', () => onSelectMarker(marker.id));
      });
      if (interactive && onSelectLocation !== undefined) {
        maps.event.addListener(map, 'click', (event) => onSelectLocation(event.latLng.getLat(), event.latLng.getLng()));
      }
    });
    return () => {
      disposed = true;
    };
  }, [
    center.latitude,
    center.longitude,
    config.kakaoMapJavaScriptKey,
    interactive,
    liveMode,
    markers,
    onSelectLocation,
    onSelectMarker,
  ]);

  if (liveMode) return <div ref={containerRef} className={styles.canvas} aria-label="Kakao 지도" />;

  return (
    <div
      className={`${styles.canvas} ${styles.demo}`}
      aria-label="데모 지도"
      onClick={(event) => {
        if (!interactive || onSelectLocation === undefined) return;
        const rect = event.currentTarget.getBoundingClientRect();
        const latitude = center.latitude + (0.5 - (event.clientY - rect.top) / rect.height) * 0.018;
        const longitude = center.longitude + ((event.clientX - rect.left) / rect.width - 0.5) * 0.024;
        onSelectLocation(latitude, longitude);
      }}
    >
      <span className={styles.roadOne} />
      <span className={styles.roadTwo} />
      <span className={styles.park}>MOCA PARK</span>
      {markers.map((marker, index) => (
        <button
          key={marker.id}
          type="button"
          className={`${styles.marker} ${marker.tone === 'place' ? styles.placeMarker : ''}`}
          data-selected={selectedMarkerId === marker.id}
          style={{ left: `${18 + ((index * 23) % 68)}%`, top: `${18 + ((index * 31) % 60)}%` }}
          aria-label={marker.label}
          onClick={(event) => {
            event.stopPropagation();
            onSelectMarker?.(marker.id);
          }}
        >
          {index + 1}
        </button>
      ))}
      {interactive && markers.length === 0 && <span className={styles.centerPin} aria-hidden="true" />}
      <span className={styles.demoBadge}>DEMO MAP</span>
    </div>
  );
};

export default MapCanvas;
