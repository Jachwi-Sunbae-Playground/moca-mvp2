import { HttpResponse, http } from 'msw';
import { describe, expect, it } from 'vitest';
import { setAuthentication } from '../app/authStore';
import {
  checkItemPageFixture,
  checklistDetailFixture,
  checklistPageFixture,
  checklistSummaryFixture,
  customChecklistItemFixture,
  mixedChecklistDetailFixture,
  onlineItemFixture,
  presetFixture,
} from '../test/checklistFixtures';
import { errorEnvelope, successEnvelope } from '../test/propertyFixtures';
import { server } from '../test/server';
import type { PublicConfig } from '../types/PublicConfig';
import {
  assignActiveChecklist,
  createChecklist,
  createChecklistV11,
  fetchCheckItems,
  fetchChecklistDetail,
  fetchChecklistPreset,
  fetchChecklists,
  removeActiveChecklist,
  removeChecklist,
  updateChecklist,
  updateChecklistV11,
} from './checklistApi';
import { getChecklistErrorMessage } from './checklistErrorMessages';

const config: PublicConfig = {
  apiBaseUrl: 'http://localhost:8080',
  googleClientId: 'test-client',
  googleRedirectUri: 'http://localhost:3000/oauth/google/callback',
};

const authenticate = () => setAuthentication({ accessToken: 'memory-token', tokenType: 'Bearer', expiresIn: 60 });

