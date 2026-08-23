import { http } from 'msw';
import { success } from '../mockStore';

const address = {
  roadAddress: '서울 중구 세종대로 110',
  jibunAddress: '서울 중구 태평로1가 31',
  latitude: 37.5665,
  longitude: 126.978,
};

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
  http.get('*/api/maps/nearby', () =>
    success({
      center: { latitude: address.latitude, longitude: address.longitude },
      radius: 1000,
      counts: { HOSPITAL: 1, TRANSPORT: 1, SCHOOL: 0, CONVENIENCE: 1, AGENCY: 1 },
      places: [
        {
          providerPlaceId: 'demo-hospital-1',
          name: '서울시립병원',
          category: 'HOSPITAL',
          address: '서울 중구 세종대로 92',
          latitude: 37.567,
          longitude: 126.9785,
          distanceMeters: 320,
        },
      ],
    }),
  ),
];
