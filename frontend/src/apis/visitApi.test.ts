import { HttpResponse, http } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import { setAuthentication } from '../app/authStore';
import { errorEnvelope, successEnvelope } from '../test/propertyFixtures';
import { server } from '../test/server';
import { visitDetailFixture, visitPageFixture } from '../test/visitFixtures';
import type { PublicConfig } from '../types/PublicConfig';
import {
  completeVisit,
  fetchPropertyVisits,
  fetchVisitDetail,
  startPropertyVisit,
  updateVisitItem,
  updateVisitItemMemo,
  updateVisitItemStatus,
} from './visitApi';

const config: PublicConfig = {
  apiBaseUrl: 'http://localhost:8080',
  googleClientId: 'test-client',
  googleRedirectUri: 'http://localhost:3000/oauth/google/callback',
};

const authenticate = () => setAuthentication({ accessToken: 'memory-token', tokenType: 'Bearer', expiresIn: 60 });

describe('FE-4 API-501~506 경계', () => {
  it('API-501은 page·size만 보내고 memberId를 노출하지 않는다', async () => {
    authenticate();
    server.use(
      http.get(`${config.apiBaseUrl}/api/properties/10/visits`, ({ request }) => {
        const url = new URL(request.url);
        expect(url.searchParams.get('page')).toBe('2');
        expect(url.searchParams.get('size')).toBe('20');
        expect(url.searchParams.has('memberId')).toBe(false);
        expect(request.headers.get('Authorization')).toBe('Bearer memory-token');
        return HttpResponse.json(successEnvelope(visitPageFixture([], 2)));
      }),
    );

    await expect(fetchPropertyVisits(config, 10, 2)).resolves.toMatchObject({ page: 2, content: [] });
  });

  it('API-502는 본문과 Content-Type 없이 방문을 시작한다', async () => {
    authenticate();
    server.use(
      http.post(`${config.apiBaseUrl}/api/properties/10/visits`, async ({ request }) => {
        expect(request.headers.get('Content-Type')).toBeNull();
        expect(await request.text()).toBe('');
        return HttpResponse.json(successEnvelope(visitDetailFixture), { status: 201 });
      }),
    );

    await expect(startPropertyVisit(config, 10)).resolves.toMatchObject({ visitId: 31, propertyId: 10 });
  });

  it('API-503은 nullable 출처와 안내를 포함한 스냅샷을 검증한다', async () => {
    authenticate();
    server.use(
      http.get(`${config.apiBaseUrl}/api/visits/31`, () => HttpResponse.json(successEnvelope(visitDetailFixture))),
    );

    const result = await fetchVisitDetail(config, 31);
    expect(result.stages[0]?.sourceChecklistId).toBeNull();
    expect(result.stages[0]?.items[0]?.guide).toBeNull();
    expect(result.stages[0]?.items[0]).toMatchObject({
      origin: 'PROVIDED',
      sourceChecklistItemId: null,
      statusVersion: 0,
      statusSavedAt: '2026-08-11T04:00:00Z',
      inlineMemo: '',
      memoVersion: 0,
      memoSavedAt: null,
    });
    expect(result.stages[1]?.items[1]).toMatchObject({ origin: 'CUSTOM', sourceCheckItemId: null });
  });

  it('API-503은 origin과 PROVIDED 출처·CUSTOM 안내의 잘못된 조합을 거부한다', async () => {
    authenticate();
    server.use(
      http.get(`${config.apiBaseUrl}/api/visits/31`, () =>
        HttpResponse.json(
          successEnvelope({
            ...visitDetailFixture,
            stages: [
              visitDetailFixture.stages[0],
              {
                ...visitDetailFixture.stages[1],
                items: [
                  visitDetailFixture.stages[1]?.items[0],
                  { ...visitDetailFixture.stages[1]?.items[1], guide: 'CUSTOM에는 안내 출처가 없음' },
                ],
              },
            ],
          }),
        ),
      ),
    );

    await expect(fetchVisitDetail(config, 31)).rejects.toMatchObject({ kind: 'invalid-response' });

    server.use(
      http.get(`${config.apiBaseUrl}/api/visits/31`, () =>
        HttpResponse.json(
          successEnvelope({
            ...visitDetailFixture,
            stages: [
              {
                ...visitDetailFixture.stages[0],
                items: [{ ...visitDetailFixture.stages[0]?.items[0], sourceCheckItemId: null }],
              },
              visitDetailFixture.stages[1],
            ],
          }),
        ),
      ),
    );

    await expect(fetchVisitDetail(config, 31)).rejects.toMatchObject({ kind: 'invalid-response' });
  });

  it('API-503과 API-504 응답 식별자가 요청 리소스와 다르면 거부한다', async () => {
    authenticate();
    server.use(
      http.get(`${config.apiBaseUrl}/api/visits/31`, () =>
        HttpResponse.json(successEnvelope({ ...visitDetailFixture, visitId: 32 })),
      ),
      http.patch(`${config.apiBaseUrl}/api/visits/31/items/501`, () =>
        HttpResponse.json(
          successEnvelope({
            item: {
              visitItemId: 999,
              status: 'GOOD',
              statusVersion: 1,
              statusSavedAt: '2026-08-11T04:03:00Z',
              version: 1,
              savedAt: '2026-08-11T04:03:00Z',
            },
            stageSummary: { totalCount: 1, checkedCount: 1, goodCount: 1, cautionCount: 0, unconfirmedCount: 0 },
            visitSummary: { totalCount: 3, checkedCount: 2, goodCount: 2, cautionCount: 0, unconfirmedCount: 1 },
          }),
        ),
      ),
    );

    await expect(fetchVisitDetail(config, 31)).rejects.toMatchObject({ kind: 'invalid-response' });
    await expect(
      updateVisitItemStatus(config, 31, 501, { status: 'GOOD', expectedStatusVersion: 0 }),
    ).rejects.toMatchObject({ kind: 'invalid-response' });
  });

  it('API-504는 expectedStatusVersion만 보내고 상태 응답의 deprecated 별칭 일치를 검증한다', async () => {
    authenticate();
    let body: unknown;
    server.use(
      http.patch(`${config.apiBaseUrl}/api/visits/31/items/501`, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(
          successEnvelope({
            item: {
              visitItemId: 501,
              status: 'GOOD',
              statusVersion: 4,
              statusSavedAt: '2026-08-11T04:03:00Z',
              version: 4,
              savedAt: '2026-08-11T04:03:00Z',
            },
            stageSummary: { totalCount: 1, checkedCount: 1, goodCount: 1, cautionCount: 0, unconfirmedCount: 0 },
            visitSummary: { totalCount: 3, checkedCount: 2, goodCount: 2, cautionCount: 0, unconfirmedCount: 1 },
          }),
        );
      }),
    );

    const result = await updateVisitItemStatus(config, 31, 501, { status: 'GOOD', expectedStatusVersion: 3 });
    expect(body).toEqual({ status: 'GOOD', expectedStatusVersion: 3 });
    expect(body).not.toHaveProperty('expectedVersion');
    expect(result.item.statusVersion).toBe(4);
  });

  it('API-503·504는 v1.1 상태 필드와 deprecated version·savedAt 불일치를 거부한다', async () => {
    authenticate();
    server.use(
      http.get(`${config.apiBaseUrl}/api/visits/31`, () =>
        HttpResponse.json(
          successEnvelope({
            ...visitDetailFixture,
            stages: [
              {
                ...visitDetailFixture.stages[0],
                items: [{ ...visitDetailFixture.stages[0]?.items[0], version: 9 }],
              },
              visitDetailFixture.stages[1],
            ],
          }),
        ),
      ),
      http.patch(`${config.apiBaseUrl}/api/visits/31/items/501`, () =>
        HttpResponse.json(
          successEnvelope({
            item: {
              visitItemId: 501,
              status: 'GOOD',
              statusVersion: 2,
              statusSavedAt: '2026-08-11T04:03:00Z',
              version: 1,
              savedAt: '2026-08-11T04:03:00Z',
            },
            stageSummary: visitDetailFixture.stages[0]?.summary,
            visitSummary: visitDetailFixture.summary,
          }),
        ),
      ),
    );

    await expect(fetchVisitDetail(config, 31)).rejects.toMatchObject({ kind: 'invalid-response' });
    await expect(
      updateVisitItemStatus(config, 31, 501, { status: 'GOOD', expectedStatusVersion: 1 }),
    ).rejects.toMatchObject({ kind: 'invalid-response' });
  });

  it('API-506은 memo·expectedMemoVersion만 보내고 상태 채널 없이 메모 저장 결과를 읽는다', async () => {
    authenticate();
    let body: unknown;
    server.use(
      http.patch(`${config.apiBaseUrl}/api/visits/31/items/501/memo`, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(
          successEnvelope({
            visitItemId: 501,
            memo: '  창틀 습기 확인  ',
            memoVersion: 1,
            memoSavedAt: '2026-08-11T04:04:00Z',
          }),
        );
      }),
    );

    const result = await updateVisitItemMemo(config, 31, 501, {
      memo: '  창틀 습기 확인  ',
      expectedMemoVersion: 0,
    });
    expect(body).toEqual({ memo: '  창틀 습기 확인  ', expectedMemoVersion: 0 });
    expect(body).not.toHaveProperty('expectedStatusVersion');
    expect(body).not.toHaveProperty('expectedVersion');
    expect(result).toMatchObject({ memoVersion: 1, memo: '  창틀 습기 확인  ' });
  });

  it('API-506은 한 줄 200 Unicode 코드포인트를 벗어나거나 저장 시각이 UTC가 아니면 거부한다', async () => {
    authenticate();
    server.use(
      http.patch(`${config.apiBaseUrl}/api/visits/31/items/501/memo`, () =>
        HttpResponse.json(
          successEnvelope({
            visitItemId: 501,
            memo: '첫 줄\n둘째 줄',
            memoVersion: 1,
            memoSavedAt: '2026-08-11T13:04:00+09:00',
          }),
        ),
      ),
    );

    await expect(updateVisitItemMemo(config, 31, 501, { memo: '', expectedMemoVersion: 0 })).rejects.toMatchObject({
      kind: 'invalid-response',
    });

    server.use(
      http.patch(`${config.apiBaseUrl}/api/visits/31/items/501/memo`, () =>
        HttpResponse.json(
          successEnvelope({
            visitItemId: 501,
            memo: '😀'.repeat(201),
            memoVersion: 1,
            memoSavedAt: '2026-08-11T04:04:00Z',
          }),
        ),
      ),
    );

    await expect(updateVisitItemMemo(config, 31, 501, { memo: '', expectedMemoVersion: 0 })).rejects.toMatchObject({
      kind: 'invalid-response',
    });
  });

  it('API-506 실패는 메모 원문과 서버 message를 오류나 console에 노출하지 않는다', async () => {
    authenticate();
    const memo = '외부에 남으면 안 되는 메모';
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    const consoleLog = vi.spyOn(console, 'log').mockImplementation(() => undefined);
    server.use(
      http.patch(`${config.apiBaseUrl}/api/visits/31/items/501/memo`, () =>
        HttpResponse.json(
          { code: 'VISIT_ITEM_MEMO_VERSION_CONFLICT', message: `내부 상세: ${memo}`, errors: [] },
          { status: 409 },
        ),
      ),
    );

    try {
      const error = await updateVisitItemMemo(config, 31, 501, { memo, expectedMemoVersion: 0 }).catch(
        (caught: unknown) => caught,
      );
      expect(error).toMatchObject({ status: 409, code: 'VISIT_ITEM_MEMO_VERSION_CONFLICT' });
      expect(String(error)).not.toContain(memo);
      expect(consoleError).not.toHaveBeenCalled();
      expect(consoleLog).not.toHaveBeenCalled();
    } finally {
      consoleError.mockRestore();
      consoleLog.mockRestore();
    }
  });

  it('상태·메모 409를 자동 재시도하지 않고 각각의 오류 code로 보존한다', async () => {
    authenticate();
    let statusAttempts = 0;
    let memoAttempts = 0;
    server.use(
      http.patch(`${config.apiBaseUrl}/api/visits/31/items/501`, () => {
        statusAttempts += 1;
        return HttpResponse.json(errorEnvelope('VISIT_ITEM_STATUS_VERSION_CONFLICT'), { status: 409 });
      }),
      http.patch(`${config.apiBaseUrl}/api/visits/31/items/501/memo`, () => {
        memoAttempts += 1;
        return HttpResponse.json(errorEnvelope('VISIT_ITEM_MEMO_VERSION_CONFLICT'), { status: 409 });
      }),
    );

    await expect(
      updateVisitItemStatus(config, 31, 501, { status: 'GOOD', expectedStatusVersion: 0 }),
    ).rejects.toMatchObject({ status: 409, code: 'VISIT_ITEM_STATUS_VERSION_CONFLICT' });
    await expect(updateVisitItemMemo(config, 31, 501, { memo: '', expectedMemoVersion: 0 })).rejects.toMatchObject({
      status: 409,
      code: 'VISIT_ITEM_MEMO_VERSION_CONFLICT',
    });
    expect(statusAttempts).toBe(1);
    expect(memoAttempts).toBe(1);
  });

  it('legacy 상태 함수는 expectedVersion을 v1.1 expectedStatusVersion으로 변환한다', async () => {
    authenticate();
    let body: unknown;
    server.use(
      http.patch(`${config.apiBaseUrl}/api/visits/31/items/501`, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(
          successEnvelope({
            item: {
              visitItemId: 501,
              status: 'GOOD',
              statusVersion: 1,
              statusSavedAt: '2026-08-11T04:03:00Z',
              version: 1,
              savedAt: '2026-08-11T04:03:00Z',
            },
            stageSummary: visitDetailFixture.stages[0]?.summary,
            visitSummary: visitDetailFixture.summary,
          }),
        );
      }),
    );

    await expect(updateVisitItem(config, 31, 501, { status: 'GOOD', expectedVersion: 0 })).resolves.toMatchObject({
      item: { version: 1 },
    });
    expect(body).toEqual({ status: 'GOOD', expectedStatusVersion: 0 });
  });

  it('API-505는 COMPLETED만 보내고 서버의 최초 완료 시각을 사용한다', async () => {
    authenticate();
    let body: unknown;
    server.use(
      http.patch(`${config.apiBaseUrl}/api/visits/31`, async ({ request }) => {
        body = await request.json();
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

    const result = await completeVisit(config, 31);
    expect(body).toEqual({ status: 'COMPLETED' });
    expect(result.completedAt).toBe('2026-08-11T04:05:00Z');
  });
});
