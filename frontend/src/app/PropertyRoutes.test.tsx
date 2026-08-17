import { QueryClientProvider } from '@tanstack/react-query';
import { StrictMode } from 'react';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { HttpResponse, delay, http } from 'msw';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { setAuthentication } from './authStore';
import { propertyQueryKeys } from './propertyQueryKeys';
import { queryClient } from './queryClient';
import AppRoutes from './AppRoutes';
import { server } from '../test/server';
import {
  errorEnvelope,
  memberFixture,
  photoFixture,
  propertyDetailFixture,
  propertyPageFixture,
  propertySummaryFixture,
  secondPropertySummaryFixture,
  successEnvelope,
} from '../test/propertyFixtures';
import type { PublicConfig } from '../types/PublicConfig';

const config: PublicConfig = {
  apiBaseUrl: 'http://localhost:8080',
  googleClientId: 'test-client',
  googleRedirectUri: 'http://localhost:3000/oauth/google/callback',
};

const detailWithoutPhotos = {
  ...propertyDetailFixture,
  recentVisit: null,
  activeChecklists: [],
  photoCount: 0,
  photoPreview: { totalCount: 0, photos: [] },
  deletionImpact: { ...propertyDetailFixture.deletionImpact, photoCount: 0 },
};

const renderAuthenticated = (path: string) => {
  setAuthentication({ accessToken: 'memory-token', tokenType: 'Bearer', expiresIn: 60 });
  server.use(http.get(`${config.apiBaseUrl}/api/members/me`, () => HttpResponse.json(successEnvelope(memberFixture))));

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

describe('FE-2 매물 목록', () => {
  it('매물 0개와 검색 결과 없음을 구분한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties`, () => {
        return HttpResponse.json(successEnvelope(propertyPageFixture([], 0, false)));
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties');

    expect(await screen.findByText('아직 등록한 매물이 없어요.')).toBeInTheDocument();
    await user.type(screen.getByRole('textbox', { name: '매물 이름 검색' }), '없는 매물{Enter}');
    expect(await screen.findByText('검색 결과가 없어요.')).toBeInTheDocument();
  });

  it('매물 2개 이상과 최근 방문 있음·없음을 표시한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties`, () =>
        HttpResponse.json(successEnvelope(propertyPageFixture([propertySummaryFixture, secondPropertySummaryFixture]))),
      ),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties');

    expect(await screen.findByRole('link', { name: '신림역 원룸' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '망원동 투룸' })).toBeInTheDocument();
    expect(screen.queryByText('방문 완료')).not.toBeInTheDocument();
    expect(screen.getAllByText('미완료')).toHaveLength(2);
    expect(screen.getByRole('list', { name: '최근 방문 결과 집계' })).toHaveTextContent('괜찮음 10');
    expect(screen.getByRole('list', { name: '최근 방문 결과 집계' })).toHaveTextContent('주의 5');
    expect(screen.getByRole('list', { name: '최근 방문 결과 집계' })).toHaveTextContent('미확인 7');
    expect(screen.getByText('아직 방문 확인 기록이 없어요.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '완료' }));
    expect(screen.getByRole('link', { name: '신림역 원룸' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: '망원동 투룸' })).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '미완료' }));
    expect(screen.queryByRole('link', { name: '신림역 원룸' })).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: '망원동 투룸' })).toBeInTheDocument();
  });

  it('전체 개수와 서버가 반환한 순서를 목록에 반영한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties`, () =>
        HttpResponse.json(
          successEnvelope(
            propertyPageFixture([
              { ...propertySummaryFixture, lastActivityAt: '2026-08-12T07:30:00Z' },
              { ...secondPropertySummaryFixture, lastActivityAt: '2026-08-09T07:30:00Z' },
            ]),
          ),
        ),
      ),
    );
    renderAuthenticated('/properties');

    expect(
      await screen.findByText((_, element) => element?.tagName === 'SPAN' && element.textContent === '전체 2'),
    ).toBeInTheDocument();
    const list = screen.getByRole('region', { name: '매물 목록' });
    expect(
      within(list)
        .getAllByRole('link')
        .map((link) => link.getAttribute('aria-label')),
    ).toEqual(['신림역 원룸', '망원동 투룸']);
  });

  it('검색 변경 시 늦은 이전 응답이 새 검색 결과를 덮어쓰지 않는다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties`, async ({ request }) => {
        const query = new URL(request.url).searchParams.get('query');
        if (query === '느린 검색') {
          await delay(120);
          return HttpResponse.json(successEnvelope(propertyPageFixture([propertySummaryFixture])));
        }
        if (query === '망원')
          return HttpResponse.json(successEnvelope(propertyPageFixture([secondPropertySummaryFixture])));
        return HttpResponse.json(successEnvelope(propertyPageFixture([])));
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties');
    const searchbox = await screen.findByRole('textbox', { name: '매물 이름 검색' });

    await user.type(searchbox, '느린 검색');
    await user.keyboard('{Enter}');
    await user.clear(searchbox);
    await user.type(searchbox, '  망원  ');
    await user.keyboard('{Enter}');

    expect(await screen.findByRole('link', { name: '망원동 투룸' })).toBeInTheDocument();
    await delay(150);
    expect(screen.queryByRole('link', { name: '신림역 원룸' })).not.toBeInTheDocument();
  });

  it('다음 페이지를 이어 붙이고 추가 조회 실패를 기존 목록과 분리한다', async () => {
    let pageOneAttempts = 0;
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties`, ({ request }) => {
        const page = new URL(request.url).searchParams.get('page');
        if (page === '1') {
          pageOneAttempts += 1;
          if (pageOneAttempts === 1) return HttpResponse.error();
          return HttpResponse.json(successEnvelope(propertyPageFixture([secondPropertySummaryFixture], 1, false)));
        }
        return HttpResponse.json(successEnvelope(propertyPageFixture([propertySummaryFixture], 0, true)));
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties');

    expect(await screen.findByRole('link', { name: '신림역 원룸' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '매물 더 보기' }));
    expect(await screen.findByText(/기존 목록은 그대로 유지/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '신림역 원룸' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '다시 불러오기' }));
    expect(await screen.findByRole('link', { name: '망원동 투룸' })).toBeInTheDocument();
  });

  it('최초 목록 실패 후 재시도할 수 있다', async () => {
    let shouldFail = true;
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties`, () => {
        return shouldFail
          ? HttpResponse.error()
          : HttpResponse.json(successEnvelope(propertyPageFixture([propertySummaryFixture])));
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties');

    expect(await screen.findByText('매물 목록을 불러오지 못했어요.')).toBeInTheDocument();
    shouldFail = false;
    await user.click(screen.getByRole('button', { name: '다시 시도' }));
    expect(await screen.findByRole('link', { name: '신림역 원룸' })).toBeInTheDocument();
  });
});

