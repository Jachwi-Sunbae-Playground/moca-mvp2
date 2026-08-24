export type MapCoordinate = {
  latitude: number;
  longitude: number;
};

export const SEOUL_MAP_CENTER: MapCoordinate = { latitude: 37.5665, longitude: 126.978 };

const LAST_MAP_CENTER_KEY = 'moca.lastMapCenter';

const isCoordinate = (value: unknown): value is MapCoordinate => {
  if (typeof value !== 'object' || value === null) return false;
  const coordinate = value as Partial<MapCoordinate>;
  return (
    typeof coordinate.latitude === 'number' &&
    Number.isFinite(coordinate.latitude) &&
    coordinate.latitude >= -90 &&
    coordinate.latitude <= 90 &&
    typeof coordinate.longitude === 'number' &&
    Number.isFinite(coordinate.longitude) &&
    coordinate.longitude >= -180 &&
    coordinate.longitude <= 180
  );
};

export const readLastMapCenter = (storage: Storage = window.sessionStorage): MapCoordinate | null => {
  try {
    const stored = storage.getItem(LAST_MAP_CENTER_KEY);
    if (stored === null) return null;
    const parsed: unknown = JSON.parse(stored);
    return isCoordinate(parsed) ? parsed : null;
  } catch {
    return null;
  }
};

export const writeLastMapCenter = (coordinate: MapCoordinate, storage: Storage = window.sessionStorage): void => {
  try {
    storage.setItem(LAST_MAP_CENTER_KEY, JSON.stringify(coordinate));
  } catch {
    // 브라우저 저장소를 사용할 수 없어도 지도 탐색은 계속한다.
  }
};

export const requestCurrentMapLocation = (): Promise<MapCoordinate> =>
  new Promise((resolve, reject) => {
    if (!('geolocation' in navigator)) {
      reject(new Error('현재 위치를 사용할 수 없습니다.'));
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (position) => resolve({ latitude: position.coords.latitude, longitude: position.coords.longitude }),
      () => reject(new Error('현재 위치를 확인하지 못했습니다.')),
      { enableHighAccuracy: true, timeout: 7_000, maximumAge: 120_000 },
    );
  });

export const coordinatesAreClose = (first: MapCoordinate, second: MapCoordinate, tolerance = 0.000_01): boolean =>
  Math.abs(first.latitude - second.latitude) < tolerance && Math.abs(first.longitude - second.longitude) < tolerance;
