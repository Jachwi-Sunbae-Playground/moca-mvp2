import { describe, expect, it } from 'vitest';
import type { NearbyPlace } from '../types/Map';
import { clusterNearbyPlaces } from './mapClustering';

const place = (id: string, latitude: number, longitude: number, category: NearbyPlace['category'] = 'TRANSPORT') => ({
  providerPlaceId: id,
  name: `장소 ${id}`,
  category,
  address: '서울 중구 세종대로',
  latitude,
  longitude,
  distanceMeters: Number(id.replace(/\D/g, '')) || 0,
});

describe('지도 장소 군집', () => {
  it('확대한 지도에서는 실제 장소를 각각 표시한다', () => {
    const markers = clusterNearbyPlaces([place('bus-1', 37.5665, 126.978), place('bus-2', 37.5665, 126.978)], 3);

    expect(markers).toHaveLength(2);
    expect(markers.map((marker) => marker.label)).toEqual(['장소 bus-1', '장소 bus-2']);
    expect(markers.every((marker) => marker.tone === 'place')).toBe(true);
  });

  it('축소한 지도에서는 가까운 장소만 실제 좌표의 중심으로 묶는다', () => {
    const markers = clusterNearbyPlaces(
      [
        place('bus-1', 37.5665, 126.978),
        place('bus-2', 37.56655, 126.978),
        place('hospital-3', 37.57, 126.982, 'HOSPITAL'),
      ],
      5,
    );

    expect(markers).toHaveLength(2);
    expect(markers).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ tone: 'cluster', category: 'TRANSPORT', count: 2, label: '교통 2개' }),
        expect.objectContaining({ tone: 'place', category: 'HOSPITAL', label: '장소 hospital-3' }),
      ]),
    );
  });
});