describe('FE-2 등록·수정·메모', () => {
  it('등록 필수값을 검증하고 성공 후 생성된 상세로 이동한다', async () => {
    let requestBody: unknown;
    server.use(
      http.post(`${config.apiBaseUrl}/api/properties`, async ({ request }) => {
        requestBody = await request.json();
        return HttpResponse.json(
          successEnvelope({
            propertyId: 10,
            name: '신림역 원룸',
            depositAmount: 0,
            monthlyRentAmount: 550_000,
            discoverySource: { type: 'TEXT', value: '중개사 추천' },
            createdAt: '2026-08-10T07:30:00Z',
          }),
          { status: 201 },
        );
      }),
      http.get(`${config.apiBaseUrl}/api/properties/10`, () =>
        HttpResponse.json(
          successEnvelope({
            ...detailWithoutPhotos,
            depositAmount: 0,
            discoverySource: { type: 'TEXT', value: '중개사 추천' },
          }),
        ),
      ),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties/new');

    await user.click(await screen.findByRole('button', { name: '매물 등록' }));
    expect(screen.getByText('매물을 구분할 이름을 입력해 주세요.')).toBeInTheDocument();

    await user.type(screen.getByLabelText('이름'), ' 신림역 원룸 ');
    await user.type(screen.getByLabelText('보증금'), '0');
    await user.type(screen.getByLabelText('월세'), '550000');
    await user.type(screen.getByLabelText('확인한 곳'), ' 중개사 추천 ');
    await user.click(screen.getByRole('button', { name: '매물 등록' }));

    expect(await screen.findByRole('heading', { name: '신림역 원룸', level: 1 })).toBeInTheDocument();
    expect(requestBody).toEqual({
      name: '신림역 원룸',
      depositAmount: 0,
      monthlyRentAmount: 550_000,
      discoverySource: '중개사 추천',
    });
  });

  it('등록 실패 시 입력을 유지하고 서버 validation 필드를 가까이 표시한다', async () => {
    server.use(
      http.post(`${config.apiBaseUrl}/api/properties`, () =>
        HttpResponse.json(errorEnvelope('INVALID_REQUEST', [{ field: 'name', reason: 'internal validation' }]), {
          status: 400,
        }),
      ),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties/new');

    await user.type(await screen.findByLabelText('이름'), '유지할 이름');
    await user.type(screen.getByLabelText('보증금'), '1000');
    await user.type(screen.getByLabelText('월세'), '55');
    await user.type(screen.getByLabelText('확인한 곳'), 'https://example.com');
    await user.click(screen.getByRole('button', { name: '매물 등록' }));

    expect(await screen.findByText('서버에서 매물 이름을 확인하지 못했습니다.')).toBeInTheDocument();
    expect(screen.getByLabelText('이름')).toHaveValue('유지할 이름');
    expect(document.body.textContent).not.toContain('internal validation');
  });

  it('수정은 변경된 필드만 보내고 변경이 없으면 API를 호출하지 않는다', async () => {
    let patchCalls = 0;
    let patchBody: unknown;
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () => HttpResponse.json(successEnvelope(detailWithoutPhotos))),
      http.patch(`${config.apiBaseUrl}/api/properties/10`, async ({ request }) => {
        patchCalls += 1;
        patchBody = await request.json();
        return HttpResponse.json(
          successEnvelope({
            propertyId: 10,
            name: propertyDetailFixture.name,
            depositAmount: propertyDetailFixture.depositAmount,
            monthlyRentAmount: 530_000,
            discoverySource: propertyDetailFixture.discoverySource,
            updatedAt: '2026-08-11T01:00:00Z',
          }),
        );
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties/10/edit');

    await user.click(await screen.findByRole('button', { name: '변경사항 저장' }));
    expect(screen.getByText(/변경된 내용이 없어/)).toBeInTheDocument();
    expect(patchCalls).toBe(0);

    await user.clear(screen.getByLabelText('월세'));
    await user.type(screen.getByLabelText('월세'), '530000');
    await user.click(screen.getByRole('button', { name: '변경사항 저장' }));
    expect(await screen.findByRole('heading', { name: '신림역 원룸', level: 1 })).toBeInTheDocument();
    expect(patchBody).toEqual({ monthlyRentAmount: 530_000 });
  });

  it('기존 구조화 메모를 하나의 자유 메모로 합쳐 표시한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () => HttpResponse.json(successEnvelope(detailWithoutPhotos))),
    );
    renderAuthenticated('/properties/10');

    const memo = await screen.findByRole('textbox', { name: '메모' });
    const memoValue = (memo as HTMLTextAreaElement).value;
    expect(memoValue).toContain('채광 다시 확인');
    expect(memoValue).toContain('방 보러 가는 일정: 8월 20일 오후 2시 방문');
    expect(memoValue).toContain('입주 가능일: 9월 1일부터 입주 가능');
    expect(memoValue).toContain('정부 지원금 가능 종류: 중소기업 청년 대출 가능 여부 확인');
    expect(screen.getByRole('button', { name: '메모 저장' })).toBeDisabled();
  });

  it('저장 이력이 없는 빈 메모의 초기 상태를 안내한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () =>
        HttpResponse.json(
          successEnvelope({
            ...detailWithoutPhotos,
            memo: {
              viewingSchedule: '',
              moveInAvailability: '',
              provisionalDeposit: '',
              roomOptions: '',
              maintenanceAndUtilities: '',
              commuteTime: '',
              governmentSupport: '',
              additionalMemo: '',
              content: '',
              savedAt: null,
            },
          }),
        ),
      ),
    );
    renderAuthenticated('/properties/10');

    expect(await screen.findByText('아직 저장한 메모가 없어요.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '메모 저장' })).toBeDisabled();
  });

  it('자유 메모로 저장하고 실패 시 작성 내용을 유지해 재시도한다', async () => {
    let saveAttempts = 0;
    const bodies: unknown[] = [];
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () => HttpResponse.json(successEnvelope(detailWithoutPhotos))),
      http.put(`${config.apiBaseUrl}/api/properties/10/memo`, async ({ request }) => {
        saveAttempts += 1;
        const body = (await request.json()) as Record<string, string>;
        bodies.push(body);
        if (saveAttempts === 1) return HttpResponse.json(errorEnvelope('INTERNAL_SERVER_ERROR'), { status: 500 });
        return HttpResponse.json(
          successEnvelope({
            ...body,
            content: body.additionalMemo,
            savedAt: '2026-08-11T01:00:00Z',
          }),
        );
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties/10');
    const memo = await screen.findByRole('textbox', { name: '메모' });

    await user.clear(memo);
    await user.type(memo, '8월 21일 오후 3시에 방문');
    await user.click(screen.getByRole('button', { name: '메모 저장' }));
    expect(await screen.findByText(/작성 중인 내용은 유지/)).toBeInTheDocument();
    expect(memo).toHaveValue('8월 21일 오후 3시에 방문');

    await user.click(screen.getByRole('button', { name: '다시 저장' }));
    expect(await screen.findByText(/저장했어요. 마지막 저장/)).toBeInTheDocument();
    const expectedBody = {
      viewingSchedule: '',
      moveInAvailability: '',
      provisionalDeposit: '',
      roomOptions: '',
      maintenanceAndUtilities: '',
      commuteTime: '',
      governmentSupport: '',
      additionalMemo: '8월 21일 오후 3시에 방문',
    };
    expect(bodies).toEqual([expectedBody, expectedBody]);
    expect(bodies[1]).not.toHaveProperty('content');
    expect(bodies[1]).not.toHaveProperty('expectedVersion');
    expect(queryClient.getQueryData(propertyQueryKeys.detail(10))).toMatchObject({
      memo: { ...expectedBody, content: expectedBody.additionalMemo, savedAt: '2026-08-11T01:00:00Z' },
      updatedAt: '2026-08-11T01:00:00Z',
      lastActivityAt: '2026-08-11T01:00:00Z',
    });
  });

  it('저장 중에는 중복 저장과 입력 변경을 막고 진행 상태를 알린다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () => HttpResponse.json(successEnvelope(detailWithoutPhotos))),
      http.put(`${config.apiBaseUrl}/api/properties/10/memo`, async ({ request }) => {
        const body = (await request.json()) as Record<string, string>;
        await delay(80);
        return HttpResponse.json(
          successEnvelope({
            ...body,
            content: body.additionalMemo,
            savedAt: '2026-08-11T01:00:00Z',
          }),
        );
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties/10');
    const memo = await screen.findByRole('textbox', { name: '메모' });

    await user.type(memo, ' 변경');
    await user.click(screen.getByRole('button', { name: '메모 저장' }));

    expect(screen.getByText('퀵 메모를 저장하고 있어요…')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '메모 저장 중…' })).toBeDisabled();
    expect(memo).toHaveAttribute('readonly');
    expect(await screen.findByText(/저장했어요. 마지막 저장/)).toBeInTheDocument();
  });

  it('자유 메모 5,000자를 Unicode 코드포인트 기준으로 검증한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () => HttpResponse.json(successEnvelope(detailWithoutPhotos))),
    );
    renderAuthenticated('/properties/10');
    const memo = await screen.findByRole('textbox', { name: '메모' });
    const saveButton = screen.getByRole('button', { name: '메모 저장' });

    fireEvent.change(memo, { target: { value: '🏠'.repeat(5_001) } });
    expect(screen.getByText('5,000자 이하로 입력해 주세요.')).toBeInTheDocument();
    expect(saveButton).toBeDisabled();

    fireEvent.change(memo, { target: { value: '🏠'.repeat(5_000) } });
    expect(saveButton).toBeEnabled();
  });

  it('작성 중 상세가 다시 조회되어도 서버 값으로 폼을 덮어쓰지 않는다', async () => {
    let detailCalls = 0;
    let memo = detailWithoutPhotos.memo;
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () => {
        detailCalls += 1;
        return HttpResponse.json(successEnvelope({ ...detailWithoutPhotos, memo }));
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties/10');
    const memoField = await screen.findByRole('textbox', { name: '메모' });

    await user.clear(memoField);
    await user.type(memoField, '작성 중인 메모');
    memo = {
      ...memo,
      viewingSchedule: '다른 곳에서 저장된 일정',
      moveInAvailability: '다른 곳에서 저장된 입주 가능일',
      savedAt: '2026-08-11T02:00:00Z',
    };
    await queryClient.invalidateQueries({ queryKey: propertyQueryKeys.detail(10), exact: true });
    await waitFor(() => expect(detailCalls).toBeGreaterThan(1));

    expect(memoField).toHaveValue('작성 중인 메모');
    expect(screen.getByText('저장되지 않은 변경사항이 있어요.')).toBeInTheDocument();
  });
});

