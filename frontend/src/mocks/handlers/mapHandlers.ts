import { http } from 'msw';
import { success } from '../mockStore';

const address = {
  roadAddress: '서울 중구 세종대로 110',
  jibunAddress: '서울 중구 태평로1가 31',
  latitude: 37.5665,
  longitude: 126.978,
};

const nearbyPlaces = [
  ...Array.from({ length: 14 }, (_, index) => {
    const distanceMeters = 120 + index * 80;
    const offset = distanceMeters / 1_000_000;
    return {
      providerPlaceId: `demo-hospital-${index + 1}`,
      name: index === 0 ? '서울시립병원' : `도심 의원 ${index + 1}`,
      category: 'HOSPITAL' as const,
      address: `서울 중구 세종대로 ${92 + index}`,
      latitude: address.latitude + offset,
      longitude: address.longitude + offset,
      distanceMeters,
    };
  }),
  {
    providerPlaceId: 'demo-transport-1',
    name: '시청역',
    category: 'TRANSPORT' as const,
    address: '서울 중구 세종대로 지하 101',
    latitude: 37.5668,
    longitude: 126.9783,
    distanceMeters: 280,
  },
  {
    providerPlaceId: 'demo-convenience-1',
    name: '모카 편의점',
    category: 'CONVENIENCE' as const,
    address: '서울 중구 세종대로 100',
    latitude: 37.5667,
    longitude: 126.9782,
    distanceMeters: 180,
  },
  {
    providerPlaceId: 'demo-agency-1',
    name: '자취선배 공인중개사',
    category: 'AGENCY' as const,
    address: '서울 중구 다동길 8',
    latitude: 37.567,
    longitude: 126.9785,
    distanceMeters: 510,
  },
];

export const mapHandlers = [
  http.get('*/api/maps/geocode', () => success([address])),
  http.get('*/api/maps/reverse-geocode', ({ request }) => {
    const url = new URL(request.url);
    return success({
      ...address,
      latitude: Number(url.searchParams.get('latitude') ?? address.latitude),
      longitude: Number(url.searchParams.get('longitude') ?? address.longitude),
    });
  }),
  http.get('*/api/maps/nearby', ({ request }) => {
    const radius = Number(new URL(request.url).searchParams.get('radius') ?? 2000);
    const places = nearbyPlaces.filter((place) => place.distanceMeters <= radius);
    const counts = { HOSPITAL: 0, TRANSPORT: 0, SCHOOL: 0, CONVENIENCE: 0, AGENCY: 0 };
    places.forEach((place) => {
      counts[place.category] += 1;
    });
    return success({
      center: { latitude: address.latitude, longitude: address.longitude },
      radius,
      counts,
      places,
    });
  }),
];
