import { beforeEach, describe, expect, it } from 'vitest';
import { handlers } from './handlers';
import { resetMockStore } from './mockStore';
import { server } from '../test/server';

const apiUrl = (path: string) => `http://localhost${path}`;

const readJson = async (response: Response) => response.json() as Promise<Record<string, unknown>>;

describe('최종 API 명세 MSW handlers', () => {
  beforeEach(() => {
    resetMockStore();
    server.use(...handlers);
  });

  it('인증 없이 단계별 시스템 체크 항목을 검색한다', async () => {
    const response = await fetch(apiUrl('/api/check-items?stage=ON_SITE&query=수압'));
    const body = await readJson(response);

    expect(response.status).toBe(200);
    expect(body).toMatchObject({
      code: 'SUCCESS',
      data: [{ id: 201, stage: 'ON_SITE', itemType: 'CORE', question: '수압이 충분한가요?' }],
    });
  });

  it('초기 메모 생성 응답에 매물 메모 항목 ID를 포함한다', async () => {
    const response = await fetch(apiUrl('/api/properties/10/memo'), { method: 'POST' });
    const body = await readJson(response);

    expect(response.status).toBe(200);
    expect(body).toMatchObject({ code: 'SUCCESS', data: { propertyId: 10 } });
    const data = body.data as { items: Array<Record<string, unknown>> };
    expect(data.items[0]).toMatchObject({ propertyMemoItemId: 1001, systemMemoItemId: 1 });
  });

  it('CORE 항목을 선택 항목으로 보내면 체크리스트 생성을 거절한다', async () => {
    const response = await fetch(apiUrl('/api/checklists'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: '잘못된 체크리스트',
        stage: 'ONLINE_PHONE',
        optionalSystemCheckItemIds: [101],
      }),
    });
    const body = await readJson(response);

    expect(response.status).toBe(400);
    expect(body).toMatchObject({ code: 'INVALID_SYSTEM_CHECK_ITEM' });
  });

  it('체크리스트를 생성해 매물에 연결하고 상태와 메모를 자동 저장한다', async () => {
    const createResponse = await fetch(apiUrl('/api/checklists'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: '현장 확인 목록',
        stage: 'ON_SITE',
        optionalSystemCheckItemIds: [203],
      }),
    });
    const createBody = await readJson(createResponse);
    const created = createBody.data as {
      id: number;
      itemCount: number;
      items: Array<{ systemCheckItemId: number }>;
    };

    expect(createResponse.status).toBe(201);
    expect(created).toMatchObject({ itemCount: 3 });
    expect(created.items.map((item) => item.systemCheckItemId)).toEqual([201, 202, 203]);

    const applyResponse = await fetch(apiUrl('/api/properties/10/checklists/ON_SITE'), {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ checklistId: created.id }),
    });
    const applyBody = await readJson(applyResponse);
    const applied = applyBody.data as {
      id: number;
      propertyId: number;
      stage: string;
      items: Array<{ id: number; status: string; memo: string }>;
    };

    expect(applyResponse.status).toBe(200);
    expect(applied).toMatchObject({ propertyId: 10, stage: 'ON_SITE' });
    expect(applied.items).toHaveLength(3);
    expect(applied.items[0]).toMatchObject({ status: 'UNCONFIRMED', memo: '' });

    const itemId = applied.items[0]?.id;
    expect(itemId).toBeTypeOf('number');

    const statusResponse = await fetch(apiUrl(`/api/properties/10/checklists/${applied.id}/items/${itemId}/status`), {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status: 'GOOD' }),
    });
    const memoResponse = await fetch(apiUrl(`/api/properties/10/checklists/${applied.id}/items/${itemId}/memo`), {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ memo: '수압이 충분함' }),
    });

    expect(statusResponse.status).toBe(200);
    expect(memoResponse.status).toBe(200);

    const detailResponse = await fetch(apiUrl(`/api/properties/10/checklists/${applied.id}`));
    const detailBody = await readJson(detailResponse);
    const detail = detailBody.data as {
      items: Array<{ id: number; status: string; memo: string }>;
    };

    expect(detailResponse.status).toBe(200);
    expect(detail.items.find((item) => item.id === itemId)).toMatchObject({
      status: 'GOOD',
      memo: '수압이 충분함',
    });
  });

  it.each([
    ['POST', '/api/properties/10/photos', 501, 'NOT_IMPLEMENTED'],
    ['GET', '/api/checklist-presets', 410, 'API_CONTRACT_REMOVED'],
    ['GET', '/api/properties/10/active-checklists/ONLINE_PHONE', 410, 'API_CONTRACT_REMOVED'],
  ])('%s %s는 성공 응답으로 위장하지 않는다', async (method, path, status, code) => {
    const response = await fetch(apiUrl(path), { method });
    const body = await readJson(response);

    expect(response.status).toBe(status);
    expect(body).toMatchObject({ code });
  });
});
