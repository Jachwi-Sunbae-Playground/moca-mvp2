import { QueryClientProvider } from '@tanstack/react-query';
import { StrictMode } from 'react';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { HttpResponse, http } from 'msw';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
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

describe('MVP2 지도 화면', () => {
  it('위치가 있는 매물을 데모 지도와 선택 카드에 표시한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties`, () =>
        HttpResponse.json(successEnvelope({ totalCount: 1, items: [property] })),
      ),
    );

    renderAuthenticated('/map');

    expect(await screen.findByRole('generic', { name: '데모 지도' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '신림역 원룸' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: '신림역 원룸' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '주변 시설 분석' })).toHaveAttribute('href', '/properties/10/nearby');
  });

  it('도로명·지번 주소 검색 결과와 좌표를 위치 선택에 유지한다', async () => {
    const user = userEvent.setup();
    server.use(
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
    await user.type(await screen.findByRole('textbox', { name: '주소 검색' }), '신림');
    await user.click(screen.getByRole('button', { name: '검색' }));

    const results = await screen.findByRole('list', { name: '주소 검색 결과' });
    expect(within(results).getByText(property.roadAddress)).toBeInTheDocument();
    expect(within(results).getByText(property.jibunAddress)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '이 위치로 매물 등록하기' })).toBeEnabled();
  });

  it('초기 2km 집계, 반경 재조회와 카테고리 빈 상태를 표시한다', async () => {
    const user = userEvent.setup();
    const requestedRadii: string[] = [];
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () => HttpResponse.json(successEnvelope(property))),
      http.get(`${config.apiBaseUrl}/api/maps/nearby`, ({ request }) => {
        const radius = new URL(request.url).searchParams.get('radius') ?? '';
        requestedRadii.push(radius);
        return HttpResponse.json(
          successEnvelope({
            center: { latitude: property.latitude, longitude: property.longitude },
            radius: Number(radius),
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
          }),
        );
      }),
    );

    renderAuthenticated('/properties/10/nearby');

    expect(await screen.findByRole('heading', { name: '신림역 원룸 주변은 어때요?' })).toBeInTheDocument();
    expect(await screen.findByRole('list', { name: '주변 시설 목록' })).toHaveTextContent('신림 안심의원');
    await waitFor(() => expect(requestedRadii).toContain('2000'));

    await user.click(screen.getByRole('button', { name: '500m' }));
    await waitFor(() => expect(requestedRadii).toContain('500'));

    await user.click(screen.getByRole('button', { name: /학교\s*0/ }));
    expect(await screen.findByText('이 반경에는 표시할 시설이 없어요')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /전체\s*1/ }));
    expect(screen.getByText('반경 2km')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '매물 지도로 돌아가기' })).toHaveAttribute('href', '/map');
  });
});
