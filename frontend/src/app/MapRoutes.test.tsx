import { QueryClientProvider } from '@tanstack/react-query';
import { StrictMode } from 'react';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { HttpResponse, http } from 'msw';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { successEnvelope } from '../test/propertyFixtures';
import { server } from '../test/server';
import type { PublicConfig } from '../types/PublicConfig';
import AppRoutes from './AppRoutes';
import { setAuthentication } from './authStore';
import { queryClient } from './queryClient';

const config: PublicConfig = {
  apiBaseUrl: 'http://localhost:8080',
  googleClientId: '',
  googleRedirectUri: 'http://localhost:3000/oauth/google/callback',
  authMode: 'demo',
  mapProviderMode: 'demo',
};

const progress = {
  totalCount: 0,
  completedCount: 0,
  goodCount: 0,
  cautionCount: 0,
  unconfirmedCount: 0,
  progressRate: 0,
};

const property = {
  id: 10,
  name: '신림역 원룸',
  depositAmount: 10_000_000,
  monthlyRentAmount: 550_000,
  discoverySource: '데모 지도',
  address: '서울 관악구 신림로 12길 3',
  roadAddress: '서울 관악구 신림로 12길 3',
  jibunAddress: '서울 관악구 신림동 1433-12',
  latitude: 37.48412,
  longitude: 126.92912,
  photoCount: 0,
  photos: [],
  representativePhoto: null,
  overallProgress: progress,
  createdAt: '2026-08-10T07:30:00Z',
  updatedAt: '2026-08-10T07:40:00Z',
  lastActivityAt: '2026-08-10T07:40:00Z',
};

const nearbyResult = (radius: number) => ({
  center: { latitude: property.latitude, longitude: property.longitude },
  radius,
  counts: { HOSPITAL: 1, TRANSPORT: 0, SCHOOL: 0, CONVENIENCE: 0, AGENCY: 0 },
  places: [
    {
      providerPlaceId: 'demo-hospital-1',
      name: '신림 안심의원',
      category: 'HOSPITAL',
      address: '서울 관악구 신림로 20',
      latitude: 37.485,
      longitude: 126.93,
      distanceMeters: 320,
    },
  ],
});

const renderAuthenticated = (path: string) => {
  setAuthentication({ accessToken: 'demo-token', tokenType: 'Bearer', expiresIn: 60 });
  server.use(
    http.get(`${config.apiBaseUrl}/api/members/me`, () =>
      HttpResponse.json(successEnvelope({ id: 1, name: '이자취', email: 'demo@moca.local' })),
    ),
  );

  return render(
    <StrictMode>
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={[path]}>
          <AppRoutes config={config} storage={window.sessionStorage} />
        </MemoryRouter>
      </QueryClientProvider>
    </StrictMode>,
  );
};

const originalGeolocation = navigator.geolocation;

