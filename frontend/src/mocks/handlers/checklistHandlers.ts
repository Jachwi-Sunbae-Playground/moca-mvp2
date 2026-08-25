import { http, HttpResponse } from 'msw';
import { isChecklistStage } from '../../constants/checklist';
import {
  checklistDetail,
  checkItems,
  failure,
  getMockChecklists,
  obsoleteEndpoint,
  readPositiveInteger,
  setMockChecklists,
  success,
} from '../mockStore';
import type { MockChecklistItem } from '../mockStore';
import type { ChecklistStage } from '../../types/Checklist';

type ChecklistItemBody = { systemCheckItemId?: unknown; question?: unknown };

const checklistItemsFromBody = (stage: ChecklistStage, value: unknown): MockChecklistItem[] | null => {
  if (!Array.isArray(value) || value.length < 1 || value.length > 30) return null;
  const seen = new Set<string>();
  const result: MockChecklistItem[] = [];
  for (const [index, raw] of value.entries()) {
    if (typeof raw !== 'object' || raw === null) return null;
    const item = raw as ChecklistItemBody;
    if (Number.isInteger(item.systemCheckItemId) && item.question === undefined) {
      const system = checkItems.find(
        (candidate) => candidate.id === item.systemCheckItemId && candidate.stage === stage,
      );
      const key = `SYSTEM:${String(item.systemCheckItemId)}`;
      if (system === undefined || seen.has(key)) return null;
      seen.add(key);
      result.push({
        ...system,
        systemCheckItemId: system.id,
        origin: 'PROVIDED' as const,
        displayOrder: index + 1,
        active: true,
      });
      continue;
    }
    if (item.systemCheckItemId === null && typeof item.question === 'string') {
      const question = item.question.trim();
      const key = `CUSTOM:${question}`;
      if (question.length < 1 || question.length > 200 || seen.has(key)) return null;
      seen.add(key);
      result.push({
        id: 9_000 + index,
        systemCheckItemId: null,
        origin: 'CUSTOM' as const,
        stage,
        itemType: 'OPTIONAL' as const,
        question,
        displayOrder: index + 1,
        active: true,
      });
      continue;
    }
    return null;
  }
  return result;
};

export const checklistHandlers = [
  http.get('*/api/check-items', ({ request }) => {
    const url = new URL(request.url);
    const stage = url.searchParams.get('stage');
    if (!isChecklistStage(stage)) return failure('INVALID_REQUEST', 400);
    const query = url.searchParams.get('query')?.trim() ?? '';
    const items = checkItems.filter((item) => item.stage === stage && item.question.includes(query));
    return success(items);
  }),
  http.get('*/api/checklist-presets', obsoleteEndpoint),
  http.get('*/api/checklists', ({ request }) => {
    const requestedStage = new URL(request.url).searchParams.get('stage');
    if (requestedStage !== null && !isChecklistStage(requestedStage)) return failure('INVALID_REQUEST', 400);
    const items = getMockChecklists()
      .filter((checklist) => requestedStage === null || checklist.stage === requestedStage)
      .sort((a, b) => b.id - a.id)
      .map((checklist) => ({
        id: checklist.id,
        name: checklist.name,
        stage: checklist.stage,
        itemCount: checklist.items.length,
      }));
    return success({ totalCount: items.length, items });
  }),
  http.post('*/api/checklists', async ({ request }) => {
    const body = (await request.json()) as {
      name?: unknown;
      stage?: unknown;
      items?: unknown;
    };
    if (
      typeof body.name !== 'string' ||
      body.name.trim().length < 1 ||
      body.name.trim().length > 30 ||
      !isChecklistStage(body.stage) ||
      !Array.isArray(body.items)
    ) {
      return failure('INVALID_REQUEST', 400);
    }
    const items = checklistItemsFromBody(body.stage, body.items);
    if (items === null) return failure('CHECKLIST_ITEMS_INVALID', 400);
    const checklist = {
      id: Math.max(0, ...getMockChecklists().map((candidate) => candidate.id)) + 1,
      name: body.name.trim(),
      stage: body.stage,
      items,
    };
    setMockChecklists([...getMockChecklists(), checklist]);
    return success(checklistDetail(checklist), 201);
  }),
  http.get('*/api/checklists/:checklistId', ({ params }) => {
    const checklistId = readPositiveInteger(params.checklistId);
    const checklist = getMockChecklists().find((candidate) => candidate.id === checklistId);
    return checklist === undefined ? failure('CHECKLIST_NOT_FOUND', 404) : success(checklistDetail(checklist));
  }),
  http.put('*/api/checklists/:checklistId', async ({ params, request }) => {
    const checklistId = readPositiveInteger(params.checklistId);
    const current = getMockChecklists().find((candidate) => candidate.id === checklistId);
    if (current === undefined) return failure('CHECKLIST_NOT_FOUND', 404);
    const body = (await request.json()) as { name?: unknown; items?: unknown };
    if (
      typeof body.name !== 'string' ||
      body.name.trim().length < 1 ||
      body.name.trim().length > 30 ||
      !Array.isArray(body.items)
    ) {
      return failure('INVALID_REQUEST', 400);
    }
    const items = checklistItemsFromBody(current.stage, body.items);
    if (items === null) return failure('CHECKLIST_ITEMS_INVALID', 400);
    const updated = { ...current, name: body.name.trim(), items };
    setMockChecklists(getMockChecklists().map((checklist) => (checklist.id === current.id ? updated : checklist)));
    return success(checklistDetail(updated));
  }),
  http.delete('*/api/checklists/:checklistId', ({ params }) => {
    const checklistId = readPositiveInteger(params.checklistId);
    if (!getMockChecklists().some((checklist) => checklist.id === checklistId)) {
      return failure('CHECKLIST_NOT_FOUND', 404);
    }
    setMockChecklists(getMockChecklists().filter((checklist) => checklist.id !== checklistId));
    return new HttpResponse(null, { status: 200 });
  }),
];
