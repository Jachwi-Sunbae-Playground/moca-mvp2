import { http, HttpResponse } from 'msw';
import { isChecklistStage } from '../../constants/checklist';
import {
  checklistDetail,
  checklistItemsFor,
  checkItems,
  failure,
  getMockChecklists,
  hasDuplicates,
  obsoleteEndpoint,
  readPositiveInteger,
  setMockChecklists,
  success,
} from '../mockStore';

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
      optionalSystemCheckItemIds?: unknown;
    };
    if (
      typeof body.name !== 'string' ||
      body.name.trim().length < 1 ||
      body.name.trim().length > 30 ||
      !isChecklistStage(body.stage) ||
      !Array.isArray(body.optionalSystemCheckItemIds) ||
      !body.optionalSystemCheckItemIds.every((id) => Number.isInteger(id))
    ) {
      return failure('INVALID_REQUEST', 400);
    }
    const optionalIds = body.optionalSystemCheckItemIds as number[];
    if (hasDuplicates(optionalIds)) return failure('DUPLICATE_CHECK_ITEM', 400);
    const optionalItems = optionalIds.map((id) => checkItems.find((item) => item.id === id));
    if (optionalItems.some((item) => item === undefined || item.stage !== body.stage || item.itemType !== 'OPTIONAL')) {
      return failure('INVALID_SYSTEM_CHECK_ITEM', 400);
    }
    const coreIds = checkItems
      .filter((item) => item.stage === body.stage && item.itemType === 'CORE')
      .map((item) => item.id);
    if (coreIds.length + optionalIds.length > 30) return failure('CHECKLIST_ITEM_COUNT_OUT_OF_RANGE', 400);
    const checklist = {
      id: Math.max(0, ...getMockChecklists().map((candidate) => candidate.id)) + 1,
      name: body.name.trim(),
      stage: body.stage,
      items: checklistItemsFor(body.stage, [...coreIds, ...optionalIds]),
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
    const body = (await request.json()) as { name?: unknown; systemCheckItemIds?: unknown };
    if (
      typeof body.name !== 'string' ||
      body.name.trim().length < 1 ||
      body.name.trim().length > 30 ||
      !Array.isArray(body.systemCheckItemIds) ||
      !body.systemCheckItemIds.every((id) => Number.isInteger(id))
    ) {
      return failure('INVALID_REQUEST', 400);
    }
    const ids = body.systemCheckItemIds as number[];
    if (hasDuplicates(ids)) return failure('DUPLICATE_CHECK_ITEM', 400);
    if (ids.length < 1 || ids.length > 30) return failure('CHECKLIST_ITEM_COUNT_OUT_OF_RANGE', 400);
    const requestedItems = ids.map((id) => checkItems.find((item) => item.id === id));
    if (requestedItems.some((item) => item === undefined)) return failure('INVALID_SYSTEM_CHECK_ITEM', 400);
    if (requestedItems.some((item) => item?.stage !== current.stage)) {
      return failure('CHECKLIST_STAGE_MISMATCH', 400);
    }
    const updated = { ...current, name: body.name.trim(), items: checklistItemsFor(current.stage, ids) };
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