describe('MVP2 지도 화면', () => {
  beforeEach(() => {
    queryClient.clear();
    Object.defineProperty(navigator, 'geolocation', {
      configurable: true,
      value: {
        getCurrentPosition: vi.fn((success: PositionCallback) =>
          success({
            coords: {
              latitude: property.latitude,
              longitude: property.longitude,
              accuracy: 10,
              altitude: null,
              altitudeAccuracy: null,
              heading: null,
              speed: null,
              toJSON: () => ({}),
            },
            timestamp: Date.now(),
            toJSON: () => ({}),
          }),
        ),
      },
    });
  });

  afterEach(() => {
    Object.defineProperty(navigator, 'geolocation', { configurable: true, value: originalGeolocation });
  });

  it('현재 위치와 매물·시설 군집을 표시하고 반경 변경과 매물 주변 분석 이동을 제공한다', async () => {
    const requestedRadii: string[] = [];
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties`, () =>
        HttpResponse.json(successEnvelope({ totalCount: 1, items: [property] })),
      ),
      http.get(`${config.apiBaseUrl}/api/properties/10`, () => HttpResponse.json(successEnvelope(property))),
      http.get(`${config.apiBaseUrl}/api/maps/nearby`, ({ request }) => {
        const radius = Number(new URL(request.url).searchParams.get('radius'));
        requestedRadii.push(String(radius));
        const result = nearbyResult(radius);
        return HttpResponse.json(
          successEnvelope({
            ...result,
            counts: { ...result.counts, HOSPITAL: 3 },
            places: [
              ...result.places,
              {
                ...result.places[0],
                providerPlaceId: 'demo-hospital-nearest',
                name: '신림 가까운 의원',
                distanceMeters: 120,
              },
              {
                ...result.places[0],
                providerPlaceId: 'demo-hospital-far',
                name: '신림 먼 의원',
                distanceMeters: 890,
              },
            ],
          }),
        );
      }),
    );

    const user = userEvent.setup();
    renderAuthenticated('/map');

    expect(await screen.findByRole('generic', { name: '데모 지도' })).toBeInTheDocument();
    expect(await screen.findByRole('img', { name: '현재 위치' })).toBeInTheDocument();
    expect(await screen.findByRole('img', { name: '병원 3개' })).toBeInTheDocument();
    await waitFor(() => expect(requestedRadii).toContain('2000'));
    await user.click(screen.getByRole('button', { name: '1km' }));
    await waitFor(() => expect(requestedRadii).toContain('1000'));
    expect(screen.getByRole('button', { name: '1km' })).toHaveAttribute('aria-pressed', 'true');
    await user.click(await screen.findByRole('button', { name: '신림역 원룸' }));

    expect(await screen.findByRole('heading', { name: '매물 주변 분석' })).toBeInTheDocument();
    expect(screen.getByRole('img', { name: '선택한 매물' })).toBeInTheDocument();
  });

  it('주소 검색은 필요할 때 열고 도로명·지번 주소와 좌표를 위치 선택에 유지한다', async () => {
    const user = userEvent.setup();
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties`, () =>
        HttpResponse.json(successEnvelope({ totalCount: 0, items: [] })),
      ),
      http.get(`${config.apiBaseUrl}/api/maps/reverse-geocode`, () =>
        HttpResponse.json(
          successEnvelope({
            roadAddress: property.roadAddress,
            jibunAddress: property.jibunAddress,
            latitude: property.latitude,
            longitude: property.longitude,
          }),
        ),
      ),
      http.get(`${config.apiBaseUrl}/api/maps/geocode`, ({ request }) => {
        expect(new URL(request.url).searchParams.get('query')).toBe('신림');
        return HttpResponse.json(
          successEnvelope([
            {
              roadAddress: property.roadAddress,
              jibunAddress: property.jibunAddress,
              latitude: property.latitude,
              longitude: property.longitude,
            },
          ]),
        );
      }),
    );

    renderAuthenticated('/map/select-location');
    const openSearchButton = await screen.findByRole('button', { name: '주소 검색 열기' });
    expect(screen.queryByRole('textbox', { name: '주소 검색' })).not.toBeInTheDocument();
    expect(screen.queryByRole('navigation', { name: '주요 메뉴' })).not.toBeInTheDocument();

    await user.click(openSearchButton);
    await user.type(await screen.findByRole('textbox', { name: '주소 검색' }), '신림');
    await user.click(screen.getByRole('button', { name: '검색' }));

    const results = await screen.findByRole('list', { name: '주소 검색 결과' });
    expect(within(results).getByText(property.roadAddress)).toBeInTheDocument();
    expect(within(results).getByText(property.jibunAddress)).toBeInTheDocument();
    await user.click(within(results).getByRole('button'));
    expect(screen.getByRole('button', { name: '이 위치로 매물 등록하기' })).toBeEnabled();
  });

  it('초기 2km 집계, 반경 재조회, 다중 카테고리와 접힌 시설 목록을 제공한다', async () => {
    const user = userEvent.setup();
    const requestedRadii: string[] = [];
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () => HttpResponse.json(successEnvelope(property))),
      http.get(`${config.apiBaseUrl}/api/maps/nearby`, ({ request }) => {
        const radius = new URL(request.url).searchParams.get('radius') ?? '';
        requestedRadii.push(radius);
        return HttpResponse.json(successEnvelope(nearbyResult(Number(radius))));
      }),
    );

    renderAuthenticated('/properties/10/nearby');

    expect(await screen.findByRole('heading', { name: '선택한 매물 주변 2km' })).toBeInTheDocument();
    expect(screen.queryByRole('list', { name: '주변 시설 목록' })).not.toBeInTheDocument();
    await waitFor(() => expect(requestedRadii).toContain('2000'));

    await user.click(screen.getByRole('button', { name: '시설 목록 보기' }));
    expect(await screen.findByRole('list', { name: '주변 시설 목록' })).toHaveTextContent('신림 안심의원');

    await user.click(screen.getByRole('button', { name: '500m' }));
    await waitFor(() => expect(requestedRadii).toContain('500'));

    await user.click(screen.getByRole('button', { name: '학교 숨기기, 0개' }));
    expect(screen.getByRole('button', { name: '학교 표시하기, 0개' })).toHaveAttribute('aria-pressed', 'false');
    expect(screen.getByRole('button', { name: '병원 숨기기, 1개' })).toHaveAttribute('aria-pressed', 'true');

    await user.click(screen.getByRole('button', { name: '전체' }));
    expect(screen.getByRole('button', { name: '전체' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: '2km' })).toHaveAttribute('aria-pressed', 'false');
    expect(screen.getByRole('link', { name: '매물 지도로 돌아가기' })).toHaveAttribute('href', '/map');
  });
});
