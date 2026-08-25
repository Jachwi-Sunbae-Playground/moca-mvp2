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
  counts: { HOSPITAL: 1, TRANSPORT: 0, SCHOOL: 0, CONVENIENCE: 1, AGENCY: 0 },
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
    {
      providerPlaceId: 'demo-convenience-1',
      name: '모카 편의점',
      category: 'CONVENIENCE',
      address: '서울 관악구 신림로 18',
      latitude: 37.4845,
      longitude: 126.9295,
      distanceMeters: 180,
    },
  ],
});

const renderAuthenticated = (path: string) => {
  setAuthentication({ accessToken: 'demo-token', tokenType: 'Bearer', expiresIn: 60 });
  server.use(
    http.get(`${config.apiBaseUrl}/api/members/me`, () =>
      HttpResponse.json(successEnvelope({ id: 1, name: '이자취', passwordProtected: false })),
    ),
  );

  return render(
    <StrictMode>
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={[path]}>
          <AppRoutes config={config} />
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

  it('현재 위치 요약을 표시하고 시설을 선택했을 때만 핀과 목록을 제공한다', async () => {
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
    await waitFor(() => expect(requestedRadii).toContain('2000'));
    expect(await screen.findByRole('heading', { name: '현재 위치 주변 2km' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '병원 3개' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: '병원 표시하기, 3개' })).toHaveAttribute('aria-pressed', 'false');

    await user.click(screen.getByRole('button', { name: '병원 표시하기, 3개' }));
    expect(await screen.findByRole('button', { name: '병원 3개' })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '시설 목록 보기' }));
    expect(screen.getByRole('region', { name: '스크롤 가능한 주변 시설 목록' })).toBeInTheDocument();
    expect(screen.getByRole('list', { name: '주변 시설 목록' })).toHaveTextContent('신림 가까운 의원');
    expect(screen.getByRole('list', { name: '주변 시설 목록' })).not.toHaveTextContent('모카 편의점');

    await user.click(screen.getByRole('button', { name: '편의점 표시하기, 1개' }));
    const convenienceMarker = await screen.findByRole('button', { name: '모카 편의점' });
    expect(convenienceMarker.querySelector('[data-map-category-icon="CONVENIENCE"]')).toBeInTheDocument();
    expect(
      screen
        .getByRole('button', { name: '편의점 숨기기, 1개' })
        .querySelector('[data-map-category-icon="CONVENIENCE"]'),
    ).toBeInTheDocument();
    await user.click(convenienceMarker);
    const placeDetail = await screen.findByRole('region', { name: '모카 편의점 시설 상세' });
    expect(placeDetail).toHaveTextContent('편의점');
    expect(placeDetail).toHaveTextContent('모카 편의점');
    expect(placeDetail).toHaveTextContent('180m');
    expect(placeDetail).toHaveTextContent('서울 관악구 신림로 18');
    expect(screen.queryByRole('region', { name: '스크롤 가능한 주변 시설 목록' })).not.toBeInTheDocument();

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

  it('초기 카테고리를 끄고 선택 카테고리만 모바일 스크롤 목록에 표시한다', async () => {
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

    expect(await screen.findByRole('heading', { name: '신림역 원룸 매물 주변 2km' })).toBeInTheDocument();
    expect(screen.queryByText('신림역 원룸', { selector: 'span' })).not.toBeInTheDocument();
    expect(screen.queryByRole('list', { name: '주변 시설 목록' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: '병원 표시하기, 1개' })).toHaveAttribute('aria-pressed', 'false');
    await waitFor(() => expect(requestedRadii).toContain('2000'));

    await user.click(screen.getByRole('button', { name: '시설 목록 보기' }));
    expect(screen.getByText('위에서 확인할 시설을 선택해 주세요.')).toBeInTheDocument();
    expect(screen.getByRole('region', { name: '스크롤 가능한 주변 시설 목록' })).toHaveAttribute('tabindex', '0');

    await user.click(screen.getByRole('button', { name: '병원 1개 표시하기' }));
    expect(await screen.findByRole('list', { name: '주변 시설 목록' })).toHaveTextContent('신림 안심의원');
    expect(screen.getByRole('list', { name: '주변 시설 목록' })).not.toHaveTextContent('모카 편의점');

    await user.click(screen.getByRole('button', { name: '편의점 1개 표시하기' }));
    await user.click(await screen.findByRole('button', { name: '모카 편의점' }));
    const placeDetail = await screen.findByRole('region', { name: '모카 편의점 시설 상세' });
    expect(placeDetail).toHaveTextContent('180m');
    expect(placeDetail).toHaveTextContent('서울 관악구 신림로 18');
    await user.click(within(placeDetail).getByRole('button', { name: '모카 편의점 상세 닫기' }));
    expect(screen.queryByRole('region', { name: '모카 편의점 시설 상세' })).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '500m' }));
    await waitFor(() => expect(requestedRadii).toContain('500'));

    await user.click(screen.getByRole('button', { name: '학교 표시하기, 0개' }));
    expect(screen.getByRole('button', { name: '학교 숨기기, 0개' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: '병원 숨기기, 1개' })).toHaveAttribute('aria-pressed', 'true');

    await user.click(screen.getByRole('button', { name: '전체' }));
    expect(screen.getByRole('button', { name: '전체' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: '2km' })).toHaveAttribute('aria-pressed', 'false');
    expect(screen.getByRole('link', { name: '매물 지도로 돌아가기' })).toHaveAttribute('href', '/map');
  });
});