describe('FE-2 사진과 삭제 확인', () => {
  it('상세 사진은 큰 화면에서 넘겨 보고 수정 화면에서는 관리한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () =>
        HttpResponse.json(successEnvelope(propertyDetailFixture)),
      ),
      http.get(`${config.apiBaseUrl}/api/properties/10/photos`, () =>
        HttpResponse.json(
          successEnvelope({
            photos: [
              photoFixture,
              { ...photoFixture, photoId: 82, contentUrl: '/api/properties/10/photos/82/content' },
            ],
            totalCount: 2,
          }),
        ),
      ),
      http.get(
        `${config.apiBaseUrl}/api/properties/10/photos/81/content`,
        () => new HttpResponse(new Uint8Array([255, 216, 255]), { headers: { 'Content-Type': 'image/jpeg' } }),
      ),
      http.get(
        `${config.apiBaseUrl}/api/properties/10/photos/82/content`,
        () => new HttpResponse(new Uint8Array([255, 216, 254]), { headers: { 'Content-Type': 'image/jpeg' } }),
      ),
    );

    const user = userEvent.setup();
    const detailView = renderAuthenticated('/properties/10');
    const photoButton = await screen.findByRole('button', { name: '신림역 원룸 사진 1 크게 보기' });
    await user.click(photoButton);

    expect(await screen.findByRole('dialog', { name: '신림역 원룸 사진 크게 보기' })).toBeInTheDocument();
    expect(await screen.findByRole('img', { name: '신림역 원룸 사진 1' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '다음 사진' }));
    expect(await screen.findByRole('img', { name: '신림역 원룸 사진 2' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '사진 크게 보기 닫기' }));
    expect(screen.queryByText('사진 관리')).not.toBeInTheDocument();
    detailView.unmount();

    renderAuthenticated('/properties/10/edit');
    expect(await screen.findByRole('heading', { name: '사진 관리' })).toBeInTheDocument();
    expect(screen.getByLabelText('사진 파일 선택')).toBeEnabled();
    expect(await screen.findByRole('img', { name: '업로드 순 1번째 사진' })).toBeInTheDocument();
  });

  it('빈 사진 목록과 잘못된 형식·10MiB 초과를 업로드 전에 안내한다', async () => {
    let uploadCalls = 0;
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () => HttpResponse.json(successEnvelope(detailWithoutPhotos))),
      http.get(`${config.apiBaseUrl}/api/properties/10/photos`, () =>
        HttpResponse.json(successEnvelope({ photos: [], totalCount: 0 })),
      ),
      http.post(`${config.apiBaseUrl}/api/properties/10/photos`, () => {
        uploadCalls += 1;
        return HttpResponse.json(successEnvelope(photoFixture), { status: 201 });
      }),
    );
    const user = userEvent.setup({ applyAccept: false });
    renderAuthenticated('/properties/10/photos');

    expect(await screen.findByText('등록한 사진이 없어요.')).toBeInTheDocument();
    const input = screen.getByLabelText(/사진 추가|파일 선택/, { selector: 'input[type="file"]' });
    await user.upload(input, [
      new File(['text'], 'wrong.txt', { type: 'text/plain' }),
      new File([new Uint8Array(10 * 1024 * 1024 + 1)], 'large.jpg', { type: 'image/jpeg' }),
    ]);

    expect(await screen.findByText('JPEG, PNG 또는 WebP 사진을 선택해 주세요.')).toBeInTheDocument();
    expect(screen.getByText('사진 한 장은 10MiB 이하만 등록할 수 있습니다.')).toBeInTheDocument();
    expect(uploadCalls).toBe(0);
  });

  it('사진 30장 제한에 도달하면 파일 선택을 비활성화한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () => HttpResponse.json(successEnvelope(detailWithoutPhotos))),
      http.get(`${config.apiBaseUrl}/api/properties/10/photos`, () =>
        HttpResponse.json(successEnvelope({ photos: [], totalCount: 30 })),
      ),
    );
    renderAuthenticated('/properties/10/photos');

    expect(await screen.findByText('사진 30장이 모두 등록되어 추가할 수 없어요.')).toBeInTheDocument();
    expect(screen.getByLabelText('사진 파일 선택')).toBeDisabled();
  });

  it('다중 파일을 순차 처리하고 일부 실패 후에도 나머지를 계속한다', async () => {
    let activeUploads = 0;
    let maxActiveUploads = 0;
    let uploadCalls = 0;
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () => HttpResponse.json(successEnvelope(detailWithoutPhotos))),
      http.get(`${config.apiBaseUrl}/api/properties/10/photos`, () =>
        HttpResponse.json(successEnvelope({ photos: [], totalCount: 0 })),
      ),
      http.post(`${config.apiBaseUrl}/api/properties/10/photos`, async () => {
        uploadCalls += 1;
        activeUploads += 1;
        maxActiveUploads = Math.max(maxActiveUploads, activeUploads);
        await delay(10);
        activeUploads -= 1;
        if (uploadCalls === 2) return HttpResponse.json(errorEnvelope('PHOTO_UPLOAD_FAILED'), { status: 500 });
        return HttpResponse.json(successEnvelope({ ...photoFixture, photoId: 80 + uploadCalls }), { status: 201 });
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties/10/photos');
    const input = await screen.findByLabelText(/사진 추가|파일 선택/, { selector: 'input[type="file"]' });

    await user.upload(
      input,
      [1, 2, 3].map((number) => new File([new Uint8Array([number])], `photo-${number}.jpg`, { type: 'image/jpeg' })),
    );
    await waitFor(() => expect(uploadCalls).toBe(3));
    expect(screen.queryByText('업로드 결과')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '결과 지우기' })).not.toBeInTheDocument();
    expect(screen.getByText('사진을 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.')).toBeInTheDocument();
    expect(uploadCalls).toBe(3);
    expect(maxActiveUploads).toBe(1);
  });

  it('사진 삭제는 취소·성공을 구분하고 성공 전에는 화면에서 제거하지 않는다', async () => {
    let deleted = false;
    let deleteCalls = 0;
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () =>
        HttpResponse.json(successEnvelope(propertyDetailFixture)),
      ),
      http.get(`${config.apiBaseUrl}/api/properties/10/photos`, () =>
        HttpResponse.json(successEnvelope({ photos: deleted ? [] : [photoFixture], totalCount: deleted ? 0 : 1 })),
      ),
      http.get(
        `${config.apiBaseUrl}/api/properties/10/photos/81/content`,
        () => new HttpResponse(new Uint8Array([255, 216, 255]), { headers: { 'Content-Type': 'image/jpeg' } }),
      ),
      http.delete(`${config.apiBaseUrl}/api/properties/10/photos/81`, () => {
        deleteCalls += 1;
        deleted = true;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties/10/photos');

    const deleteButton = await screen.findByRole('button', { name: '업로드 순 1번째 사진 삭제' });
    expect(screen.queryByRole('img', { name: '삭제할 업로드 순 1번째 사진' })).not.toBeInTheDocument();
    await user.click(deleteButton);
    const dialog = screen.getByRole('dialog', { name: '이 사진을 삭제할까요?' });
    expect(within(dialog).getByRole('img', { name: '삭제할 업로드 순 1번째 사진' })).toBeInTheDocument();
    expect(within(dialog).getByRole('button', { name: '취소' })).toHaveFocus();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(deleteButton).toHaveFocus();
    expect(screen.queryByRole('img', { name: '삭제할 업로드 순 1번째 사진' })).not.toBeInTheDocument();

    await user.click(deleteButton);
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: '취소' }));
    expect(deleteCalls).toBe(0);
    expect(screen.getByRole('img', { name: '업로드 순 1번째 사진' })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '업로드 순 1번째 사진 삭제' }));
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: '사진 삭제' }));
    expect(await screen.findByText('등록한 사진이 없어요.')).toBeInTheDocument();
    expect(deleteCalls).toBe(1);
  });

  it('사진 삭제 실패는 사진을 유지하고 안전한 재시도 오류를 표시한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () =>
        HttpResponse.json(successEnvelope(propertyDetailFixture)),
      ),
      http.get(`${config.apiBaseUrl}/api/properties/10/photos`, () =>
        HttpResponse.json(successEnvelope({ photos: [photoFixture], totalCount: 1 })),
      ),
      http.get(
        `${config.apiBaseUrl}/api/properties/10/photos/81/content`,
        () => new HttpResponse(new Uint8Array([255, 216, 255]), { headers: { 'Content-Type': 'image/jpeg' } }),
      ),
      http.delete(`${config.apiBaseUrl}/api/properties/10/photos/81`, () =>
        HttpResponse.json(errorEnvelope('PHOTO_DELETE_FAILED'), { status: 500 }),
      ),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties/10/photos');

    await user.click(await screen.findByRole('button', { name: '업로드 순 1번째 사진 삭제' }));
    const dialog = screen.getByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: '사진 삭제' }));
    expect(await within(dialog).findByText(/사진은 화면에서 제거하지 않았어요/)).toBeInTheDocument();
    expect(screen.getByRole('img', { name: '업로드 순 1번째 사진' })).toBeInTheDocument();
  });

  it('매물 삭제 영향·취소·성공을 처리하고 목록으로 이동한다', async () => {
    let deleteCalls = 0;
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () => HttpResponse.json(successEnvelope(detailWithoutPhotos))),
      http.delete(`${config.apiBaseUrl}/api/properties/10`, () => {
        deleteCalls += 1;
        return new HttpResponse(null, { status: 204 });
      }),
      http.get(`${config.apiBaseUrl}/api/properties`, () =>
        HttpResponse.json(successEnvelope(propertyPageFixture([]))),
      ),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties/10');

    await user.click(await screen.findByRole('button', { name: '매물 삭제' }));
    const dialog = screen.getByRole('dialog', { name: '신림역 원룸 매물을 삭제할까요?' });
    expect(within(dialog).getByText('방문 2개')).toBeInTheDocument();
    expect(within(dialog).getByText('활성 연결 1개')).toBeInTheDocument();
    await user.click(within(dialog).getByRole('button', { name: '취소' }));
    expect(deleteCalls).toBe(0);

    await user.click(screen.getByRole('button', { name: '매물 삭제' }));
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: '매물 삭제' }));
    expect(await screen.findByRole('heading', { name: '내 매물' })).toHaveFocus();
    expect(deleteCalls).toBe(1);
  });

  it('PHOTO_DELETE_FAILED인 매물은 상세에 유지한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () => HttpResponse.json(successEnvelope(detailWithoutPhotos))),
      http.delete(`${config.apiBaseUrl}/api/properties/10`, () =>
        HttpResponse.json(errorEnvelope('PHOTO_DELETE_FAILED'), { status: 500 }),
      ),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties/10');

    await user.click(await screen.findByRole('button', { name: '매물 삭제' }));
    const dialog = screen.getByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: '매물 삭제' }));
    expect(await within(dialog).findByText(/매물은 그대로 유지/)).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: '신림역 원룸', level: 1 })).toBeInTheDocument();
  });

  it('PROPERTY_NOT_FOUND를 네트워크 오류와 구분해 목록 이동을 제공한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/99`, () =>
        HttpResponse.json(errorEnvelope('PROPERTY_NOT_FOUND'), { status: 404 }),
      ),
    );
    renderAuthenticated('/properties/99');

    expect(await screen.findByText('매물을 찾을 수 없어요.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '매물 목록으로 돌아가기' })).toHaveAttribute('href', '/properties');
    expect(screen.queryByRole('button', { name: '다시 시도' })).not.toBeInTheDocument();
  });
});