describe('FE-3 API 경계', () => {
  it('API-301은 stage·trim query·page·size만 보내고 항목 페이지를 검증한다', async () => {
    authenticate();
    server.use(
      http.get(`${config.apiBaseUrl}/api/check-items`, ({ request }) => {
        const url = new URL(request.url);
        expect(Object.fromEntries(url.searchParams)).toEqual({
          stage: 'ONLINE_PHONE',
          page: '2',
          size: '20',
          query: '관리비',
        });
        expect(url.searchParams.has('memberId')).toBe(false);
        expect(request.headers.get('Authorization')).toBe('Bearer memory-token');
        return HttpResponse.json(successEnvelope(checkItemPageFixture([{ ...onlineItemFixture, guide: null }], 2)));
      }),
    );

    await expect(
      fetchCheckItems(config, { stage: 'ONLINE_PHONE', query: '  관리비  ', page: 2 }),
    ).resolves.toMatchObject({ content: [{ checkItemId: 101, guide: null }] });
  });

  it('API-302는 stage와 presetType을 필수로 보내고 프리셋 순서를 읽는다', async () => {
    authenticate();
    server.use(
      http.get(`${config.apiBaseUrl}/api/checklist-presets`, ({ request }) => {
        expect(new URL(request.url).searchParams.toString()).toBe('stage=ONLINE_PHONE&presetType=ONE_ROOM');
        return HttpResponse.json(
          successEnvelope({
            ...presetFixture,
            items: [{ ...presetFixture.items[0], guide: null }, presetFixture.items[1]],
          }),
        );
      }),
    );
    const result = await fetchChecklistPreset(config, 'ONLINE_PHONE', 'ONE_ROOM');
    expect(result.items.map((item) => item.order)).toEqual([0, 1]);
    expect(result.items[0]?.guide).toBeNull();
  });

  it('API-303은 stage 페이지를 조회하고 memberId를 보내지 않는다', async () => {
    authenticate();
    server.use(
      http.get(`${config.apiBaseUrl}/api/checklists`, ({ request }) => {
        const search = new URL(request.url).searchParams;
        expect(search.get('stage')).toBe('ONLINE_PHONE');
        expect(search.has('memberId')).toBe(false);
        return HttpResponse.json(successEnvelope(checklistPageFixture([checklistSummaryFixture])));
      }),
    );
    await expect(fetchChecklists(config, { stage: 'ONLINE_PHONE', page: 0 })).resolves.toMatchObject({
      content: [{ checklistId: 7 }],
    });
  });

  it('API-304는 trim된 이름·고정 단계·정렬된 항목 ID 배열을 그대로 보낸다', async () => {
    authenticate();
    let body: unknown;
    server.use(
      http.post(`${config.apiBaseUrl}/api/checklists`, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(
          successEnvelope({
            ...checklistDetailFixture,
            name: '전화 문의',
          }),
          { status: 201 },
        );
      }),
    );
    await createChecklist(config, { name: '전화 문의', stage: 'ONLINE_PHONE', checkItemIds: [102, 101] });
    expect(body).toEqual({ name: '전화 문의', stage: 'ONLINE_PHONE', checkItemIds: [102, 101] });
  });

  it('API-304 v1.1은 PROVIDED·CUSTOM 혼합 items만 전송하고 안정적인 로컬 ID 응답을 읽는다', async () => {
    authenticate();
    let body: unknown;
    server.use(
      http.post(`${config.apiBaseUrl}/api/checklists`, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(successEnvelope(mixedChecklistDetailFixture), { status: 201 });
      }),
    );

    const result = await createChecklistV11(config, {
      name: '전화 문의 기본 목록',
      stage: 'ONLINE_PHONE',
      items: [
        { origin: 'PROVIDED', sourceCheckItemId: 101 },
        { origin: 'CUSTOM', question: '창틀 곰팡이는 괜찮은가?' },
      ],
    });

    expect(body).toEqual({
      name: '전화 문의 기본 목록',
      stage: 'ONLINE_PHONE',
      items: [
        { origin: 'PROVIDED', sourceCheckItemId: 101 },
        { origin: 'CUSTOM', question: '창틀 곰팡이는 괜찮은가?' },
      ],
    });
    expect(body).not.toHaveProperty('checkItemIds');
    expect(result.items).toEqual([
      expect.objectContaining({ checklistItemId: 701, origin: 'PROVIDED', sourceCheckItemId: 101 }),
      expect.objectContaining({ checklistItemId: 703, origin: 'CUSTOM', sourceCheckItemId: null }),
    ]);
  });

  it('API-305는 상세 항목 배열 순서와 집계를 검증한다', async () => {
    authenticate();
    server.use(
      http.get(`${config.apiBaseUrl}/api/checklists/7`, () =>
        HttpResponse.json(successEnvelope(checklistDetailFixture)),
      ),
    );
    const result = await fetchChecklistDetail(config, 7);
    expect(result.items.map((item) => item.checklistItemId)).toEqual([701, 702]);
    expect(result.items.map((item) => item.sourceCheckItemId)).toEqual([101, 102]);
    expect(result.assignedPropertyCount).toBe(1);
  });

  it('API-305 응답에서 CUSTOM 질문과 PROVIDED 출처가 섞이거나 deprecated ID가 다르면 거부한다', async () => {
    authenticate();
    let detail = {
      ...mixedChecklistDetailFixture,
      items: [{ ...customChecklistItemFixture, sourceCheckItemId: 101 }, mixedChecklistDetailFixture.items[0]],
    };
    server.use(http.get(`${config.apiBaseUrl}/api/checklists/7`, () => HttpResponse.json(successEnvelope(detail))));
    await expect(fetchChecklistDetail(config, 7)).rejects.toMatchObject({ kind: 'invalid-response' });

    detail = {
      ...checklistDetailFixture,
      items: [{ ...checklistDetailFixture.items[0], checkItemId: 999 }, checklistDetailFixture.items[1]],
    };
    await expect(fetchChecklistDetail(config, 7)).rejects.toMatchObject({ kind: 'invalid-response' });
  });

  it('API-306은 생성 후 변경할 수 없는 stage를 보내지 않고 전체 항목 배열만 보낸다', async () => {
    authenticate();
    let body: unknown;
    server.use(
      http.put(`${config.apiBaseUrl}/api/checklists/7`, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(
          successEnvelope({
            ...checklistDetailFixture,
            name: '수정한 목록',
            items: [...checklistDetailFixture.items].reverse(),
          }),
        );
      }),
    );
    await updateChecklist(config, 7, { name: '수정한 목록', checkItemIds: [102, 101] });
    expect(body).toEqual({ name: '수정한 목록', checkItemIds: [102, 101] });
    expect(body).not.toHaveProperty('stage');
  });

  it('API-306 v1.1은 기존 CUSTOM checklistItemId를 유지하고 신규 CUSTOM에는 ID를 보내지 않는다', async () => {
    authenticate();
    let body: unknown;
    server.use(
      http.put(`${config.apiBaseUrl}/api/checklists/7`, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(successEnvelope(mixedChecklistDetailFixture));
      }),
    );

    await updateChecklistV11(config, 7, {
      name: '전화 문의 기본 목록',
      items: [
        { origin: 'CUSTOM', checklistItemId: 703, question: '곰팡이 냄새는 괜찮은가?' },
        { origin: 'PROVIDED', sourceCheckItemId: 101 },
        { origin: 'CUSTOM', question: '환기 상태는 괜찮은가?' },
      ],
    });

    expect(body).toEqual({
      name: '전화 문의 기본 목록',
      items: [
        { origin: 'CUSTOM', checklistItemId: 703, question: '곰팡이 냄새는 괜찮은가?' },
        { origin: 'PROVIDED', sourceCheckItemId: 101 },
        { origin: 'CUSTOM', question: '환기 상태는 괜찮은가?' },
      ],
    });
    expect(body).not.toHaveProperty('checkItemIds');
  });

  it('legacy checkItemIds 함수는 items를 함께 보내지 않고 409 오류 code를 보존한다', async () => {
    authenticate();
    let body: unknown;
    server.use(
      http.put(`${config.apiBaseUrl}/api/checklists/7`, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(errorEnvelope('CHECKLIST_REQUIRES_V11_CLIENT'), { status: 409 });
      }),
    );

    const error = await updateChecklist(config, 7, { name: '수정', checkItemIds: [101] }).catch(
      (caught: unknown) => caught,
    );
    expect(body).toEqual({ name: '수정', checkItemIds: [101] });
    expect(body).not.toHaveProperty('items');
    expect(error).toMatchObject({ status: 409, code: 'CHECKLIST_REQUIRES_V11_CLIENT' });
  });

  it('API-307과 API-402의 204 응답을 JSON으로 읽지 않는다', async () => {
    authenticate();
    server.use(
      http.delete(`${config.apiBaseUrl}/api/checklists/7`, () => new HttpResponse(null, { status: 204 })),
      http.delete(
        `${config.apiBaseUrl}/api/properties/10/active-checklists/ONLINE_PHONE`,
        () => new HttpResponse(null, { status: 204 }),
      ),
    );
    await expect(removeChecklist(config, 7)).resolves.toBeUndefined();
    await expect(removeActiveChecklist(config, 10, 'ONLINE_PHONE')).resolves.toBeUndefined();
  });

  it('API-401은 최종 checklistId만 보내고 연결 응답을 검증한다', async () => {
    authenticate();
    let body: unknown;
    server.use(
      http.put(`${config.apiBaseUrl}/api/properties/10/active-checklists/ONLINE_PHONE`, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(
          successEnvelope({
            propertyId: 10,
            stage: 'ONLINE_PHONE',
            checklistId: 7,
            name: '전화 문의 기본 목록',
            itemCount: 2,
          }),
        );
      }),
    );
    await expect(assignActiveChecklist(config, 10, 'ONLINE_PHONE', { checklistId: 7 })).resolves.toMatchObject({
      propertyId: 10,
      checklistId: 7,
    });
    expect(body).toEqual({ checklistId: 7 });
  });

  it('서버 메시지를 노출하지 않고 체크리스트 오류 코드를 안전한 문구로 매핑한다', async () => {
    authenticate();
    server.use(
      http.get(`${config.apiBaseUrl}/api/checklists/7`, () =>
        HttpResponse.json(errorEnvelope('CHECK_ITEM_INACTIVE'), { status: 400 }),
      ),
    );
    const error = await fetchChecklistDetail(config, 7).catch((caught: unknown) => caught);
    expect(getChecklistErrorMessage(error)).toContain('더 이상 제공되지 않는 항목');
    expect(getChecklistErrorMessage(error)).not.toContain('서버 내부 상세 메시지');
  });
});
