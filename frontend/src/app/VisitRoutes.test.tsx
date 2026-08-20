import { QueryClientProvider } from '@tanstack/react-query';
import { StrictMode } from 'react';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { HttpResponse, delay, http } from 'msw';
import { unstable_HistoryRouter as HistoryRouter, UNSAFE_createMemoryHistory } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { errorEnvelope, memberFixture, propertyDetailFixture, successEnvelope } from '../test/propertyFixtures';
import { server } from '../test/server';
import { visitDetailFixture, visitPageFixture } from '../test/visitFixtures';
import type { PublicConfig } from '../types/PublicConfig';
import AppRoutes from './AppRoutes';
import { setAuthentication } from './authStore';
import { createGuardedHistory } from './guardedHistory';
import { queryClient } from './queryClient';
import { visitQueryKeys } from './visitQueryKeys';

const config: PublicConfig = {
  apiBaseUrl: 'http://localhost:8080',
  googleClientId: 'test-client',
  googleRedirectUri: 'http://localhost:3000/oauth/google/callback',
};

const renderAuthenticated = (path: string, history?: { initialEntries: string[]; initialIndex: number }) => {
  setAuthentication({ accessToken: 'memory-token', tokenType: 'Bearer', expiresIn: 60 });
  server.use(http.get(`${config.apiBaseUrl}/api/members`, () => HttpResponse.json(successEnvelope(memberFixture))));
  const guardedHistory = createGuardedHistory(
    UNSAFE_createMemoryHistory({
      initialEntries: history?.initialEntries ?? [path],
      initialIndex: history?.initialIndex,
      v5Compat: true,
    }),
  );
  const result = render(
    <StrictMode>
      <QueryClientProvider client={queryClient}>
        <HistoryRouter history={guardedHistory}>
          <AppRoutes config={config} storage={window.sessionStorage} />
        </HistoryRouter>
      </QueryClientProvider>
    </StrictMode>,
  );
  return { ...result, history: guardedHistory };
};

const useVisitDetailHandlers = (getVisit: () => unknown = () => visitDetailFixture) => {
  server.use(
    http.get(`${config.apiBaseUrl}/api/visits/31`, () => HttpResponse.json(successEnvelope(getVisit()))),
    http.get(`${config.apiBaseUrl}/api/properties/10`, () => HttpResponse.json(successEnvelope(propertyDetailFixture))),
    http.get(`${config.apiBaseUrl}/api/properties`, () =>
      HttpResponse.json(
        successEnvelope({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, hasNext: false }),
      ),
    ),
    http.get(`${config.apiBaseUrl}/api/properties/10/visits`, () =>
      HttpResponse.json(successEnvelope(visitPageFixture([]))),
    ),
  );
};

const statusUpdateResult = (visitItemId: number, status: 'GOOD' | 'CAUTION' | 'UNCONFIRMED', version: number) => ({
  item: {
    visitItemId,
    status,
    statusVersion: version,
    statusSavedAt: `2026-08-11T04:0${Math.min(version + 1, 9)}:00Z`,
    version,
    savedAt: `2026-08-11T04:0${Math.min(version + 1, 9)}:00Z`,
  },
  stageSummary: { totalCount: 1, checkedCount: 1, goodCount: 1, cautionCount: 0, unconfirmedCount: 0 },
  visitSummary: { totalCount: 3, checkedCount: 2, goodCount: 2, cautionCount: 0, unconfirmedCount: 1 },
});

const memoUpdateResult = (visitItemId: number, memo: string, version: number) => ({
  visitItemId,
  memo,
  memoVersion: version,
  memoSavedAt: `2026-08-11T04:0${Math.min(version + 1, 9)}:30Z`,
});

const openFirstMemo = async () => {
  const opened = screen.queryAllByRole('textbox', { name: '한 줄 메모' })[0];
  if (opened !== undefined) return opened;

  const toggle = (await screen.findAllByRole('button', { name: /메모 열기$/ }))[0];
  if (toggle === undefined) throw new Error('메모 열기 버튼을 찾지 못했습니다.');
  await userEvent.setup().click(toggle);
  const memo = screen.getAllByRole('textbox', { name: '한 줄 메모' })[0];
  if (memo === undefined) throw new Error('메모 입력을 찾지 못했습니다.');
  return memo;
};

describe('FE-4 방문 시작과 목록', () => {
  it('연결된 체크리스트는 상세에서 체크 화면 링크로 제공한다', async () => {
    let startCalls = 0;
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () =>
        HttpResponse.json(
          successEnvelope({
            ...propertyDetailFixture,
            activeChecklists: [{ stage: 'ONLINE_PHONE', checklistId: 7, name: '전화 문의 기본 목록', itemCount: 2 }],
            recentVisit: null,
          }),
        ),
      ),
      http.post(`${config.apiBaseUrl}/api/properties/10/visits`, () => {
        startCalls += 1;
        return HttpResponse.json(successEnvelope(visitDetailFixture), { status: 201 });
      }),
      http.get(`${config.apiBaseUrl}/api/visits/31`, () => HttpResponse.json(successEnvelope(visitDetailFixture))),
      http.get(`${config.apiBaseUrl}/api/properties/10/checklists`, () =>
        HttpResponse.json(
          successEnvelope({
            propertyId: 10,
            overallProgress: {
              totalCount: 2,
              completedCount: 0,
              goodCount: 0,
              cautionCount: 0,
              unconfirmedCount: 2,
              progressRate: 0,
            },
            stages: [
              {
                stage: 'ONLINE_PHONE',
                applied: true,
                propertyChecklistId: 47,
                checklistName: '전화 문의 기본 목록',
                sourceChecklistId: 7,
                progress: {
                  totalCount: 2,
                  completedCount: 0,
                  goodCount: 0,
                  cautionCount: 0,
                  unconfirmedCount: 2,
                  progressRate: 0,
                },
              },
              ...(['ON_SITE', 'PRE_CONTRACT'] as const).map((stage) => ({
                stage,
                applied: false,
                propertyChecklistId: null,
                checklistName: null,
                sourceChecklistId: null,
                progress: {
                  totalCount: 0,
                  completedCount: 0,
                  goodCount: 0,
                  cautionCount: 0,
                  unconfirmedCount: 0,
                  progressRate: 0,
                },
              })),
            ],
          }),
        ),
      ),
      http.get(
        `${config.apiBaseUrl}/api/properties/10/photos/81/content`,
        () => new HttpResponse(new Uint8Array([255, 216, 255]), { headers: { 'Content-Type': 'image/jpeg' } }),
      ),
    );
    renderAuthenticated('/properties/10');

    expect(await screen.findByRole('link', { name: /온라인·전화.*전화 문의 기본 목록/ })).toHaveAttribute(
      'href',
      '/properties/10/checklists/47',
    );
    expect(startCalls).toBe(0);
  });

  it('복수 방문을 표시하고 다음 페이지 실패를 기존 기록과 분리한다', async () => {
    let pageOneAttempts = 0;
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10`, () =>
        HttpResponse.json(successEnvelope(propertyDetailFixture)),
      ),
      http.get(`${config.apiBaseUrl}/api/properties/10/visits`, ({ request }) => {
        if (new URL(request.url).searchParams.get('page') === '1') {
          pageOneAttempts += 1;
          if (pageOneAttempts === 1) return HttpResponse.error();
          return HttpResponse.json(
            successEnvelope(
              visitPageFixture(
                [{ ...propertyDetailFixture.recentVisit, visitId: 29, status: 'IN_PROGRESS', completedAt: null }],
                1,
              ),
            ),
          );
        }
        return HttpResponse.json(successEnvelope(visitPageFixture([propertyDetailFixture.recentVisit], 0, true)));
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/properties/10/visits');

    expect(await screen.findByRole('link', { name: /방문 #31/ })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '방문 더 보기' }));
    expect(await screen.findByText(/기존 기록은 유지/)).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '다시 불러오기' }));
    await waitFor(() => expect(screen.getByRole('link', { name: /방문 #29/ })).toHaveAttribute('href', '/visits/29'));
  });

  it('다른 회원 소유이거나 없는 방문의 404를 안전한 안내로 처리한다', async () => {
    server.use(
      http.get(`${config.apiBaseUrl}/api/visits/404`, () =>
        HttpResponse.json(errorEnvelope('VISIT_NOT_FOUND'), { status: 404 }),
      ),
    );
    renderAuthenticated('/visits/404');

    expect(await screen.findByRole('alert')).toHaveTextContent('방문 기록을 찾을 수 없어요.');
    expect(screen.queryByText('서버 내부 상세 메시지')).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: '매물 목록으로 돌아가기' })).toBeInTheDocument();
  });
});

describe('FE-4 항목 자동 저장', () => {
  it('연결되지 않은 단계도 열어 체크리스트 선택 화면으로 이동할 수 있다', async () => {
    useVisitDetailHandlers();
    renderAuthenticated('/visits/31?stage=PRE_CONTRACT');

    expect(await screen.findByRole('tab', { name: '계약 전' })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByText('이 단계에 연결된 체크리스트가 없어요.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '체크리스트 선택하러 가기' })).toHaveAttribute(
      'href',
      '/properties/10/active-checklists/PRE_CONTRACT',
    );
    expect(screen.queryByRole('button', { name: '체크 완료 및 저장' })).not.toBeInTheDocument();
  });

  it('상태 선택 즉시 정본 version만 보내고 같은 값을 다시 저장해도 증가한 version을 반영한다', async () => {
    const requests: { visitItemId: number; body: unknown }[] = [];
    useVisitDetailHandlers();
    server.use(
      http.patch(`${config.apiBaseUrl}/api/visits/31/items/:visitItemId`, async ({ params, request }) => {
        const visitItemId = Number(params.visitItemId);
        const body = await request.json();
        requests.push({ visitItemId, body });
        await delay(80);
        const sameGood = visitItemId === 502;
        return HttpResponse.json(
          successEnvelope({
            item: {
              visitItemId,
              status: sameGood ? 'GOOD' : 'CAUTION',
              statusVersion: sameGood ? 3 : 1,
              statusSavedAt: '2026-08-11T04:03:00Z',
              version: sameGood ? 3 : 1,
              savedAt: '2026-08-11T04:03:00Z',
            },
            stageSummary: { totalCount: 2, checkedCount: 2, goodCount: 1, cautionCount: 1, unconfirmedCount: 0 },
            visitSummary: { totalCount: 3, checkedCount: 2, goodCount: 1, cautionCount: 1, unconfirmedCount: 1 },
          }),
        );
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/visits/31');

    await user.click(await screen.findByRole('tab', { name: /집에서 확인/ }));
    const goodForNoise = screen.getAllByRole('radio', { name: /^괜찮음/ })[0];
    expect(goodForNoise).toBeChecked();
    if (goodForNoise !== undefined) await user.click(goodForNoise);
    const cautionForWater = screen.getAllByRole('radio', { name: /^주의/ })[1];
    if (cautionForWater !== undefined) await user.click(cautionForWater);
    expect(screen.getByRole('button', { name: '모두 저장하고 체크 완료' })).toBeEnabled();
    expect(screen.getAllByRole('radio', { name: /^괜찮음/ })[0]).toBeEnabled();
    expect(screen.getAllByRole('radio', { name: /^괜찮음/ })[1]).toBeEnabled();

    await waitFor(() => expect(requests).toHaveLength(2));
    expect(requests).toEqual([
      { visitItemId: 502, body: { status: 'GOOD', expectedStatusVersion: 2 } },
      { visitItemId: 503, body: { status: 'CAUTION', expectedStatusVersion: 0 } },
    ]);
    expect(requests[0]?.body).not.toHaveProperty('expectedVersion');
    await waitFor(() => expect(screen.getByText(/상태 v3/)).toBeInTheDocument());
    expect(screen.getByLabelText('방문 결과 집계')).toHaveTextContent('전체 3개 중 2개 확인');
  });

  it('상태 409는 최신 version을 조회해 사용자 의도를 최대 한 번 자동 재적용한다', async () => {
    let serverHasConflictingChange = false;
    let patchCalls = 0;
    const refreshedVisit = {
      ...visitDetailFixture,
      stages: visitDetailFixture.stages.map((stage) =>
        stage.stage !== 'ONLINE_PHONE'
          ? stage
          : {
              ...stage,
              items: stage.items.map((item) =>
                item.visitItemId === 501
                  ? {
                      ...item,
                      status: 'CAUTION',
                      statusVersion: 1,
                      statusSavedAt: '2026-08-11T04:02:00Z',
                      version: 1,
                      savedAt: '2026-08-11T04:02:00Z',
                    }
                  : item,
              ),
            },
      ),
    };
    useVisitDetailHandlers(() => (serverHasConflictingChange ? refreshedVisit : visitDetailFixture));
    server.use(
      http.patch(`${config.apiBaseUrl}/api/visits/31/items/501`, async ({ request }) => {
        patchCalls += 1;
        const body = await request.json();
        if (patchCalls === 1) {
          expect(body).toEqual({ status: 'GOOD', expectedStatusVersion: 0 });
          serverHasConflictingChange = true;
          return HttpResponse.json(errorEnvelope('VISIT_ITEM_STATUS_VERSION_CONFLICT'), { status: 409 });
        }
        expect(body).toEqual({ status: 'GOOD', expectedStatusVersion: 1 });
        return HttpResponse.json(
          successEnvelope({
            item: {
              visitItemId: 501,
              status: 'GOOD',
              statusVersion: 2,
              statusSavedAt: '2026-08-11T04:03:00Z',
              version: 2,
              savedAt: '2026-08-11T04:03:00Z',
            },
            stageSummary: { totalCount: 1, checkedCount: 1, goodCount: 1, cautionCount: 0, unconfirmedCount: 0 },
            visitSummary: { totalCount: 3, checkedCount: 2, goodCount: 2, cautionCount: 0, unconfirmedCount: 1 },
          }),
        );
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/visits/31');

    await user.click(await screen.findByRole('radio', { name: /^괜찮음/ }));
    await waitFor(() => expect(screen.getByRole('radio', { name: /^괜찮음/ })).toBeChecked());
    await waitFor(() => expect(patchCalls).toBe(2));
    expect(screen.queryByRole('button', { name: '상태 다시 저장' })).not.toBeInTheDocument();
  });

  it('API-503의 독립 version과 nullable 메모 저장 시각을 초깃값으로 표시한다', async () => {
    useVisitDetailHandlers();
    renderAuthenticated('/visits/31');

    expect(await screen.findAllByText(/상태 v0/)).toHaveLength(2);
    expect(screen.getAllByText(/메모 v0 · 아직 저장하지 않음/)).toHaveLength(2);
    expect(await openFirstMemo()).toHaveValue('');

    await userEvent.setup().click(screen.getByRole('tab', { name: /집에서 확인/ }));
    expect(screen.getByText(/상태 v2/)).toBeInTheDocument();
    expect(screen.getByText(/메모 v1 · 마지막 저장/)).toBeInTheDocument();
    expect(screen.getAllByRole('textbox', { name: '한 줄 메모' })[0]).toHaveValue('밤 소음도 다시 확인');
  });

  it('메모는 마지막 입력 1초 전에는 저장하지 않고 1초 뒤 공백을 보존해 저장한다', async () => {
    const bodies: unknown[] = [];
    useVisitDetailHandlers();
    server.use(
      http.patch(`${config.apiBaseUrl}/api/visits/31/items/501/memo`, async ({ request }) => {
        const body = await request.json();
        bodies.push(body);
        return HttpResponse.json(successEnvelope(memoUpdateResult(501, (body as { memo: string }).memo, 1)));
      }),
    );
    renderAuthenticated('/visits/31');
    const memo = await openFirstMemo();

    fireEvent.change(memo, { target: { value: '  현관 폭  ' } });
    expect(bodies).toHaveLength(0);
    await new Promise((resolve) => setTimeout(resolve, 1050));
    await waitFor(() => expect(bodies).toEqual([{ memo: '  현관 폭  ', expectedMemoVersion: 0 }]));
    expect(bodies[0]).not.toHaveProperty('expectedVersion');
    expect(bodies[0]).not.toHaveProperty('expectedStatusVersion');
  });

  it('blur는 debounce를 기다리지 않고 빈 문자열 메모 삭제를 즉시 flush한다', async () => {
    let body: unknown;
    useVisitDetailHandlers();
    server.use(
      http.patch(`${config.apiBaseUrl}/api/visits/31/items/502/memo`, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(successEnvelope(memoUpdateResult(502, '', 2)));
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/visits/31');
    await user.click(await screen.findByRole('tab', { name: /집에서 확인/ }));
    const memo = screen.getAllByRole('textbox', { name: '한 줄 메모' })[0];
    if (memo === undefined) throw new Error('메모 입력을 찾지 못했습니다.');

    await user.clear(memo);
    expect(body).toBeUndefined();
    await user.tab();
    await waitFor(() => expect(body).toEqual({ memo: '', expectedMemoVersion: 1 }));
  });

  it('메모 입력은 CR·LF를 제거하고 Unicode 200 코드포인트까지만 보존한다', async () => {
    useVisitDetailHandlers();
    renderAuthenticated('/visits/31');
    const memo = await openFirstMemo();

    fireEvent.change(memo, { target: { value: '🏠'.repeat(200) } });
    expect(memo).toHaveValue('🏠'.repeat(200));
    expect(screen.getByText('200/200')).toBeInTheDocument();
    fireEvent.change(memo, { target: { value: '🏠'.repeat(201) } });
    expect(memo).toHaveValue('🏠'.repeat(200));
    fireEvent.change(memo, { target: { value: '수압\r\n다시\n확인' } });
    expect(memo).toHaveValue('수압다시확인');
  });

  it('같은 항목의 상태 요청을 직렬화하고 첫 응답 version으로 마지막 선택을 후속 저장한다', async () => {
    const bodies: unknown[] = [];
    let active = 0;
    let maximumActive = 0;
    useVisitDetailHandlers();
    server.use(
      http.patch(`${config.apiBaseUrl}/api/visits/31/items/501`, async ({ request }) => {
        active += 1;
        maximumActive = Math.max(maximumActive, active);
        const body = (await request.json()) as { status: 'GOOD' | 'CAUTION'; expectedStatusVersion: number };
        bodies.push(body);
        await delay(80);
        active -= 1;
        return HttpResponse.json(successEnvelope(statusUpdateResult(501, body.status, body.expectedStatusVersion + 1)));
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/visits/31');

    await user.click(await screen.findByRole('radio', { name: /^괜찮음/ }));
    await user.click(screen.getByRole('radio', { name: /^주의/ }));
    await waitFor(() => expect(bodies).toHaveLength(2));
    expect(bodies).toEqual([
      { status: 'GOOD', expectedStatusVersion: 0 },
      { status: 'CAUTION', expectedStatusVersion: 1 },
    ]);
    expect(maximumActive).toBe(1);
    expect(screen.getByRole('radio', { name: /^주의/ })).toBeChecked();
  });

  it('같은 항목의 메모 요청을 직렬화하고 저장 중 추가 입력을 최신 draft로 후속 저장한다', async () => {
    const bodies: unknown[] = [];
    let active = 0;
    let maximumActive = 0;
    useVisitDetailHandlers();
    server.use(
      http.patch(`${config.apiBaseUrl}/api/visits/31/items/501/memo`, async ({ request }) => {
        active += 1;
        maximumActive = Math.max(maximumActive, active);
        const body = (await request.json()) as { memo: string; expectedMemoVersion: number };
        bodies.push(body);
        await delay(100);
        active -= 1;
        return HttpResponse.json(successEnvelope(memoUpdateResult(501, body.memo, body.expectedMemoVersion + 1)));
      }),
    );
    renderAuthenticated('/visits/31');
    const memo = await openFirstMemo();

    fireEvent.change(memo, { target: { value: '첫 입력' } });
    fireEvent.blur(memo);
    fireEvent.change(memo, { target: { value: '첫 입력 후속' } });
    fireEvent.blur(memo);
    await waitFor(() => expect(bodies).toHaveLength(2));
    expect(bodies).toEqual([
      { memo: '첫 입력', expectedMemoVersion: 0 },
      { memo: '첫 입력 후속', expectedMemoVersion: 1 },
    ]);
    expect(maximumActive).toBe(1);
  });

  it('같은 항목의 상태와 메모 채널은 서로 기다리지 않고 동시에 저장한다', async () => {
    let statusActive = false;
    let memoOverlappedStatus = false;
    useVisitDetailHandlers();
    server.use(
      http.patch(`${config.apiBaseUrl}/api/visits/31/items/501`, async () => {
        statusActive = true;
        await delay(120);
        statusActive = false;
        return HttpResponse.json(successEnvelope(statusUpdateResult(501, 'GOOD', 1)));
      }),
      http.patch(`${config.apiBaseUrl}/api/visits/31/items/501/memo`, async ({ request }) => {
        memoOverlappedStatus = statusActive;
        const body = (await request.json()) as { memo: string };
        return HttpResponse.json(successEnvelope(memoUpdateResult(501, body.memo, 1)));
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/visits/31');
    await user.click(await screen.findByRole('radio', { name: /^괜찮음/ }));
    await waitFor(() => expect(statusActive).toBe(true));
    const memo = await openFirstMemo();
    fireEvent.change(memo, { target: { value: '동시 저장' } });
    fireEvent.blur(memo);

    await waitFor(() => expect(memoOverlappedStatus).toBe(true));
    expect(screen.getByRole('radio', { name: /^괜찮음/ })).toBeChecked();
  });

  it('상태 저장 실패는 마지막 선택과 메모 draft를 보존하고 명시적 재시도로 복구한다', async () => {
    let patchCalls = 0;
    useVisitDetailHandlers();
    server.use(
      http.patch(`${config.apiBaseUrl}/api/visits/31/items/501`, async ({ request }) => {
        patchCalls += 1;
        const body = (await request.json()) as { status: 'GOOD'; expectedStatusVersion: number };
        if (patchCalls === 1) return HttpResponse.json(errorEnvelope('INTERNAL_SERVER_ERROR'), { status: 500 });
        return HttpResponse.json(successEnvelope(statusUpdateResult(501, body.status, body.expectedStatusVersion + 1)));
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/visits/31');
    const memo = await openFirstMemo();

    fireEvent.change(memo, { target: { value: '현관 폭 재확인' } });
    await user.click(screen.getByRole('radio', { name: /^괜찮음/ }));
    expect(await screen.findByRole('button', { name: '상태 다시 저장' })).toBeInTheDocument();
    expect(screen.getByRole('radio', { name: /^괜찮음/ })).toBeChecked();
    expect(memo).toHaveValue('현관 폭 재확인');

    await user.click(screen.getByRole('button', { name: '상태 다시 저장' }));
    await waitFor(() => expect(patchCalls).toBe(2));
    expect(screen.queryByRole('button', { name: '상태 다시 저장' })).not.toBeInTheDocument();
    expect(memo).toHaveValue('현관 폭 재확인');
  });

  it('메모 저장 실패는 draft와 확정 상태를 보존하고 명시적 재시도로 복구한다', async () => {
    let patchCalls = 0;
    useVisitDetailHandlers();
    server.use(
      http.patch(`${config.apiBaseUrl}/api/visits/31/items/501/memo`, async ({ request }) => {
        patchCalls += 1;
        const body = (await request.json()) as { memo: string; expectedMemoVersion: number };
        if (patchCalls === 1) return HttpResponse.json(errorEnvelope('INTERNAL_SERVER_ERROR'), { status: 500 });
        return HttpResponse.json(successEnvelope(memoUpdateResult(501, body.memo, body.expectedMemoVersion + 1)));
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/visits/31');
    const memo = await openFirstMemo();

    fireEvent.change(memo, { target: { value: '보일러 위치' } });
    fireEvent.blur(memo);
    expect(await screen.findByRole('button', { name: '메모 다시 저장' })).toBeInTheDocument();
    expect(memo).toHaveValue('보일러 위치');
    expect(screen.getByRole('radio', { name: /^미확인/ })).toBeChecked();

    await user.click(screen.getByRole('button', { name: '메모 다시 저장' }));
    await waitFor(() => expect(patchCalls).toBe(2));
    expect(screen.queryByRole('button', { name: '메모 다시 저장' })).not.toBeInTheDocument();
    expect(memo).toHaveValue('보일러 위치');
    expect(screen.getByRole('radio', { name: /^미확인/ })).toBeChecked();
  });

  it('상태 재충돌은 자동 재시도를 한 번에서 멈추고 사용자가 다시 저장할 수 있게 한다', async () => {
    let latest = false;
    let patchCalls = 0;
    const refreshedVisit = {
      ...visitDetailFixture,
      stages: visitDetailFixture.stages.map((stage) => ({
        ...stage,
        items: stage.items.map((item) =>
          item.visitItemId === 501 ? { ...item, status: 'CAUTION' as const, statusVersion: 1, version: 1 } : item,
        ),
      })),
    };
    useVisitDetailHandlers(() => (latest ? refreshedVisit : visitDetailFixture));
    server.use(
      http.patch(`${config.apiBaseUrl}/api/visits/31/items/501`, () => {
        patchCalls += 1;
        latest = true;
        return HttpResponse.json(errorEnvelope('VISIT_ITEM_STATUS_VERSION_CONFLICT'), { status: 409 });
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/visits/31');

    await user.click(await screen.findByRole('radio', { name: /^괜찮음/ }));
    expect(await screen.findByRole('button', { name: '상태 다시 저장' })).toBeInTheDocument();
    expect(patchCalls).toBe(2);
    await new Promise((resolve) => setTimeout(resolve, 80));
    expect(patchCalls).toBe(2);
    expect(screen.getByRole('radio', { name: /^괜찮음/ })).toBeChecked();
    expect(screen.getByText(/자동 재시도를 멈췄습니다/)).toBeInTheDocument();
  });

  it('메모 409 재조회는 사용자 draft와 상태 채널을 보존하고 최신 memoVersion으로 한 번 재저장한다', async () => {
    let latest = false;
    const bodies: unknown[] = [];
    const refreshedVisit = {
      ...visitDetailFixture,
      stages: visitDetailFixture.stages.map((stage) => ({
        ...stage,
        items: stage.items.map((item) =>
          item.visitItemId === 501
            ? {
                ...item,
                status: 'CAUTION' as const,
                statusVersion: 9,
                statusSavedAt: '2026-08-11T04:09:00Z',
                version: 9,
                savedAt: '2026-08-11T04:09:00Z',
                inlineMemo: '다른 기기의 메모',
                memoVersion: 1,
                memoSavedAt: '2026-08-11T04:02:00Z',
              }
            : item,
        ),
      })),
    };
    useVisitDetailHandlers(() => (latest ? refreshedVisit : visitDetailFixture));
    server.use(
      http.patch(`${config.apiBaseUrl}/api/visits/31/items/501/memo`, async ({ request }) => {
        const body = await request.json();
        bodies.push(body);
        if (bodies.length === 1) {
          latest = true;
          return HttpResponse.json(errorEnvelope('VISIT_ITEM_MEMO_VERSION_CONFLICT'), { status: 409 });
        }
        return HttpResponse.json(successEnvelope(memoUpdateResult(501, '내 현장 메모', 2)));
      }),
    );
    renderAuthenticated('/visits/31');
    const memo = await openFirstMemo();

    fireEvent.change(memo, { target: { value: '내 현장 메모' } });
    fireEvent.blur(memo);
    await waitFor(() => expect(bodies).toHaveLength(2));
    expect(bodies).toEqual([
      { memo: '내 현장 메모', expectedMemoVersion: 0 },
      { memo: '내 현장 메모', expectedMemoVersion: 1 },
    ]);
    expect(memo).toHaveValue('내 현장 메모');
    expect(screen.getByRole('radio', { name: /^미확인/ })).toBeChecked();
    expect(screen.getByText(/메모 v2/)).toBeInTheDocument();
  });

  it('dirty 상태에서 API-503을 다시 조회해도 draft를 유지하고 최신 memoVersion으로 저장한다', async () => {
    let latest = false;
    let body: unknown;
    const refreshedVisit = {
      ...visitDetailFixture,
      stages: visitDetailFixture.stages.map((stage) => ({
        ...stage,
        items: stage.items.map((item) =>
          item.visitItemId === 501
            ? {
                ...item,
                inlineMemo: '서버 메모',
                memoVersion: 1,
                memoSavedAt: '2026-08-11T04:02:00Z',
              }
            : item,
        ),
      })),
    };
    useVisitDetailHandlers(() => (latest ? refreshedVisit : visitDetailFixture));
    server.use(
      http.patch(`${config.apiBaseUrl}/api/visits/31/items/501/memo`, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(successEnvelope(memoUpdateResult(501, '내 draft', 2)));
      }),
    );
    renderAuthenticated('/visits/31');
    const memo = await openFirstMemo();

    fireEvent.change(memo, { target: { value: '내 draft' } });
    latest = true;
    await queryClient.refetchQueries({ queryKey: visitQueryKeys.detail(31), exact: true });
    expect(memo).toHaveValue('내 draft');
    fireEvent.blur(memo);
    await waitFor(() => expect(body).toEqual({ memo: '내 draft', expectedMemoVersion: 1 }));
  });

  it('메모 재충돌은 자동 재시도를 한 번에서 멈추고 draft를 재시도 UI에 유지한다', async () => {
    let latest = false;
    let patchCalls = 0;
    const refreshedVisit = {
      ...visitDetailFixture,
      stages: visitDetailFixture.stages.map((stage) => ({
        ...stage,
        items: stage.items.map((item) =>
          item.visitItemId === 501
            ? {
                ...item,
                inlineMemo: '다른 기기의 메모',
                memoVersion: 1,
                memoSavedAt: '2026-08-11T04:02:00Z',
              }
            : item,
        ),
      })),
    };
    useVisitDetailHandlers(() => (latest ? refreshedVisit : visitDetailFixture));
    server.use(
      http.patch(`${config.apiBaseUrl}/api/visits/31/items/501/memo`, () => {
        patchCalls += 1;
        latest = true;
        return HttpResponse.json(errorEnvelope('VISIT_ITEM_MEMO_VERSION_CONFLICT'), { status: 409 });
      }),
    );
    renderAuthenticated('/visits/31');
    const memo = await openFirstMemo();

    fireEvent.change(memo, { target: { value: '내 draft' } });
    fireEvent.blur(memo);
    expect(await screen.findByRole('button', { name: '메모 다시 저장' })).toBeInTheDocument();
    expect(patchCalls).toBe(2);
    await new Promise((resolve) => setTimeout(resolve, 80));
    expect(patchCalls).toBe(2);
    expect(memo).toHaveValue('내 draft');
    expect(screen.getByText(/자동 재시도를 멈췄습니다/)).toBeInTheDocument();
  });
});

describe('FE-4 완료와 마이페이지', () => {
  it('방문 내 다른 단계로 이동하기 전에 dirty 메모를 flush한다', async () => {
    let body: unknown;
    useVisitDetailHandlers();
    server.use(
      http.patch(`${config.apiBaseUrl}/api/visits/31/items/501/memo`, async ({ request }) => {
        body = await request.json();
        await delay(40);
        return HttpResponse.json(successEnvelope(memoUpdateResult(501, '단계 전환 전 저장', 1)));
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/visits/31');
    const memo = await openFirstMemo();

    fireEvent.change(memo, { target: { value: '단계 전환 전 저장' } });
    await user.click(screen.getByRole('tab', { name: /집에서 확인/ }));
    await waitFor(() =>
      expect(screen.getByRole('tab', { name: /집에서 확인/ })).toHaveAttribute('aria-selected', 'true'),
    );
    expect(body).toEqual({ memo: '단계 전환 전 저장', expectedMemoVersion: 0 });
  });

  it('내부 화면 이동 전에 dirty 메모를 즉시 flush하고 성공한 뒤에만 이동한다', async () => {
    let memoBody: unknown;
    useVisitDetailHandlers();
    server.use(
      http.patch(`${config.apiBaseUrl}/api/visits/31/items/501/memo`, async ({ request }) => {
        memoBody = await request.json();
        await delay(60);
        return HttpResponse.json(successEnvelope(memoUpdateResult(501, '이동 전 저장', 1)));
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/visits/31');
    const memo = await openFirstMemo();

    fireEvent.change(memo, { target: { value: '이동 전 저장' } });
    await user.click(screen.getByRole('link', { name: '매물 상세로 돌아가기' }));
    expect(await screen.findByRole('heading', { name: '신림역 원룸', level: 1 })).toBeInTheDocument();
    expect(memoBody).toEqual({ memo: '이동 전 저장', expectedMemoVersion: 0 });
  });

  it('브라우저 뒤로 가기 전에 dirty 메모를 즉시 flush하고 성공한 뒤에만 이동한다', async () => {
    let memoBody: unknown;
    useVisitDetailHandlers();
    server.use(
      http.patch(`${config.apiBaseUrl}/api/visits/31/items/501/memo`, async ({ request }) => {
        memoBody = await request.json();
        await delay(60);
        return HttpResponse.json(successEnvelope(memoUpdateResult(501, '뒤로 가기 전 저장', 1)));
      }),
    );
    const { history } = renderAuthenticated('/visits/31', {
      initialEntries: ['/properties/10/visits', '/visits/31'],
      initialIndex: 1,
    });
    const memo = await openFirstMemo();

    fireEvent.change(memo, { target: { value: '뒤로 가기 전 저장' } });
    history.go(-1);
    expect(
      await screen.findByRole('heading', { name: '신림역 원룸 방문 기록', level: 1 }, { timeout: 15_000 }),
    ).toBeInTheDocument();
    expect(memoBody).toEqual({ memo: '뒤로 가기 전 저장', expectedMemoVersion: 0 });
  });

  it('브라우저 뒤로 가기 전 저장이 실패하면 URL과 draft를 유지하고 현재 화면에 남는다', async () => {
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(false);
    useVisitDetailHandlers();
    server.use(
      http.patch(`${config.apiBaseUrl}/api/visits/31/items/501/memo`, () =>
        HttpResponse.json(errorEnvelope('INTERNAL_SERVER_ERROR'), { status: 500 }),
      ),
    );
    const { history } = renderAuthenticated('/visits/31', {
      initialEntries: ['/properties/10/visits', '/visits/31'],
      initialIndex: 1,
    });
    const memo = await openFirstMemo();

    fireEvent.change(memo, { target: { value: '뒤로 가도 잃으면 안 되는 메모' } });
    history.go(-1);

    await waitFor(() => expect(confirm).toHaveBeenCalledOnce());
    expect(history.location.pathname).toBe('/visits/31');
    expect(screen.getByRole('heading', { name: '신림역 원룸 방문', level: 1 })).toBeInTheDocument();
    expect(memo).toHaveValue('뒤로 가도 잃으면 안 되는 메모');
    expect(screen.getByRole('button', { name: '메모 다시 저장' })).toBeInTheDocument();
    confirm.mockRestore();
  });

  it('내부 화면 이동 전 저장이 실패하면 draft를 유지하고 사용자 확인 없이는 현재 화면에 남는다', async () => {
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(false);
    useVisitDetailHandlers();
    server.use(
      http.patch(`${config.apiBaseUrl}/api/visits/31/items/501/memo`, () =>
        HttpResponse.json(errorEnvelope('INTERNAL_SERVER_ERROR'), { status: 500 }),
      ),
    );
    const user = userEvent.setup();
    renderAuthenticated('/visits/31');
    const memo = await openFirstMemo();

    fireEvent.change(memo, { target: { value: '잃으면 안 되는 메모' } });
    await user.click(screen.getByRole('link', { name: '매물 상세로 돌아가기' }));
    await waitFor(() => expect(confirm).toHaveBeenCalledOnce());
    expect(screen.getByRole('heading', { name: '신림역 원룸 방문', level: 1 })).toBeInTheDocument();
    expect(memo).toHaveValue('잃으면 안 되는 메모');
    expect(screen.getByRole('button', { name: '메모 다시 저장' })).toBeInTheDocument();
    confirm.mockRestore();
  });

  it('모든 상태·메모 pending 저장이 성공한 뒤에만 API-505 방문 완료를 호출한다', async () => {
    const events: string[] = [];
    useVisitDetailHandlers();
    server.use(
      http.patch(`${config.apiBaseUrl}/api/visits/31/items/501`, async ({ request }) => {
        const body = (await request.json()) as { status: 'GOOD' };
        events.push('status-start');
        await delay(80);
        events.push('status-saved');
        return HttpResponse.json(successEnvelope(statusUpdateResult(501, body.status, 1)));
      }),
      http.patch(`${config.apiBaseUrl}/api/visits/31/items/501/memo`, async ({ request }) => {
        const body = (await request.json()) as { memo: string };
        events.push('memo-start');
        await delay(40);
        events.push('memo-saved');
        return HttpResponse.json(successEnvelope(memoUpdateResult(501, body.memo, 1)));
      }),
      http.patch(`${config.apiBaseUrl}/api/visits/31`, async () => {
        events.push('completed');
        return HttpResponse.json(
          successEnvelope({
            visitId: 31,
            status: 'COMPLETED',
            startedAt: visitDetailFixture.startedAt,
            completedAt: '2026-08-11T04:05:00Z',
            summary: visitDetailFixture.summary,
          }),
        );
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/visits/31');
    const memo = await openFirstMemo();

    await user.click(screen.getByRole('radio', { name: /^괜찮음/ }));
    fireEvent.change(memo, { target: { value: '완료 전 메모' } });
    await user.click(screen.getByRole('button', { name: '모두 저장하고 체크 완료' }));
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: '방문 완료' }));
    expect(await screen.findByText(/완료한 방문입니다/)).toBeInTheDocument();
    expect(events).toContain('status-saved');
    expect(events).toContain('memo-saved');
    expect(events.at(-1)).toBe('completed');
  });

  it('pending 저장 하나라도 실패하면 API-505를 호출하지 않고 방문과 draft를 유지한다', async () => {
    let completionCalls = 0;
    let memoSaveCalls = 0;
    useVisitDetailHandlers();
    server.use(
      http.patch(`${config.apiBaseUrl}/api/visits/31/items/501/memo`, () => {
        memoSaveCalls += 1;
        return HttpResponse.json(errorEnvelope('INTERNAL_SERVER_ERROR'), { status: 500 });
      }),
      http.patch(`${config.apiBaseUrl}/api/visits/31`, () => {
        completionCalls += 1;
        return HttpResponse.json(errorEnvelope('SHOULD_NOT_BE_CALLED'), { status: 500 });
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/visits/31');
    const memo = await openFirstMemo();

    fireEvent.change(memo, { target: { value: '완료 전에 꼭 저장' } });
    await user.click(screen.getByRole('button', { name: '모두 저장하고 체크 완료' }));
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: '방문 완료' }));
    expect(await screen.findByText(/저장하지 못한 항목이 있어 방문을 완료하지 않았어요/)).toBeInTheDocument();
    expect(completionCalls).toBe(0);
    expect(memoSaveCalls).toBe(1);
    expect(screen.getByText('확인 진행 중')).toBeInTheDocument();
    expect(memo).toHaveValue('완료 전에 꼭 저장');
    expect(screen.getByRole('button', { name: '메모 다시 저장' })).toHaveFocus();

    fireEvent.blur(memo);
    await user.click(screen.getByRole('button', { name: '모두 저장하고 체크 완료' }));
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: '방문 완료' }));
    await waitFor(() => expect(screen.getByText(/저장하지 못한 항목이 있어 방문을 완료하지 않았어요/)).toBeVisible());
    expect(memoSaveCalls).toBe(1);
    expect(completionCalls).toBe(0);
  });

  it('미확인 항목이 있어도 완료하고 완료 후 항목을 수정한다', async () => {
    let completionBody: unknown;
    let postCompletionBody: unknown;
    let postCompletionMemoBody: unknown;
    useVisitDetailHandlers();
    server.use(
      http.patch(`${config.apiBaseUrl}/api/visits/31`, async ({ request }) => {
        completionBody = await request.json();
        return HttpResponse.json(
          successEnvelope({
            visitId: 31,
            status: 'COMPLETED',
            startedAt: visitDetailFixture.startedAt,
            completedAt: '2026-08-11T04:05:00Z',
            summary: visitDetailFixture.summary,
          }),
        );
      }),
      http.patch(`${config.apiBaseUrl}/api/visits/31/items/501`, async ({ request }) => {
        postCompletionBody = await request.json();
        return HttpResponse.json(
          successEnvelope({
            item: {
              visitItemId: 501,
              status: 'CAUTION',
              statusVersion: 1,
              statusSavedAt: '2026-08-11T04:06:00Z',
              version: 1,
              savedAt: '2026-08-11T04:06:00Z',
            },
            stageSummary: { totalCount: 1, checkedCount: 1, goodCount: 0, cautionCount: 1, unconfirmedCount: 0 },
            visitSummary: { totalCount: 3, checkedCount: 2, goodCount: 1, cautionCount: 1, unconfirmedCount: 1 },
          }),
        );
      }),
      http.patch(`${config.apiBaseUrl}/api/visits/31/items/501/memo`, async ({ request }) => {
        postCompletionMemoBody = await request.json();
        return HttpResponse.json(successEnvelope(memoUpdateResult(501, '완료 후 메모', 1)));
      }),
    );
    const user = userEvent.setup();
    renderAuthenticated('/visits/31');

    await user.click(await screen.findByRole('button', { name: '체크 완료 및 저장' }));
    const dialog = screen.getByRole('dialog', { name: '이 방문을 완료할까요?' });
    expect(within(dialog).getByText(/미확인 항목이 있어도 완료/)).toBeInTheDocument();
    await user.click(within(dialog).getByRole('button', { name: '방문 완료' }));
    expect(await screen.findByText(/완료한 방문입니다/)).toBeInTheDocument();
    expect(completionBody).toEqual({ status: 'COMPLETED' });

    await user.click(screen.getByRole('radio', { name: /^주의/ }));
    await waitFor(() => expect(postCompletionBody).toEqual({ status: 'CAUTION', expectedStatusVersion: 0 }));
    await waitFor(() => expect(screen.getByRole('radio', { name: /^주의/ })).toBeChecked());
    expect(screen.getByText(/최초 완료/, { selector: 'time' })).toBeInTheDocument();

    const memo = await openFirstMemo();
    fireEvent.change(memo, { target: { value: '완료 후 메모' } });
    fireEvent.blur(memo);
    await waitFor(() => expect(postCompletionMemoBody).toEqual({ memo: '완료 후 메모', expectedMemoVersion: 0 }));
    expect(screen.getByText(/최초 완료/, { selector: 'time' })).toHaveAttribute('datetime', '2026-08-11T04:05:00Z');
  });

  it('마이페이지는 보호 경로의 회원 정본을 사용하고 로그아웃한다', async () => {
    const user = userEvent.setup();
    renderAuthenticated('/me');

    expect(await screen.findByRole('heading', { name: '마이' })).toBeInTheDocument();
    expect(screen.getByText(memberFixture.email)).toBeInTheDocument();
    expect(screen.getByText('Google 계정 연결됨')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /내 매물 관리/ })).toHaveAttribute('href', '/properties');
    expect(screen.getByRole('link', { name: /내 체크리스트 관리/ })).toHaveAttribute('href', '/checklists');
    expect(screen.getByRole('link', { name: /내보낸 비교표/ })).toHaveAttribute('href', '/export');
    expect(screen.queryByRole('heading', { name: '현재 로그인' })).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '로그아웃' }));
    expect(await screen.findByRole('button', { name: '구글로 로그인하기' })).toBeInTheDocument();
  });
});
